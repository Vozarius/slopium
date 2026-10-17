"""Offline Spark profile analysis. Reads input files; writes derived reports only.

Wire schema: https://github.com/lucko/spark-viewer/blob/master/proto/spark.proto
All stack percentages use inclusive sampled Render-thread time as denominator.
"""
from __future__ import annotations

import csv
import gzip
import hashlib
import json
import struct
from collections import Counter, defaultdict
from datetime import datetime, timezone, timedelta
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'analysis'


def varint(data, pos):
    value = shift = 0
    while True:
        b = data[pos]
        pos += 1
        value |= (b & 127) << shift
        if not b & 128:
            return value, pos
        shift += 7
        if shift > 70:
            raise ValueError('Invalid varint')


def wire(data):
    pos = 0
    while pos < len(data):
        tag, pos = varint(data, pos)
        field, kind = tag >> 3, tag & 7
        if kind == 0:
            value, pos = varint(data, pos)
        elif kind == 1:
            value = struct.unpack_from('<d', data, pos)[0]
            pos += 8
        elif kind == 2:
            size, pos = varint(data, pos)
            value = data[pos:pos + size]
            assert len(value) == size
            pos += size
        elif kind == 5:
            value = struct.unpack_from('<f', data, pos)[0]
            pos += 4
        else:
            raise ValueError(f'Unexpected wire kind {kind}')
        yield field, kind, value
    assert pos == len(data)


# (field name, type); [] denotes repeated, map entries use @message.
SCHEMAS = {
    'Sampler': {1:('metadata','Metadata'),2:('threads','[]Thread'),3:('classSources','@StringMap'),4:('methodSources','@StringMap'),5:('lineSources','@StringMap'),6:('timeWindows','[]int'),7:('timeWindowStatistics','@WindowMap')},
    'Metadata': {1:('user','User'),2:('startTime','int'),3:('interval','int'),4:('threadDumper','Dumper'),5:('dataAggregator','Aggregator'),6:('comment','str'),7:('platform','Platform'),8:('platformStatistics','PlatformStats'),9:('systemStatistics','System'),10:('serverConfigurations','@StringMap'),11:('endTime','int'),12:('numberOfTicks','int'),13:('sources','@SourceMap'),14:('extraPlatformMetadata','@StringMap'),15:('samplerMode','int'),16:('samplerEngine','int'),17:('samplerEngineVersion','str')},
    'User': {1:('type','int'),2:('name','str'),3:('uniqueId','str')},
    'Dumper': {1:('type','int'),2:('ids','[]int'),3:('patterns','[]str')},
    'Aggregator': {1:('type','int'),2:('threadGrouper','int'),3:('tickLengthThreshold','int'),4:('numberOfIncludedTicks','int')},
    'Platform': {1:('type','int'),2:('name','str'),3:('version','str'),4:('minecraftVersion','str'),7:('sparkVersion','int'),8:('brand','str')},
    'Thread': {1:('name','str'),2:('time','double'),3:('children','[]Node'),4:('times','[]double'),5:('childrenRefs','[]int')},
    'Node': {1:('time','double'),2:('children','[]Node'),3:('className','str'),4:('methodName','str'),5:('parentLineNumber','int'),6:('lineNumber','int'),7:('methodDesc','str'),8:('times','[]double'),9:('childrenRefs','[]int')},
    'StringMap': {1:('key','str'),2:('value','str')},
    'SourceMap': {1:('key','str'),2:('value','Source')},
    'Source': {1:('name','str'),2:('version','str'),3:('author','str'),4:('description','str'),5:('builtIn','int')},
    'WindowMap': {1:('key','int'),2:('value','Window')},
    'Window': {1:('ticks','int'),2:('cpuProcess','double'),3:('cpuSystem','double'),4:('tps','double'),5:('msptMedian','double'),6:('msptMax','double'),7:('players','int'),8:('entities','int'),9:('tileEntities','int'),10:('chunks','int'),11:('startTime','int'),12:('endTime','int'),13:('duration','int')},
    'PlatformStats': {1:('memory','PlatformMemory'),2:('gc','@GcMap'),3:('uptime','int'),4:('tps','Tps'),5:('mspt','Mspt'),6:('ping','Ping'),7:('playerCount','int'),8:('world','WorldStats')},
    'PlatformMemory': {1:('heap','MemoryUsage'),2:('nonHeap','MemoryUsage'),3:('pools','[]Pool')},
    'Pool': {1:('name','str'),2:('usage','MemoryUsage'),3:('collectionUsage','MemoryUsage')},
    'MemoryUsage': {1:('used','int'),2:('committed','int'),3:('init','int'),4:('max','int')},
    'GcMap': {1:('key','str'),2:('value','Gc')},
    'Gc': {1:('total','int'),2:('avgTime','double'),3:('avgFrequency','double')},
    'Tps': {1:('last1m','double'),2:('last5m','double'),3:('last15m','double')},
    'Mspt': {1:('last1m','Rolling'),2:('last5m','Rolling')},
    'Ping': {1:('last15m','Rolling')},
    'Rolling': {1:('mean','double'),2:('max','double'),3:('min','double'),4:('median','double'),5:('percentile95','double')},
    'WorldStats': {1:('totalEntities','int'),2:('entityCounts','@IntMap'),3:('worlds','[]World')},
    'World': {1:('name','str'),2:('totalEntities','int'),3:('regions','[]Region')},
    'Region': {1:('totalEntities','int'),2:('chunks','[]Chunk')},
    'Chunk': {1:('x','int'),2:('z','int'),3:('totalEntities','int'),4:('entityCounts','@IntMap')},
    'IntMap': {1:('key','str'),2:('value','int')},
    'System': {1:('cpu','Cpu'),2:('memory','SystemMemory'),3:('gc','@GcMap'),4:('disk','Disk'),5:('os','Os'),6:('java','Java'),7:('uptime','int'),9:('jvm','Jvm')},
    'Cpu': {1:('threads','int'),2:('processUsage','Usage'),3:('systemUsage','Usage'),4:('modelName','str')},
    'Usage': {1:('last1m','double'),2:('last15m','double')},
    'SystemMemory': {1:('physical','SystemPool'),2:('swap','SystemPool')},
    'SystemPool': {1:('used','int'),2:('total','int')},
    'Disk': {1:('used','int'),2:('total','int')},
    'Os': {1:('arch','str'),2:('name','str'),3:('version','str')},
    'Java': {1:('vendor','str'),2:('version','str'),3:('vendorVersion','str'),4:('vmArgs','str')},
    'Jvm': {1:('name','str'),2:('vendor','str'),3:('version','str')},
}


