"""Structure templates (NBT) + jigsaw structure/pool/set JSON generation."""
import math
import random

from . import nbt
from .fw import mc

DATA_VERSION = 3465  # Minecraft 1.20.1

AIR = 'minecraft:air'

# (modid of the template's mod, block id) pairs used in templates - checked by tools/check/refs.py
USED_BLOCKS = set()
# loot table ids referenced from template chests: (modid, table)
USED_LOOT = set()

LAND = ['#minecraft:is_forest', '#minecraft:is_taiga', '#minecraft:is_savanna', '#minecraft:is_badlands', '#minecraft:is_jungle',
        '#forge:is_plains', '#forge:is_desert', 'minecraft:meadow', 'minecraft:snowy_plains', 'minecraft:windswept_hills',
        'minecraft:sunflower_plains', 'minecraft:cherry_grove']
FLAT_LAND = ['#forge:is_plains', '#forge:is_desert', '#minecraft:is_savanna', 'minecraft:meadow', 'minecraft:snowy_plains',
             'minecraft:sunflower_plains', 'minecraft:forest', 'minecraft:birch_forest']
FACINGS = ('north', 'east', 'south', 'west')


def parse_state(s):
    if '[' in s:
        name, props = s[:-1].split('[', 1)
        p = dict(kv.split('=', 1) for kv in props.split(',') if kv)
    else:
        name, p = s, {}
    return mc(name), p


