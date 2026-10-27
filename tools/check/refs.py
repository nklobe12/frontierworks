#!/usr/bin/env python3
"""Cross-reference check over the generated pack.

Finds every fw_* registry name (items, blocks, fluids, entities, ...) the generator registered and
reports references to fw_* ids that do not exist: in recipes, loot tables, tags, models,
blockstates, worldgen JSON and structure templates. Run after tools/build.py.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))

REG_RE = re.compile(r'REG\.(item|block|blockOnly|blockEntity|entity|menu|machineRecipe|sound|fluid\w*|effect|potion|enchantment|feature|poi|profession)\w*\("([a-z0-9_/]+)"')
ID_RE = re.compile(r'^(fw_[a-z0-9_]+):([a-z0-9_/.]+)$')
KEYS = ('item', 'result', 'name', 'Name', 'block', 'fluid', 'id', 'type', 'feature', 'sapling', 'tag', 'location', 'values', 'structure',
        'biome', 'biomes', 'features', 'entity', 'drop', 'loot_table_id', 'table', 'state', 'pool', 'start_pool', 'trunk_provider',
        'foliage_provider', 'Name')


def main():
    from gen import fw, structures  # noqa
    import importlib
    from mods import ORDER
    for name in ORDER:
        importlib.import_module('mods.' + name).build()
    mods = fw.ALL_MODS
    known = set()
    files = {}
    for m in mods:
        src = '\n'.join(m.fields)
        for kind, n in REG_RE.findall(src):
            known.add('%s:%s' % (m.modid, n))
        for rel in m.files:
            files[(m.modid, rel)] = m.files[rel]
        # data files define ids too (worldgen, loot tables, structures, recipes types)
        for rel in m.files:
            parts = rel.split('/')
            if parts[0] == 'data' and parts[1] == m.modid:
                path = '/'.join(parts[2:])
                for prefix in ('worldgen/configured_feature/', 'worldgen/placed_feature/', 'worldgen/structure/', 'worldgen/template_pool/',
                               'worldgen/biome/', 'worldgen/noise_settings/', 'dimension/', 'dimension_type/', 'loot_tables/',
                               'worldgen/structure_set/', 'worldgen/processor_list/', 'worldgen/density_function/'):
                    if path.startswith(prefix):
                        rest = path[len(prefix):].rsplit('.', 1)[0]
                        known.add('%s:%s' % (m.modid, rest))
                if path.startswith('tags/'):
                    segs = path.split('/')
                    n = 3 if segs[1] == 'worldgen' else 2
                    known.add('#%s:%s' % (m.modid, '/'.join(segs[n:]).rsplit('.', 1)[0]))
                if path.startswith('structures/'):
                    known.add('%s:%s' % (m.modid, path[len('structures/'):].rsplit('.', 1)[0]))
            if parts[0] == 'assets' and parts[1] == m.modid and parts[2] == 'textures':
                known.add('%s:%s' % (m.modid, '/'.join(parts[3:]).rsplit('.', 1)[0]))
            if parts[0] == 'assets' and parts[1] == m.modid and parts[2] == 'models':
                known.add('%s:%s' % (m.modid, '/'.join(parts[3:]).rsplit('.', 1)[0]))
        # fluids: REG fluid helpers register "<name>" and "flowing_<name>"
        for line in m.fields:
            mm = re.search(r'new FWFluid\(REG, "([a-z0-9_]+)"', line)
            if mm:
                for v in (mm.group(1), 'flowing_' + mm.group(1), mm.group(1) + '_bucket', mm.group(1) + '_block'):
                    known.add('%s:%s' % (m.modid, v))
    for m in mods:
        for t in m.tags:
            known.add('#%s:%s' % (t[1], t[2]))
    problems = []

    def walk(o, where, key=None):
        if isinstance(o, dict):
            for k, v in o.items():
                walk(v, where, k)
        elif isinstance(o, list):
            for v in o:
                walk(v, where, key)
        elif isinstance(o, str):
            s = o[1:] if o.startswith('#') else o
            s = s.split('[')[0]
            mm = ID_RE.match(s)
            if mm and (key in KEYS or key is None):
                cand = {s, '#' + s, s.replace('block/', '', 1), s.replace('item/', '', 1)}
                if not any(c in known for c in cand) and not s.startswith('fw_core:') and not s.startswith('fw_selftest'):
                    problems.append('%s: %s=%s' % (where, key, o))

    for (modid, rel), content in files.items():
        if isinstance(content, (dict, list)) and rel.endswith('.json'):
            if '/lang/' in rel:
                continue
            walk(content, '%s/%s' % (modid, rel))
    for (modid, block) in sorted(structures.USED_BLOCKS):
        if block.startswith('fw_') and block not in known:
            problems.append('%s template uses unknown block %s' % (modid, block))
    for (modid, table) in sorted(structures.USED_LOOT):
        if table not in known:
            problems.append('%s template uses unknown loot table %s' % (modid, table))
    for p in sorted(set(problems)):
        print(p)
    print('%d problems' % len(set(problems)))
    return 1 if problems else 0


if __name__ == '__main__':
    sys.exit(main())