def decode(data, schema):
    out = {}
    for field, kind, value in wire(data):
        if field not in SCHEMAS[schema]:
            continue
        name, typ = SCHEMAS[schema][field]
        repeated = typ.startswith('[]')
        mapping = typ.startswith('@')
        base = typ[2:] if repeated else typ[1:] if mapping else typ
        if base == 'str':
            values = [value.decode('utf-8')]
        elif base == 'int':
            if kind == 2:
                values, pos = [], 0
                while pos < len(value):
                    num, pos = varint(value, pos)
                    values.append(num)
            else:
                values = [value]
            values = [v - (1 << 64) if v >= (1 << 63) else v for v in values]
        elif base == 'double':
            values = list(struct.unpack('<' + 'd' * (len(value) // 8), value)) if kind == 2 else [value]
        else:
            values = [decode(value, base)]
        if repeated:
            out.setdefault(name, []).extend(values)
        elif mapping:
            entry = values[0]
            out.setdefault(name, {})[str(entry.get('key', ''))] = entry.get('value', '')
        else:
            assert len(values) == 1
            out[name] = values[0]
    return out


def read_profile(path):
    return json.loads(path.read_text(encoding='utf-8')) if path.suffix == '.json' else decode(path.read_bytes(), 'Sampler')


def label(node):
    return node.get('className', '') + '.' + node.get('methodName', '')


def total(node):
    return sum(node['times']) if node.get('times') else node.get('time', 0)


def flatten(thread):
    """Thread.children is a node table when childrenRefs is populated."""
    pool = thread['children']
    roots = thread.get('childrenRefs', [])
    rows = []
    seen = set()

    def visit(index, parent, path):
        assert index not in seen, f'Repeated/cyclic node reference: {index}'
        seen.add(index)
        node = pool[index]
        refs = node.get('childrenRefs', [])
        inc = total(node)
        child_total = sum(total(pool[r]) for r in refs)
        own = inc - child_total
        assert own >= -0.01, (label(node), inc, child_total)
        row = dict(index=index, parent=parent, label=label(node), inclusive_ms=inc,
                   self_ms=max(0, own), times=node.get('times', []), path=path + [label(node)], children=refs)
        rows.append(row)
        for child in refs:
            visit(child, index, row['path'])

    assert roots, 'Expected flattened Spark v2 tree'
    for root in roots:
        visit(root, None, [])
    assert len(seen) == len(pool), (len(seen), len(pool))
    assert abs(sum(total(pool[r]) for r in roots) - total(thread)) < 0.01
    assert abs(sum(r['self_ms'] for r in rows) - total(thread)) < 0.01
    return rows


def summary(path, data):
    meta = data['metadata']
    system = json.loads(json.dumps(meta.get('systemStatistics', {})))
    system.get('java', {}).pop('vmArgs', None)
    plat = meta.get('platformStatistics', {})
    result = {
        'file': path.name, 'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
        'duration_s': (meta['endTime'] - meta['startTime']) / 1000,
        'start_msk': datetime.fromtimestamp(meta['startTime']/1000, timezone(timedelta(hours=3))).isoformat(),
        'end_msk': datetime.fromtimestamp(meta['endTime']/1000, timezone(timedelta(hours=3))).isoformat(),
        'interval_us': meta['interval'], 'samplerEngine': meta.get('samplerEngine', 0),
        'samplerMode': meta.get('samplerMode', 0), 'threadDumper': meta.get('threadDumper'),
        'dataAggregator': meta.get('dataAggregator'), 'platform': meta.get('platform'),
        'system': system, 'platformStatistics': {k: v for k,v in plat.items() if k != 'world'},
        'world': {k: plat.get('world', {}).get(k) for k in ('totalEntities','entityCounts')},
        'timeWindows': data.get('timeWindows'), 'timeWindowStatistics': data.get('timeWindowStatistics'),
        'sources': meta.get('sources', {}), 'threads': [],
    }
    for thread in data['threads']:
        rows = flatten(thread)
        denominator = total(thread)
        aggregate = defaultdict(lambda: {'inclusive_ms': 0, 'self_ms': 0})
        for row in rows:
            aggregate[row['label']]['self_ms'] += row['self_ms']
            # Avoid recursively counting a method twice in an inclusive total.
            if row['label'] not in row['path'][:-1]:
                aggregate[row['label']]['inclusive_ms'] += row['inclusive_ms']
        methods = [dict(method=k, **v, inclusive_pct=v['inclusive_ms']/denominator*100,
                        self_pct=v['self_ms']/denominator*100) for k,v in aggregate.items()]
        methods.sort(key=lambda x: -x['inclusive_ms'])
        info = {'name': thread['name'], 'sampled_ms': denominator, 'node_count': len(rows),
                'times': thread.get('times'), 'top_inclusive': methods[:100],
                'top_self': sorted(methods, key=lambda x:-x['self_ms'])[:60]}
        result['threads'].append(info)
        prefix = path.stem + '-' + thread['name'].replace(' ', '_')
        with (OUT / (prefix + '-methods.csv')).open('w', encoding='utf-8-sig', newline='') as f:
            writer = csv.DictWriter(f, fieldnames=list(methods[0]))
            writer.writeheader()
            writer.writerows(methods)
        with gzip.open(OUT / (prefix + '-stacks.csv.gz'), 'wt', encoding='utf-8-sig', newline='') as f:
            fields = ['index', 'parent', 'inclusive_ms', 'self_ms', 'inclusive_pct', 'path']
            writer = csv.DictWriter(f, fieldnames=fields)
            writer.writeheader()
            writer.writerows({k: (' > '.join(row['path']) if k == 'path' else row['inclusive_ms']/denominator*100 if k == 'inclusive_pct' else row[k]) for k in fields} for row in sorted(rows, key=lambda x:-x['inclusive_ms']))
    return result


def main():
    OUT.mkdir(exist_ok=True)
    for path in sorted((ROOT / 'reference' / 'profilers').iterdir()):
        if path.suffix not in ('.json', '.sparkprofile'):
            continue
        data = read_profile(path)
        result = summary(path, data)
        (OUT / (path.stem + '-summary.json')).write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
        print('\nPROFILE', path.name)
        print('duration', result['duration_s'], 'start MSK', result['start_msk'])
        print('engine',result['samplerEngine'],'mode',result['samplerMode'])
        print('cpu',result['system'].get('cpu'),'GC',result['system'].get('gc'))
        print('heap',result['platformStatistics'].get('memory',{}).get('heap'))
        print('memory',result['system'].get('memory'))
        print('JVM',result['system'].get('jvm'))
        print('windows',json.dumps(result['timeWindowStatistics']))
        for thread in result['threads']:
            print('THREAD',thread['name'],'sampled_ms',thread['sampled_ms'],'nodes',thread['node_count'])
            print('TOP INCLUSIVE')
            for m in thread['top_inclusive'][:65]:
                print(f"{m['inclusive_pct']:7.3f}% self={m['self_pct']:7.3f}% {m['method']}")
            print('TOP SELF')
            for m in thread['top_self'][:25]:
                print(f"{m['self_pct']:7.3f}% incl={m['inclusive_pct']:7.3f}% {m['method']}")


if __name__ == '__main__':
    main()
