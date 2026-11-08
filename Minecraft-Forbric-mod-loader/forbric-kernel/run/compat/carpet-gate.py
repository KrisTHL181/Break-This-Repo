#!/usr/bin/env python3
"""Compile a real Carpet behavior probe and compare repaired and disabled-adapter server runs."""
import argparse
from datetime import datetime
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import zipfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--carpet', type=Path, required=True, help='Unmodified fabric-carpet-26.2+v260616.jar')
    parser.add_argument('--staged-root', type=Path, help='The shared forbric-loader/run directory')
    parser.add_argument('--output', type=Path, help='A new directory for logs, worlds and reports')
    args = parser.parse_args()
    kernel = Path(__file__).resolve().parents[2]
    stage = (args.staged_root or Path(os.environ.get('FORBRIC_OLD', kernel.parent / 'forbric-loader')) / 'run').resolve()
    mc = Path(os.environ.get('MC_DIR', Path.home() / 'Library/Application Support/minecraft'))
    carpet = args.carpet.resolve()
    with zipfile.ZipFile(carpet) as jar:
        metadata = json.loads(jar.read('fabric.mod.json'))
        if metadata.get('id') != 'carpet' or metadata.get('version') != '26.2+v260616':
            parser.error('This gate targets Carpet 26.2+v260616')
    output = (args.output or kernel / 'build/verification' / ('carpet-' + datetime.now().strftime('%Y%m%d-%H%M%S'))).resolve()
    output.mkdir(parents=True, exist_ok=False)
    root = kernel / 'canary/carpet'
    game = stage / 'neoforge-patched/patched-mc-neoforge-26.2.jar'
    forge = stage / 'merged-base/forge-runtime-interop.jar'
    neo = stage / 'neoforge-runtime/neoforge-runtime.jar'
    cp = [game, forge, neo, carpet]
    for lib in json.loads((mc / 'versions/26.2/26.2.json').read_text())['libraries']:
        path = lib.get('downloads', {}).get('artifact', {}).get('path')
        if path and (mc / 'libraries' / path).is_file():
            cp.append(mc / 'libraries' / path)
    for path in cp[:4]:
        if not path.is_file():
            parser.error(f'Missing prerequisite: {path}')
    with tempfile.TemporaryDirectory(prefix='carpet-probe-', dir=output) as temp:
        classes = Path(temp)
        subprocess.run(['javac', '-proc:none', '--release', '21', '-cp', os.pathsep.join(map(str, cp)),
                        '-d', str(classes), *map(str, sorted((root / 'src').rglob('*.java')))], check=True)
        probe = output / 'forbriccarpetprobe.jar'
        with zipfile.ZipFile(probe, 'w', zipfile.ZIP_DEFLATED) as jar:
            for path in sorted(classes.rglob('*.class')):
                jar.write(path, path.relative_to(classes).as_posix())
            jar.write(root / 'META-INF/neoforge.mods.toml', 'META-INF/neoforge.mods.toml')
    results = {}
    for phase in ('baseline', 'fixed'):
        run = output / phase
        (run / 'mods').mkdir(parents=True)
        (run / 'world/scripts').mkdir(parents=True)
        shutil.copy2(probe, run / 'mods' / probe.name)
        shutil.copy2(carpet, run / 'mods' / carpet.name)
        shutil.copy2(root / 'forbric_carpet_probe.sc', run / 'world/scripts')
        (run / 'server.properties').write_text(
            'server-ip=127.0.0.1\nserver-port=0\nlevel-name=world\nlevel-type=minecraft:flat\n'
            'generate-structures=false\nonline-mode=false\nmax-tick-time=-1\npause-when-empty-seconds=0\n'
            'view-distance=2\nsimulation-distance=2\nspawn-protection=0\n')
        env = dict(os.environ, FORBRIC_OLD=str(stage.parent), RUNDIR=str(run),
                   FORBRIC_COMPAT_POLICY='strict' if phase == 'fixed' else 'continue',
                   FORBRIC_JVM='-Xmx2G' + (' -Dforbric.carpetMixins=off' if phase == 'baseline' else ''))
        with (run / 'console.log').open('w') as log:
            subprocess.run([str(kernel / 'run/launch-kernel-server.sh')], env=env,
                           stdin=subprocess.DEVNULL, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=180)
        text = (run / 'console.log').read_text()
        if 'All dimensions are saved' not in text:
            raise RuntimeError(f'{phase}: server did not complete normal shutdown')
        report = json.loads((run / 'carpet-probe.json').read_text())
        cases = report['cases']
        if len(cases) != 22 or len({c['name'] for c in cases}) != 22:
            raise RuntimeError(f'{phase}: expected all 22 distinct behavior checks')
        failed = {c['name'] for c in cases if not c['pass']}
        results[phase] = {'passed': len(cases) - len(failed), 'failed': sorted(failed)}
        if phase == 'baseline':
            expected = {'fill.shape.false', 'fluid.blackstone.true', 'fluid.deepslate.true',
                        'fluid.blackstone.neighbor', 'fluid.deepslate.neighbor',
                        'swap.scarpetCancel.true', 'break.creative.scarpetCancel.true', 'break.survival.scarpetCancel.true'}
            if not expected <= failed or 'swap.nativeVeto' in failed or 'break.nativeVeto' in failed:
                raise RuntimeError(f'Negative control failed: {failed}')
        else:
            compatibility = json.loads((run / '.forbric-kernel/compatibility-report.json').read_text())
            confirmed = [f for f in compatibility['findings'] if f.get('modId') == 'carpet' and f.get('confidence') == 'CONFIRMED']
            if failed or confirmed or compatibility['policy'] != 'STRICT':
                raise RuntimeError(f'Fixed run failed: cases={failed}, compatibility={confirmed}')
        print(f'{phase}: {len(cases) - len(failed)}/{len(cases)} behavior checks passed', flush=True)
    results['carpet_sha256'] = hashlib.sha256(carpet.read_bytes()).hexdigest()
    results['kernel_sha256'] = hashlib.sha256((kernel / 'build/libs/forbric-kernel-0.1.0-SNAPSHOT.jar').read_bytes()).hexdigest()
    (output / 'summary.json').write_text(json.dumps(results, indent=2) + '\n')
    print(f'Carpet behavior gate passed: {output}', flush=True)


if __name__ == '__main__':
    main()
