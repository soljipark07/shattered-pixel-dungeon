#!/usr/bin/env python3
"""Compile real game sources and run headless regression without requiring Android SDK."""
import argparse
import os
from pathlib import Path
import subprocess
import urllib.request

ROOT = Path(__file__).resolve().parents[2]
DEPS = {
    'ecj.jar': 'org/eclipse/jdt/ecj/3.38.0/ecj-3.38.0.jar',
    'gdx.jar': 'com/badlogicgames/gdx/gdx/1.14.0/gdx-1.14.0.jar',
    'gdx-freetype.jar': 'com/badlogicgames/gdx/gdx-freetype/1.14.0/gdx-freetype-1.14.0.jar',
    'controllers.jar': 'com/badlogicgames/gdx-controllers/gdx-controllers-core/2.2.4/gdx-controllers-core-2.2.4.jar',
    'json.jar': 'org/json/json/20170516/json-20170516.jar',
    'headless.jar': 'com/badlogicgames/gdx/gdx-backend-headless/1.14.0/gdx-backend-headless-1.14.0.jar',
    'jnigen.jar': 'com/badlogicgames/gdx/gdx-jnigen-loader/2.5.2/gdx-jnigen-loader-2.5.2.jar',
    'natives.jar': 'com/badlogicgames/gdx/gdx-platform/1.14.0/gdx-platform-1.14.0-natives-desktop.jar',
}

parser = argparse.ArgumentParser()
parser.add_argument('mode', choices=['reference', 'check', 'start', 'visits', 'no-free-badge', 'landing', 'suite'])
parser.add_argument('--source-root', type=Path, default=ROOT)
parser.add_argument('--fixtures', type=Path, default=ROOT / 'build/imp-regression/upstream.json')
args = parser.parse_args()
deps = Path(os.environ.get('IMP_TEST_DEPS', ROOT / 'build/imp-regression/deps'))
deps.mkdir(parents=True, exist_ok=True)
for name, url in DEPS.items():
    dest = deps / name
    if not dest.exists():
        with urllib.request.urlopen('https://repo.maven.apache.org/maven2/' + url, timeout=60) as response:
            dest.write_bytes(response.read())

out = ROOT / 'build/imp-regression' / ('reference-classes' if args.mode == 'reference' else 'variant-classes')
out.mkdir(parents=True, exist_ok=True)
classpath = os.pathsep.join(str(deps / name) for name in DEPS if name != 'ecj.jar')
sources = []
for directory in ['SPD-classes/src/main/java', 'core/src/main/java', 'services/src/main/java']:
    sources.extend(sorted((args.source_root / directory).rglob('*.java')))
sources.append(ROOT / 'tools/imp-travel/ImpTravelRegression.java')
argfile = out / 'sources.txt'
argfile.write_text('\n'.join('"' + str(p) + '"' for p in sources))
subprocess.run(['java', '-jar', str(deps / 'ecj.jar'), '-11', '-encoding', 'UTF-8', '-warn:none',
                '-cp', classpath, '-d', str(out), '@' + str(argfile)], check=True)
runtime = os.pathsep.join([str(out), str(args.source_root / 'core/src/main/assets'), classpath])
modes = ['start', 'visits', 'no-free-badge', 'landing', 'check'] if args.mode == 'suite' else [args.mode]
for mode in modes:
    subprocess.run(['java', '-Xmx1500m', '-cp', runtime, 'ImpTravelRegression',
                    mode, str(args.fixtures.resolve())], check=True)