class Struct:
    def __init__(self, sx, sy, sz, seed=0):
        self.size = (sx, sy, sz)
        self.blocks = {}
        self.rng = random.Random(seed)

    def inside(self, x, y, z):
        return 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]

    def set(self, x, y, z, state, data=None):
        if callable(state):
            state = state()
        if self.inside(x, y, z):
            self.blocks[(x, y, z)] = (state, data)

    def get(self, x, y, z):
        b = self.blocks.get((x, y, z))
        return b[0] if b else None

    def fill(self, x0, y0, z0, x1, y1, z1, state, hollow=False, chance=1.0, only_empty=False):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    if hollow and not (x in (x0, x1) or y in (y0, y1) or z in (z0, z1)):
                        continue
                    if chance < 1.0 and self.rng.random() > chance:
                        continue
                    if only_empty and (x, y, z) in self.blocks and self.blocks[(x, y, z)][0] != AIR:
                        continue
                    st = state() if callable(state) else state
                    self.set(x, y, z, st)

    def clear(self, x0, y0, z0, x1, y1, z1):
        self.fill(x0, y0, z0, x1, y1, z1, AIR)

    def walls(self, x0, y0, z0, x1, y1, z1, state):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, z0, state() if callable(state) else state)
                self.set(x, y, z1, state() if callable(state) else state)
            for z in range(z0, z1 + 1):
                self.set(x0, y, z, state() if callable(state) else state)
                self.set(x1, y, z, state() if callable(state) else state)

    def room(self, x0, y0, z0, x1, y1, z1, wall, floor=None, ceiling=None):
        """Hollow box: floor, walls, ceiling, air inside."""
        self.fill(x0, y0, z0, x1, y1, z1, wall)
        self.clear(x0 + 1, y0 + 1, z0 + 1, x1 - 1, y1 - 1, z1 - 1)
        if floor:
            self.fill(x0 + 1, y0, z0 + 1, x1 - 1, y0, z1 - 1, floor)
        if ceiling:
            self.fill(x0 + 1, y1, z0 + 1, x1 - 1, y1, z1 - 1, ceiling)

    def chest(self, x, y, z, loot, facing='south', kind='chest'):
        if kind == 'barrel':
            self.set(x, y, z, 'minecraft:barrel[facing=up,open=false]', {'LootTable': loot})
        else:
            self.set(x, y, z, 'minecraft:chest[facing=%s,type=single,waterlogged=false]' % facing, {'LootTable': loot})

    def spawner(self, x, y, z, entity, delay=20):
        self.set(x, y, z, 'minecraft:spawner', {
            'SpawnData': {'entity': {'id': entity}},
            'SpawnPotentials': nbt.List(nbt.COMPOUND, [{'weight': nbt.Int(1), 'data': {'entity': {'id': entity}}}]),
            'Delay': nbt.Short(delay), 'MinSpawnDelay': nbt.Short(200), 'MaxSpawnDelay': nbt.Short(800),
            'SpawnCount': nbt.Short(3), 'MaxNearbyEntities': nbt.Short(6), 'RequiredPlayerRange': nbt.Short(16),
            'SpawnRange': nbt.Short(4)})

    def jigsaw(self, x, y, z, facing, name, target, pool, final=AIR, up='up'):
        orient = '%s_%s' % (facing, up) if facing not in ('up', 'down') else '%s_%s' % (facing, 'north')
        self.set(x, y, z, 'minecraft:jigsaw[orientation=%s]' % orient,
                 {'name': name, 'target': target, 'pool': pool, 'final_state': final, 'joint': 'rollable' if facing in ('up', 'down') else 'aligned'})

    def decay(self, remove=0.0, replace=None, protect=('minecraft:chest', 'minecraft:spawner', 'minecraft:jigsaw', 'minecraft:barrel'),
              y_bias=0.0, min_y=0):
        """Random ruination: remove blocks (more near the top with y_bias) and swap variants."""
        replace = replace or {}
        top = self.size[1]
        for pos, (state, data) in list(self.blocks.items()):
            if state == AIR or any(state.startswith(p) for p in protect) or pos[1] < min_y:
                continue
            p = remove + y_bias * (pos[1] / max(1, top))
            if self.rng.random() < p:
                self.blocks[pos] = (AIR, None)
                continue
            base = state.split('[')[0]
            if base in replace and self.rng.random() < replace[base][1]:
                self.blocks[pos] = (replace[base][0], data)

    def stamp(self, other, ox, oy, oz):
        for (x, y, z), b in other.blocks.items():
            self.set(x + ox, y + oy, z + oz, b[0], b[1])

    def to_nbt(self):
        palette = []
        index = {}
        blocks = nbt.List(nbt.COMPOUND)
        for pos in sorted(self.blocks, key=lambda p: (p[1], p[2], p[0])):
            state, data = self.blocks[pos]
            if state not in index:
                name, props = parse_state(state)
                entry = {'Name': name}
                if props:
                    entry['Properties'] = {k: str(v) for k, v in props.items()}
                index[state] = len(palette)
                palette.append(entry)
            b = {'pos': nbt.List(nbt.INT, [nbt.Int(pos[0]), nbt.Int(pos[1]), nbt.Int(pos[2])]), 'state': nbt.Int(index[state])}
            if data:
                b['nbt'] = data
            blocks.append(b)
        root = {
            'DataVersion': nbt.Int(DATA_VERSION),
            'size': nbt.List(nbt.INT, [nbt.Int(v) for v in self.size]),
            'palette': nbt.List(nbt.COMPOUND, palette),
            'blocks': blocks,
            'entities': nbt.List(nbt.COMPOUND, []),
        }
        return nbt.dumps(root)


# ----------------------------------------------------------------------------- registration

def pool_json(m, elements, fallback='minecraft:empty'):
    return {'fallback': fallback, 'elements': [
        {'weight': w, 'element': {'element_type': 'minecraft:single_pool_element', 'location': loc,
                                  'projection': 'rigid', 'processors': 'minecraft:empty'}} for (loc, w) in elements]}


def add_pool(m, pool_name, variants, fallback='minecraft:empty'):
    """variants: list of (Struct, weight). Writes templates under structures/<pool_name>/<i>.nbt."""
    elements = []
    for i, (st, w) in enumerate(variants):
        loc = '%s/%d' % (pool_name, i)
        m.data('structures/%s.nbt' % loc, st.to_nbt())
        for (state, data) in st.blocks.values():
            USED_BLOCKS.add((m.modid, parse_state(state)[0]))
            if data and 'LootTable' in data:
                USED_LOOT.add((m.modid, data['LootTable']))
            if data and isinstance(data.get('final_state'), str):
                USED_BLOCKS.add((m.modid, parse_state(data['final_state'])[0]))
        elements.append((m.rl(loc), w))
    m.data('worldgen/template_pool/%s.json' % pool_name, pool_json(m, elements, fallback))
    return m.rl(pool_name)


