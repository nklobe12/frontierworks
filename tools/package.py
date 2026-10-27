#!/usr/bin/env python3
"""Packages the built mod jars into ready-to-import modpacks.

Usage: python3 tools/package.py <forge version, e.g. 47.3.0> <jar dir> <out dir>
Produces: CurseForge zip, Prism/MultiMC instance zip, Modrinth .mrpack, server pack zip and a plain mods zip.
"""
import json
import os
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
NAME = 'Lodestar'
MC = '1.20.1'


def pack_version():
    for line in (ROOT / 'gradle.properties').read_text().splitlines():
        if line.startswith('pack_version='):
            return line.split('=', 1)[1].strip()
    return '1.0.0'


def add_jars(z, jars, prefix):
    for j in jars:
        z.write(j, prefix + j.name)


def main():
    forge, jar_dir, out = sys.argv[1], Path(sys.argv[2]), Path(sys.argv[3])
    out.mkdir(parents=True, exist_ok=True)
    ver = pack_version()
    jars = sorted(jar_dir.glob('*.jar'))
    assert jars, 'no jars in %s' % jar_dir
    base = '%s-%s' % (NAME, ver)
    readme = (ROOT / 'MODS.md').read_text() if (ROOT / 'MODS.md').exists() else ''

    # CurseForge (also imports into the CurseForge app / most launchers)
    with zipfile.ZipFile(out / (base + '-curseforge.zip'), 'w', zipfile.ZIP_DEFLATED) as z:
        z.writestr('manifest.json', json.dumps({
            'minecraft': {'version': MC, 'modLoaders': [{'id': 'forge-' + forge, 'primary': True}]},
            'manifestType': 'minecraftModpack', 'manifestVersion': 1, 'name': NAME, 'version': ver,
            'author': 'Klobe & Claude', 'files': [], 'overrides': 'overrides'}, indent=2))
        z.writestr('modlist.md', readme)
        add_jars(z, jars, 'overrides/mods/')

    # Prism Launcher / MultiMC instance
    with zipfile.ZipFile(out / (base + '-prism.zip'), 'w', zipfile.ZIP_DEFLATED) as z:
        z.writestr('instance.cfg', '\n'.join(['InstanceType=OneSix', 'name=%s %s' % (NAME, ver), 'iconKey=default',
                                               'OverrideMemory=true', 'MinMemAlloc=2048', 'MaxMemAlloc=6144', '']))
        z.writestr('mmc-pack.json', json.dumps({'formatVersion': 1, 'components': [
            {'uid': 'net.minecraft', 'version': MC, 'important': True, 'cachedName': 'Minecraft'},
            {'uid': 'net.minecraftforge', 'version': forge, 'cachedName': 'Forge'}]}, indent=2))
        add_jars(z, jars, '.minecraft/mods/')

    # Modrinth App
    with zipfile.ZipFile(out / (base + '.mrpack'), 'w', zipfile.ZIP_DEFLATED) as z:
        z.writestr('modrinth.index.json', json.dumps({'formatVersion': 1, 'game': 'minecraft', 'versionId': ver, 'name': NAME,
                                                      'summary': '114 tech, exploration and adventure mods for Forge 1.20.1',
                                                      'files': [], 'dependencies': {'minecraft': MC, 'forge': forge}}, indent=2))
        add_jars(z, jars, 'overrides/mods/')

    # Dedicated server pack
    installer = 'https://maven.minecraftforge.net/net/minecraftforge/forge/{v}/forge-{v}-installer.jar'.format(v='%s-%s' % (MC, forge))
    with zipfile.ZipFile(out / (base + '-server.zip'), 'w', zipfile.ZIP_DEFLATED) as z:
        add_jars(z, jars, 'mods/')
        sh = '\n'.join(['#!/usr/bin/env bash', 'set -e', 'cd "$(dirname "$0")"',
                        'if [ ! -f run.sh ]; then', '  curl -L -o forge-installer.jar "%s"' % installer,
                        '  java -jar forge-installer.jar --installServer', '  rm -f forge-installer.jar', 'fi',
                        'echo "eula=true" > eula.txt', 'echo "-Xmx6G" > user_jvm_args.txt', 'chmod +x run.sh', './run.sh nogui', ''])
        zi = zipfile.ZipInfo('start.sh')
        zi.external_attr = 0o755 << 16
        z.writestr(zi, sh)
        bat = '\r\n'.join(['@echo off', 'cd /d "%~dp0"', 'if not exist run.bat (',
                           '  powershell -Command "Invoke-WebRequest -Uri %s -OutFile forge-installer.jar"' % installer,
                           '  java -jar forge-installer.jar --installServer', '  del forge-installer.jar', ')',
                           'echo eula=true> eula.txt', 'echo -Xmx6G> user_jvm_args.txt', 'call run.bat nogui', ''])
        z.writestr('start.bat', bat)
        z.writestr('README.txt', 'Lodestar %s server pack (Minecraft %s, Forge %s).\n\nRun start.sh (Linux/macOS) or start.bat (Windows). '
                                 'The first run downloads and installs Forge, then starts the server. Java 17 is required.\n' % (ver, MC, forge))

    # Just the jars
    with zipfile.ZipFile(out / (base + '-mods.zip'), 'w', zipfile.ZIP_DEFLATED) as z:
        add_jars(z, jars, 'mods/')
    for f in sorted(out.iterdir()):
        print('%-48s %6.1f MB' % (f.name, f.stat().st_size / 1e6))


if __name__ == '__main__':
    main()
