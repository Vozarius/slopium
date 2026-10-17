"""Create compact, reproducible evidence and a timeline chart for the report."""
import csv
import json
from collections import Counter
from datetime import datetime, timedelta, timezone

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import numpy as np

from analyze_profiles import ROOT, OUT, read_profile, flatten, total

TARGETS = {
    'render': 'net.minecraft.client.renderer.GameRenderer.render',
    'tick': 'net.minecraft.client.Minecraft.tick',
    'block_entity_tick': 'net.minecraft.world.level.Level.tickBlockEntities',
    'create_tick': 'com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker.tick',
    'dh_lod': 'com.seibel.distanthorizons.core.api.internal.ClientApi.renderLodLayer',
    'dynamic_light': 'toni.sodiumdynamiclights.SodiumDynamicLights.getDynamicLightLevel',
    'swap': 'org.lwjgl.glfw.GLFW.glfwSwapBuffers',
    'breaking': 'net.minecraft.client.renderer.block.BlockRenderDispatcher.renderBreakingTexture',
    'block_entity_render': 'net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher.render',
    'flywheel_render': 'dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl.render',
    'map_render': 'xaero.map.MapProcessor.onRenderProcess',
    'minimap_render': 'xaero.common.minimap.render.MinimapRenderer.renderMinimap',
}
PHASES = [
    ('block_entity_tick', 'Тики блочных сущностей', '#d76532'),
    ('other_tick', 'Остальные клиентские тики', '#eab76a'),
    ('render', 'Рендеринг', '#3283a8'),
    ('swap', 'SwapBuffers: вывод / ожидание', '#9ea9b1'),
    ('other', 'Прочее', '#dfe5ea'),
]


def main():
    output, all_windows = [], []
    fig, axes = plt.subplots(1, 2, figsize=(16, 7.7), gridspec_kw={'width_ratios': [1.15, 1]})
    for ix, path in enumerate(sorted((ROOT / 'reference' / 'profilers').iterdir())):
        data = read_profile(path)
        thread = data['threads'][0]
        rows = flatten(thread)
        denominator = total(thread)
        groups = {key: [r for r in rows if r['label'] == method and method not in r['path'][:-1]] for key, method in TARGETS.items()}
        metrics = {key: sum(r['inclusive_ms'] for r in group) / denominator * 100 for key, group in groups.items()}
        contexts = Counter()
        for row in rows:
            if TARGETS['dynamic_light'] in row['path']:
                key = 'breaking' if TARGETS['breaking'] in row['path'] else 'block_entity_render' if TARGETS['block_entity_render'] in row['path'] else 'other'
                contexts[key] += row['self_ms'] / denominator * 100
        windows = []
        for wi, window_id in enumerate(data['timeWindows']):
            stats = data['timeWindowStatistics'][str(window_id)]
            stamp = datetime.fromtimestamp(stats['startTime'] / 1000, timezone(timedelta(hours=3))).strftime('%H:%M:%S')
            window = {'file': path.name, 'start_msk': stamp, 'duration_s': thread['times'][wi] / 1000,
                      'ticks': stats.get('ticks'), 'cpu_process_pct': stats.get('cpuProcess', 0) * 100,
                      'cpu_system_pct': stats.get('cpuSystem', 0) * 100,
                      'client_mspt_median': stats.get('msptMedian'), 'client_mspt_max': stats.get('msptMax')}
            for key, group in groups.items():
                window[key] = sum(r['times'][wi] for r in group) / thread['times'][wi] * 100
            window['other_tick'] = window['tick'] - window['block_entity_tick']
            window['other'] = 100 - window['render'] - window['tick'] - window['swap']
            assert min(window[k] for k, _, _ in PHASES) >= -1e-8
            windows.append(window)
        output.append({'file': path.name, 'sampled_ms': denominator, 'metrics_pct': metrics,
                       'dynamic_light_contexts_pct': dict(contexts), 'windows': windows})
        all_windows.extend(windows)
        ax = axes[ix]
        ypos, left = np.arange(len(windows)), np.zeros(len(windows))
        for key, name, color in PHASES:
            values = np.array([w[key] for w in windows])
            ax.barh(ypos, values, left=left, color=color, height=0.68, label=name)
            if key == 'block_entity_tick':
                for y, value in zip(ypos, values):
                    if value >= 7:
                        ax.text(value / 2, y, f'{value:.1f}%', ha='center', va='center', color='white', fontsize=10, fontweight='bold')
            left += values
        ax.set_yticks(ypos, [w['start_msk'] + f"  ({w['duration_s']:.0f} с)" for w in windows], fontsize=10)
        ax.invert_yaxis()
        ax.set_xlim(0, 100)
        ax.set_xticks([0, 25, 50, 75, 100], ['0%', '25%', '50%', '75%', '100%'])
        ax.set_xlabel('Доля отсчётов Render thread', labelpad=12)
        ax.set_title(('Профиль A · IEpFPpf3OY.json' if ix == 0 else 'Профиль B · .sparkprofile') + '\nНачало окна, МСК; длительность окна', loc='left', fontsize=13, pad=14)
        ax.spines[['right', 'top', 'left']].set_visible(False)
        ax.tick_params(axis='y', length=0)
        ax.grid(axis='x', alpha=0.12)
        ax.set_axisbelow(True)
    fig.suptitle('Нагрузка основного потока меняется по ходу записи', fontsize=19, fontweight='bold', x=0.065, ha='left', y=0.98)
    handles, labels = axes[0].get_legend_handles_labels()
    fig.legend(handles, labels, loc='lower center', ncol=3, frameon=False, bbox_to_anchor=(0.5, 0.055), fontsize=11)
    fig.text(0.065, 0.02, 'Тики блочных сущностей входят в клиентские тики. Цветные части не пересекаются. Это не график FPS или загрузки GPU.', fontsize=10, color='#495562')
    fig.subplots_adjust(left=0.13, right=0.985, top=0.84, bottom=0.20, wspace=0.37)
    fig.savefig(OUT / 'render-thread-timeline.png', dpi=150, facecolor='white')
    (OUT / 'evidence.json').write_text(json.dumps(output, ensure_ascii=False, indent=2), encoding='utf-8')
    with (OUT / 'timeline.csv').open('w', encoding='utf-8-sig', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=list(all_windows[0]))
        writer.writeheader()
        writer.writerows(all_windows)
    print('Created evidence.json, timeline.csv, render-thread-timeline.png')
    for profile in output:
        print(profile['file'], {k: round(v, 3) for k, v in profile['metrics_pct'].items()})


if __name__ == '__main__':
    main()