def structure(m, name, variants, biomes, spacing=32, separation=10, salt=None, step='surface_structures', terrain='beard_thin',
              height=0, heightmap='WORLD_SURFACE_WG', dim='minecraft:overworld', size=1, max_distance=80, start_pool=None,
              spawn_overrides=None):
    """Registers a jigsaw structure with its start pool (unless start_pool given), structure set and biome tag."""
    if start_pool is None:
        start_pool = add_pool(m, name + '/start', variants)
    if isinstance(biomes, str):
        biomes = [biomes]
    m.data('tags/worldgen/biome/has_structure/%s.json' % name, {'replace': False, 'values': biomes})
    sh = {'absolute': height} if isinstance(height, int) else height
    obj = {'type': 'minecraft:jigsaw', 'biomes': '#%s:has_structure/%s' % (m.modid, name), 'step': step,
           'spawn_overrides': spawn_overrides or {}, 'terrain_adaptation': terrain, 'start_pool': start_pool, 'size': size,
           'start_height': sh, 'max_distance_from_center': max_distance, 'use_expansion_hack': False}
    if heightmap:
        obj['project_start_to_heightmap'] = heightmap
    m.data('worldgen/structure/%s.json' % name, obj)
    if salt is None:
        salt = (sum(ord(c) * (i + 7) for i, c in enumerate(m.modid + name)) * 7919) % 2000000000
    m.data('worldgen/structure_set/%s.json' % name, {'structures': [{'structure': m.rl(name), 'weight': 1}],
                                                     'placement': {'type': 'minecraft:random_spread', 'spacing': spacing,
                                                                   'separation': separation, 'salt': salt}})
    m.selftest['structures'].append({'id': m.rl(name), 'dim': dim})
    m.tag('worldgen/structure', 'fw_core:explorable', m.rl(name))
    return m.rl(name)


def loot_table(m, path, pools):
    """pools: list of (min_rolls, max_rolls, [(item, weight, min, max)])."""
    out = []
    for (lo, hi, entries) in pools:
        es = []
        for e in entries:
            item, weight = e[0], e[1]
            cmin, cmax = (e[2], e[3]) if len(e) > 3 else (1, 1)
            entry = {'type': 'minecraft:item', 'name': mc(item), 'weight': weight}
            fns = []
            if (cmin, cmax) != (1, 1):
                fns.append({'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': cmin, 'max': cmax}, 'add': False})
            if len(e) > 4 and e[4] == 'enchant':
                fns.append({'function': 'minecraft:enchant_randomly'})
            if fns:
                entry['functions'] = fns
            es.append(entry)
        out.append({'rolls': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}, 'bonus_rolls': 0, 'entries': es})
    m.data('loot_tables/%s.json' % path, {'type': 'minecraft:chest', 'pools': out})
    return m.rl(path)


# ----------------------------------------------------------------------------- reusable building pieces

def stairs(material, facing, half='bottom'):
    return 'minecraft:%s_stairs[facing=%s,half=%s,shape=straight,waterlogged=false]' % (material, facing, half)


def foundation(s, x0, z0, x1, z1, y, state, depth=4):
    """Pillar-like foundation down from y so buildings don't float on slopes (structure 'beard' helps too)."""
    for d in range(depth):
        s.fill(x0, y - d, z0, x1, y - d, z1, state, chance=1.0 if d == 0 else 0.85)


def disc(s, cx, cz, y, r, state, chance=1.0):
    for x in range(cx - r, cx + r + 1):
        for z in range(cz - r, cz + r + 1):
            if (x - cx) ** 2 + (z - cz) ** 2 <= r * r + r and s.rng.random() <= chance:
                s.set(x, y, z, state() if callable(state) else state)


def sphere(s, cx, cy, cz, r, state, hollow=False, chance=1.0):
    for x in range(int(cx - r), int(cx + r) + 1):
        for y in range(int(cy - r), int(cy + r) + 1):
            for z in range(int(cz - r), int(cz + r) + 1):
                d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2 + (z - cz) ** 2)
                if d <= r and (not hollow or d > r - 1.3) and s.rng.random() <= chance:
                    s.set(x, y, z, state() if callable(state) else state)
