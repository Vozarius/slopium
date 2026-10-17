"""Compare the supplied post-installation capture with the original two profiles."""
import json
import argparse
from pathlib import Path
from datetime import datetime, timezone, timedelta
from analyze_profiles import ROOT, OUT, read_profile, flatten, total, summary

TARGETS = {
    'render': 'net.minecraft.client.renderer.GameRenderer.render',
    'tick': 'net.minecraft.client.Minecraft.tick',
    'block_entity_tick': 'net.minecraft.world.level.Level.tickBlockEntities',
    'create_tick': 'com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker.tick',
    'block_entity_render': 'net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher.render',
    'dynamic_light': 'toni.sodiumdynamiclights.SodiumDynamicLights.getDynamicLightLevel',
    'breaking': 'net.minecraft.client.renderer.block.BlockRenderDispatcher.renderBreakingTexture',
    'dh_lod': 'com.seibel.distanthorizons.core.api.internal.ClientApi.renderLodLayer',
    'flywheel_render': 'dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl.render',
    'swap': 'org.lwjgl.glfw.GLFW.glfwSwapBuffers',
    'slopium_tick_check': ('dev.slopium.visibility.CreateTickControl.skip', 'dev.slopium.visibility.CreateTickControl.skipEligible'),
    'slopium_grid_build': 'dev.slopium.light.LightGrid.<init>',
    'slopium_grid_sample': 'dev.slopium.light.LightGrid.sample',
}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('profiles', type=Path, nargs='+', help='Captures after installation, in order; the final one gets a detailed summary')
    parser.add_argument('--output-prefix', default='post-slopium')
    args = parser.parse_args()
    new = args.profiles[-1]
    paths = sorted((ROOT / 'reference/profilers').glob('*')) + args.profiles
    results = []
    for path in paths:
        if path.suffix not in ('.json', '.sparkprofile'): continue
        data = read_profile(path)
        thread = next(t for t in data['threads'] if t['name'] == 'Render thread')
        rows = flatten(thread)
        denominator = total(thread)
        groups = {}
        for key, value in TARGETS.items():
            labels = (value,) if isinstance(value, str) else value
            groups[key] = [r for r in rows if r['label'] in labels and not any(label in r['path'][:-1] for label in labels)]
        groups['slopium_tick_hook'] = [r for r in rows if r['label'].endswith(('$slopium$skipHiddenClientTick', '$slopium$allowClientTick'))]
        metrics = {key: sum(r['inclusive_ms'] for r in value) / denominator * 100 for key, value in groups.items()}
        windows = []
        for wi, wid in enumerate(data['timeWindows']):
            stats = data['timeWindowStatistics'][str(wid)]
            sampled = thread['times'][wi]
            window = {'start_msk': datetime.fromtimestamp(stats['startTime'] / 1000, timezone(timedelta(hours=3))).isoformat(),
                      'sampled_s': sampled / 1000, 'statistics': stats,
                      'metrics_pct': {key: sum(r['times'][wi] for r in value) / sampled * 100 for key, value in groups.items()}}
            windows.append(window)
        info = {'file': path.name, 'sampled_ms': denominator, 'metrics_pct': metrics, 'windows': windows}
        results.append(info)
        print(path.name, json.dumps({k:round(v,3) for k,v in metrics.items()}))
        if path == new:
            s = summary(path, data)
            (OUT / (path.stem + '-summary.json')).write_text(json.dumps(s, ensure_ascii=False, indent=2), encoding='utf-8')
            print('SYSTEM', json.dumps(s['system'], ensure_ascii=False))
            print('PLATFORM', json.dumps(s['platformStatistics'], ensure_ascii=False))
            print('TOP SELF', json.dumps(s['threads'][0]['top_self'][:35], ensure_ascii=False))
            print('NEW WINDOWS',json.dumps(windows))
    (OUT / (args.output_prefix + '-comparison.json')).write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding='utf-8')

if __name__ == '__main__': main()
