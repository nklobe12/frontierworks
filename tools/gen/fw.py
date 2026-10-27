"""Frontierworks mod generator: specs -> Java registration code, assets, data and textures."""
import io
import json
import os
import re
import shutil
from collections import defaultdict, OrderedDict
from pathlib import Path

from PIL import Image

from . import tex

REPO = Path(__file__).resolve().parents[2]
ALL_MODS = []
PACK_VERSION = '1.0.0'
AUTHORS = 'Klobe & Claude'

TAB_NAMES = {'MATERIALS', 'MACHINES', 'LOGISTICS', 'TOOLS', 'ADVENTURE', 'WORLD', 'DECOR', 'SPACE'}

SMALL_WORDS = {'of', 'the', 'and', 'a', 'an', 'in', 'on', 'to', 'for'}
ACRONYMS = {'xp': 'XP', 'fe': 'FE', 'ui': 'UI', 'tnt': 'TNT', 'mre': 'MRE', 'c4': 'C4', 'he3': 'He-3', 't1': 'T1', 't2': 'T2', 't3': 'T3',
            'mk1': 'Mk I', 'mk2': 'Mk II', 'mk3': 'Mk III', 'ii': 'II', 'iii': 'III', 'iv': 'IV'}


def pascal(s):
    return ''.join(p[:1].upper() + p[1:] for p in re.split(r'[_\W]+', s) if p)


def const(s):
    return re.sub(r'\W', '_', s).upper()


def title(s):
    words = s.split('_')
    out = []
    for i, w in enumerate(words):
        if w in ACRONYMS:
            out.append(ACRONYMS[w])
        elif i > 0 and w in SMALL_WORDS:
            out.append(w)
        else:
            out.append(w[:1].upper() + w[1:])
    return ' '.join(out)


def I(x):
    """Ingredient JSON from a short string: '#tag', 'ns:item' or list of alternatives."""
    if isinstance(x, (list, tuple)):
        return [I(v) for v in x]
    if isinstance(x, dict):
        return x
    if x.startswith('#'):
        return {'tag': x[1:]}
    return {'item': x if ':' in x else 'minecraft:' + x}


def mc(x):
    return x if ':' in x else 'minecraft:' + x


def resolve_tex(t):
    if t is None or isinstance(t, Image.Image):
        return t
    if isinstance(t, tuple):
        kind = t[0]
        if kind in tex.T:
            return tex.item(kind, *t[1:])
        fn = getattr(tex, kind)
        return fn(*t[1:])
    if callable(t):
        return t()
    raise ValueError('bad texture spec %r' % (t,))


class Mod:
    def __init__(self, modid, name, desc, deps=(), tab='MATERIALS'):
        assert modid.startswith('fw_'), modid
        self.modid = modid
        self.pkg = modid[3:]
        self.pascal = pascal(self.pkg)
        self.name = name
        self.desc = desc
        self.deps = list(deps)  # hard (Java) dependencies, besides fw_core
        self.tab = tab
        self.fields = []
        self.imports = set()
        self.inits = []
        self.client_inits = []
        self.lang = OrderedDict()
        self.files = OrderedDict()
        self.tags = defaultdict(list)
        self.loot_mods = []
        self.biome_mods = defaultdict(list)
        self.selftest = {'structures': []}
        self.mine = defaultdict(list)
        self.items_order = []
        self.recipe_names = set()
        self.java_files = OrderedDict()  # extra generated Java: relative path under com/frontierworks/<pkg>/ -> source
        ALL_MODS.append(self)

    # ------------------------------------------------------------------ basics
    def rl(self, name):
        return '%s:%s' % (self.modid, name)

    def put(self, rel, content):
        self.files[rel] = content

    def asset(self, rel, content):
        self.put('assets/%s/%s' % (self.modid, rel), content)

    def data(self, rel, content, ns=None):
        self.put('data/%s/%s' % (ns or self.modid, rel), content)

    def texture(self, path, img):
        img = resolve_tex(img)
        if img is not None:
            self.asset('textures/%s.png' % path, img)

    def field(self, line):
        self.fields.append(line)

    def java_file(self, rel, source):
        """Extra generated Java source, rel is relative to the mod package directory (e.g. 'client/RocketModel.java')."""
        self.java_files[rel] = source

    def model(self, box, subpkg='client', seed=None):
        """Generated entity model: writes the Java class and its texture (textures/entity/<layer>.png)."""
        self.java_file('%s/%s.java' % (subpkg, box.cls), box.java('com.frontierworks.%s.%s' % (self.pkg, subpkg), self.modid))
        self.texture('entity/' + box.layer, box.texture(seed or (self.modid + box.layer)))
        return 'com.frontierworks.%s.%s.%s' % (self.pkg, subpkg, box.cls)

    def imp(self, *names):
        for n in names:
            self.imports.add(n)

    def init(self, stmt):
        self.inits.append(stmt)

    def client(self, stmt):
        self.client_inits.append(stmt)

    def tag(self, kind, tag, *values):
        ns, path = tag.split(':', 1)
        lst = self.tags[(kind, ns, path)]
        for v in values:
            if v not in lst:
                lst.append(v)

    def tr(self, key, value):
        self.lang[key] = value

    def ref(self, name):
        return 'Content.' + const(name)

    # ------------------------------------------------------------------ items
    def item(self, name, tex_spec=None, java=None, jtype='Item', tab=None, model='generated', lang=None, desc=None,
             props=None, tags=(), stack=None):
        tab = tab or self.tab
        if java is None:
            p = props or 'new Item.Properties()'
            if stack:
                p += '.stacksTo(%d)' % stack
            java = 'new Item(%s)' % p
        tabref = 'FWTab.%s' % tab if tab in TAB_NAMES else 'null'
        self.field('public static final RegistryObject<%s> %s = REG.item("%s", %s, () -> %s);' % (jtype, const(name), name, tabref, java))
        if model == 'generated' or model == 'handheld':
            self.asset('models/item/%s.json' % name, {'parent': 'minecraft:item/' + model, 'textures': {'layer0': '%s:item/%s' % (self.modid, name)}})
        elif isinstance(model, dict):
            self.asset('models/item/%s.json' % name, model)
        if tex_spec is not None:
            self.texture('item/' + name, tex_spec)
        self.tr('item.%s.%s' % (self.modid, name), lang or title(name))
        if desc:
            self.tr('item.%s.%s.desc' % (self.modid, name), desc)
        for t in tags:
            self.tag('items', t, self.rl(name))
        return self.ref(name)

    # ------------------------------------------------------------------ blocks
    def block(self, name, java=None, jtype='Block', tab=None, item=True, item_java=None, state=None, models=None,
              item_model='block', textures=None, loot='self', mine='pickaxe', level=None, lang=None, desc=None,
              tags=(), item_tags=(), props=None):
        tab = tab or self.tab
        if java is None:
            java = 'new Block(%s)' % (props or 'FWProps.stone(1.5f)')
        tabref = 'FWTab.%s' % tab if tab in TAB_NAMES else 'null'
        C = const(name)
        if item:
            if item_java:
                self.field('public static final RegistryObject<%s> %s = REG.block("%s", %s, () -> %s, %s);' % (jtype, C, name, tabref, java, item_java))
            else:
                self.field('public static final RegistryObject<%s> %s = REG.block("%s", %s, () -> %s);' % (jtype, C, name, tabref, java))
        else:
            self.field('public static final RegistryObject<%s> %s = REG.blockOnly("%s", () -> %s);' % (jtype, C, name, java))
        if state is None:
            state = {'variants': {'': {'model': '%s:block/%s' % (self.modid, name)}}}
        if state is not False:
            self.asset('blockstates/%s.json' % name, state)
        if models is None:
            models = {name: {'parent': 'minecraft:block/cube_all', 'textures': {'all': '%s:block/%s' % (self.modid, name)}}}
        for mname, mjson in models.items():
            self.asset('models/block/%s.json' % mname, mjson)
        if item:
            if item_model == 'block':
                self.asset('models/item/%s.json' % name, {'parent': '%s:block/%s' % (self.modid, name)})
            elif item_model == 'generated':
                self.asset('models/item/%s.json' % name, {'parent': 'minecraft:item/generated', 'textures': {'layer0': '%s:item/%s' % (self.modid, name)}})
            elif isinstance(item_model, str) and item_model.startswith('block:'):
                self.asset('models/item/%s.json' % name, {'parent': '%s:block/%s' % (self.modid, item_model[6:])})
            elif isinstance(item_model, str) and item_model.startswith('blocktex:'):
                self.asset('models/item/%s.json' % name, {'parent': 'minecraft:item/generated', 'textures': {'layer0': '%s:block/%s' % (self.modid, item_model[9:])}})
            elif isinstance(item_model, dict):
                self.asset('models/item/%s.json' % name, item_model)
        if textures:
            for path, img in textures.items():
                self.texture(path, img)
        self.write_loot(name, loot)
        if mine:
            self.tag('blocks', 'minecraft:mineable/' + mine, self.rl(name))
        if level:
            self.tag('blocks', {'stone': 'minecraft:needs_stone_tool', 'iron': 'minecraft:needs_iron_tool',
                                'diamond': 'minecraft:needs_diamond_tool', 'netherite': 'forge:needs_netherite_tool'}[level], self.rl(name))
        self.tr('block.%s.%s' % (self.modid, name), lang or title(name))
        if desc:
            self.tr('block.%s.%s.desc' % (self.modid, name), desc)
        for t in tags:
            self.tag('blocks', t, self.rl(name))
        for t in item_tags:
            self.tag('items', t, self.rl(name))
        return self.ref(name)

    def write_loot(self, name, loot):
        if loot is None or loot == 'none':
            return
        rl = self.rl(name)
        if loot == 'self':
            table = {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0,
                     'entries': [{'type': 'minecraft:item', 'name': rl}],
                     'conditions': [{'condition': 'minecraft:survives_explosion'}]}]}
        elif loot == 'slab':
            table = {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{
                'type': 'minecraft:item', 'name': rl, 'functions': [
                    {'function': 'minecraft:set_count', 'count': 2, 'add': False,
                     'conditions': [{'condition': 'minecraft:block_state_property', 'block': rl, 'properties': {'type': 'double'}}]},
                    {'function': 'minecraft:explosion_decay'}]}]}]}
        elif isinstance(loot, tuple) and loot[0] == 'ore':
            drop = loot[1]
            lo, hi = (loot[2], loot[3]) if len(loot) > 3 else (1, 1)
            fns = []
            if (lo, hi) != (1, 1):
                fns.append({'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}, 'add': False})
            fns.append({'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:ore_drops'})
            fns.append({'function': 'minecraft:explosion_decay'})
            table = {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{
                'type': 'minecraft:alternatives', 'children': [
                    {'type': 'minecraft:item', 'name': rl, 'conditions': [{'condition': 'minecraft:match_tool', 'predicate': {
                        'enchantments': [{'enchantment': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}]},
                    {'type': 'minecraft:item', 'name': mc(drop), 'functions': fns}]}]}]}
        elif isinstance(loot, tuple) and loot[0] == 'item':
            table = {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{
                'type': 'minecraft:item', 'name': mc(loot[1]),
                'functions': [{'function': 'minecraft:set_count', 'count': loot[2] if len(loot) > 2 else 1, 'add': False}]}],
                'conditions': [{'condition': 'minecraft:survives_explosion'}]}]}
        elif isinstance(loot, tuple) and loot[0] == 'leaves':
            sapling = loot[1]
            extra = loot[2] if len(loot) > 2 else None
            silk_or_shears = {'condition': 'minecraft:any_of', 'terms': [
                {'condition': 'minecraft:match_tool', 'predicate': {'items': ['minecraft:shears']}},
                {'condition': 'minecraft:match_tool', 'predicate': {'enchantments': [{'enchantment': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}]}
            pools = [{'rolls': 1, 'bonus_rolls': 0, 'entries': [{'type': 'minecraft:alternatives', 'children': [
                {'type': 'minecraft:item', 'name': rl, 'conditions': [silk_or_shears]},
                {'type': 'minecraft:item', 'name': mc(sapling), 'conditions': [
                    {'condition': 'minecraft:survives_explosion'},
                    {'condition': 'minecraft:table_bonus', 'enchantment': 'minecraft:fortune', 'chances': [0.05, 0.0625, 0.083333336, 0.1]}]}]}]},
                {'rolls': 1, 'bonus_rolls': 0, 'conditions': [{'condition': 'minecraft:inverted', 'term': silk_or_shears}],
                 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:stick', 'functions': [
                     {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 1, 'max': 2}, 'add': False},
                     {'function': 'minecraft:explosion_decay'}],
                     'conditions': [{'condition': 'minecraft:table_bonus', 'enchantment': 'minecraft:fortune', 'chances': [0.02, 0.022222223, 0.025, 0.033333335, 0.1]}]}]}]
            if extra:
                pools.append({'rolls': 1, 'bonus_rolls': 0, 'conditions': [{'condition': 'minecraft:inverted', 'term': silk_or_shears}],
                              'entries': [{'type': 'minecraft:item', 'name': mc(extra), 'conditions': [
                                  {'condition': 'minecraft:table_bonus', 'enchantment': 'minecraft:fortune', 'chances': [0.03, 0.04, 0.05, 0.08, 0.12]}]}]})
            table = {'type': 'minecraft:block', 'pools': pools}
        elif isinstance(loot, dict):
            table = loot
        else:
            raise ValueError('bad loot %r' % (loot,))
        self.data('loot_tables/blocks/%s.json' % name, table)

    # ------------------------------------------------------------------ recipes
    def _conditions(self, obj):
        found = set()

        def walk(o, key=None):
            if isinstance(o, dict):
                for k, v in o.items():
                    walk(v, k)
            elif isinstance(o, list):
                for v in o:
                    walk(v, key)
            elif isinstance(o, str) and key in ('item', 'result', 'type', 'fluid', 'Name', 'name'):
                if ':' in o:
                    ns = o.split(':', 1)[0]
                    if ns.startswith('fw_') and ns != self.modid:
                        found.add(ns)
        walk(obj)
        return [{'type': 'forge:mod_loaded', 'modid': ns} for ns in sorted(found)]

    def recipe(self, name, obj):
        assert name not in self.recipe_names, 'duplicate recipe %s in %s' % (name, self.modid)
        self.recipe_names.add(name)
        conds = self._conditions(obj)
        if conds:
            obj = dict(obj)
            obj['conditions'] = conds
        self.data('recipes/%s.json' % name, obj)

    def shaped(self, name, result, pattern, keys, count=1, cat='misc'):
        key = {k: I(v) for k, v in keys.items()}
        self.recipe(name, {'type': 'minecraft:crafting_shaped', 'category': cat, 'pattern': pattern, 'key': key,
                           'result': {'item': mc(result), 'count': count}})

    def shapeless(self, name, result, ingredients, count=1, cat='misc'):
        self.recipe(name, {'type': 'minecraft:crafting_shapeless', 'category': cat,
                           'ingredients': [I(x) for x in ingredients], 'result': {'item': mc(result), 'count': count}})

    def smelt(self, name, ingredient, result, xp=0.5, time=200, blast=True, smoke=False, cat='misc'):
        self.recipe(name + '_smelting', {'type': 'minecraft:smelting', 'category': cat, 'ingredient': I(ingredient),
                                         'result': mc(result), 'experience': xp, 'cookingtime': time})
        if blast:
            self.recipe(name + '_blasting', {'type': 'minecraft:blasting', 'category': cat, 'ingredient': I(ingredient),
                                             'result': mc(result), 'experience': xp, 'cookingtime': time // 2})
        if smoke:
            self.recipe(name + '_smoking', {'type': 'minecraft:smoking', 'category': 'food', 'ingredient': I(ingredient),
                                            'result': mc(result), 'experience': xp, 'cookingtime': time // 2})
            self.recipe(name + '_campfire', {'type': 'minecraft:campfire_cooking', 'category': 'food', 'ingredient': I(ingredient),
                                             'result': mc(result), 'experience': xp, 'cookingtime': time * 3})

    def mrecipe(self, rtype, name, inputs, outputs, time=200, energy=20, fluid_in=None, fluid_out=None, fluid_in2=None, fluid_out2=None):
        """Machine recipe. inputs: [(ingredient, count)], outputs: [(item, count[, chance])]."""
        ins = []
        for e in inputs:
            ing, cnt = (e, 1) if isinstance(e, str) else e
            d = dict(I(ing)) if not isinstance(ing, (list, tuple)) else {'ingredient': I(ing)}
            d['count'] = cnt
            ins.append(d)
        outs = []
        for o in outputs:
            d = {'item': mc(o[0]), 'count': o[1] if len(o) > 1 else 1}
            if len(o) > 2:
                d['chance'] = o[2]
            outs.append(d)
        obj = {'type': rtype, 'inputs': ins, 'outputs': outs, 'time': time, 'energy': energy}
        if fluid_in:
            obj['fluid_in'] = {'fluid': mc(fluid_in[0]), 'amount': fluid_in[1]}
        if fluid_out:
            obj['fluid_out'] = {'fluid': mc(fluid_out[0]), 'amount': fluid_out[1]}
        if fluid_in2:
            obj['fluid_in2'] = {'fluid': mc(fluid_in2[0]), 'amount': fluid_in2[1]}
        if fluid_out2:
            obj['fluid_out2'] = {'fluid': mc(fluid_out2[0]), 'amount': fluid_out2[1]}
        self.recipe(rtype.split(':', 1)[1] + '/' + name, obj)

    # ------------------------------------------------------------------ worldgen
    def configured(self, name, obj):
        self.data('worldgen/configured_feature/%s.json' % name, obj)

    def placed(self, name, feature, placement):
        self.data('worldgen/placed_feature/%s.json' % name, {'feature': feature, 'placement': placement})

    def add_feature(self, placed_id, biomes='#minecraft:is_overworld', step='underground_ores'):
        self.biome_mods[('features', biomes if isinstance(biomes, str) else json.dumps(biomes), step)].append(placed_id)

    def ore_gen(self, name, stone_block, deep_block=None, size=8, count=8, min_y=-64, max_y=64, shape='trapezoid',
                biomes='#minecraft:is_overworld', discard=0.0, extra_targets=None, rarity=None):
        targets = []
        if stone_block:
            targets.append({'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:stone_ore_replaceables'},
                            'state': {'Name': stone_block}})
        if deep_block:
            targets.append({'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:deepslate_ore_replaceables'},
                            'state': {'Name': deep_block}})
        for t in (extra_targets or []):
            targets.append(t)
        self.configured(name, {'type': 'minecraft:ore', 'config': {'size': size, 'discard_chance_on_air_exposure': discard, 'targets': targets}})
        placement = []
        if rarity:
            placement.append({'type': 'minecraft:rarity_filter', 'chance': rarity})
        else:
            placement.append({'type': 'minecraft:count', 'count': count})
        placement.append({'type': 'minecraft:in_square'})
        placement.append({'type': 'minecraft:height_range', 'height': {'type': 'minecraft:' + shape,
                          'min_inclusive': {'absolute': min_y}, 'max_inclusive': {'absolute': max_y}}})
        placement.append({'type': 'minecraft:biome'})
        self.placed(name, self.rl(name), placement)
        self.add_feature(self.rl(name), biomes, 'underground_ores')

    def add_spawn(self, entity, biomes, weight, lo, hi):
        self.biome_mods[('spawns', biomes if isinstance(biomes, str) else json.dumps(biomes), '')].append(
            {'type': entity, 'weight': weight, 'minCount': lo, 'maxCount': hi})

    # ------------------------------------------------------------------ loot injection
    def inject_loot(self, name, tables, table_obj):
        """Adds the loot table data/<mod>/loot_tables/inject/<name>.json to the given vanilla/other tables."""
        self.data('loot_tables/inject/%s.json' % name, table_obj)
        terms = [{'condition': 'forge:loot_table_id', 'loot_table_id': mc(t)} for t in tables]
        cond = terms[0] if len(terms) == 1 else {'condition': 'minecraft:any_of', 'terms': terms}
        self.data('loot_modifiers/%s.json' % name, {'type': 'fw_core:add_table', 'conditions': [cond], 'table': self.rl('inject/' + name)})
        self.loot_mods.append(self.rl(name))

    # ------------------------------------------------------------------ output
    def finalize(self):
        # lang
        self.asset('lang/en_us.json', dict(self.lang))
        # tags
        for (kind, ns, path), vals in self.tags.items():
            self.put('data/%s/tags/%s/%s.json' % (ns, kind, path), {'replace': False, 'values': vals})
        # biome modifiers
        for i, ((kind, biomes, step), entries) in enumerate(sorted(self.biome_mods.items(), key=lambda kv: str(kv[0]))):
            b = json.loads(biomes) if biomes.startswith('[') or biomes.startswith('{') else biomes
            if kind == 'features':
                obj = {'type': 'forge:add_features', 'biomes': b, 'features': entries, 'step': step}
            else:
                obj = {'type': 'forge:add_spawns', 'biomes': b, 'spawners': entries}
            self.data('forge/biome_modifier/%s_%d.json' % (kind, i), obj)
        if self.loot_mods:
            self.put('data/forge/loot_modifiers/global_loot_modifiers.json', {'replace': False, 'entries': self.loot_mods})
        if self.selftest['structures']:
            self.data('fw_selftest/tests.json', self.selftest)

    def mods_toml_entry(self):
        lines = ['[[mods]]', 'modId="%s"' % self.modid, 'version="%s"' % PACK_VERSION,
                 'displayName="Frontierworks: %s"' % self.name, 'authors="%s"' % AUTHORS,
                 "description='''%s'''" % self.desc.replace("'''", "'"), '']
        deps = [('forge', '[47,)', 'NONE'), ('minecraft', '[1.20.1,1.20.2)', 'NONE')]
        if self.modid != 'fw_core':
            deps.append(('fw_core', '[%s,)' % PACK_VERSION, 'AFTER'))
        for d in self.deps:
            deps.append((d, '[%s,)' % PACK_VERSION, 'AFTER'))
        for (mid, rng, order) in deps:
            lines += ['[[dependencies.%s]]' % self.modid, '    modId="%s"' % mid, '    mandatory=true',
                      '    versionRange="%s"' % rng, '    ordering="%s"' % order, '    side="BOTH"', '']
        return '\n'.join(lines)

    def java_content(self):
        imports = [
            'com.frontierworks.core.block.FWBlocks', 'com.frontierworks.core.energy.*', 'com.frontierworks.core.fluid.FWFluid',
            'com.frontierworks.core.item.*', 'com.frontierworks.core.machine.*', 'com.frontierworks.core.recipe.MachineRecipeKind',
            'com.frontierworks.core.registry.FWProps', 'com.frontierworks.core.registry.FWRegistry', 'com.frontierworks.core.registry.FWTab',
            'net.minecraft.resources.ResourceLocation', 'net.minecraft.util.valueproviders.UniformInt',
            'net.minecraft.world.entity.EntityType', 'net.minecraft.world.entity.MobCategory', 'net.minecraft.world.item.*',
            'net.minecraft.world.item.crafting.RecipeType', 'net.minecraft.world.level.block.*',
            'net.minecraft.world.level.block.entity.BlockEntityType', 'net.minecraft.world.level.block.state.BlockBehaviour',
            'net.minecraft.world.level.block.state.properties.BlockSetType', 'net.minecraft.world.level.block.state.properties.WoodType',
            'net.minecraft.world.level.material.MapColor', 'net.minecraftforge.registries.RegistryObject',
        ]
        imports += sorted(self.imports)
        out = ['// GENERATED by tools/build.py from tools/mods/%s.py - do not edit' % self.pkg,
               'package com.frontierworks.%s;' % self.pkg, '']
        for i in imports:
            out.append('import %s;' % i)
        out += ['', '@SuppressWarnings({"unused", "Convert2MethodRef"})', 'public final class Content {',
                '    private Content() {}', '',
                '    public static final FWRegistry REG = new FWRegistry(%sMod.MODID);' % self.pascal, '']
        for f in self.fields:
            out.append('    ' + f)
        out += ['', '    public static void init() {}', '}', '']
        return '\n'.join(out)

    def java_main(self):
        out = ['// GENERATED by tools/build.py - do not edit', 'package com.frontierworks.%s;' % self.pkg, '',
               'import net.minecraftforge.api.distmarker.Dist;', 'import net.minecraftforge.eventbus.api.IEventBus;',
               'import net.minecraftforge.fml.DistExecutor;', 'import net.minecraftforge.fml.common.Mod;',
               'import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;', '',
               '@Mod(%sMod.MODID)' % self.pascal, 'public final class %sMod {' % self.pascal,
               '    public static final String MODID = "%s";' % self.modid, '',
               '    @SuppressWarnings("removal")',
               '    public %sMod() {' % self.pascal,
               '        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();',
               '        Content.REG.register(bus);']
        for s in self.inits:
            out.append('        %s;' % s.rstrip(';'))
        if self.client_inits:
            out.append('        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {')
            for s in self.client_inits:
                out.append('            %s;' % s.rstrip(';'))
            out.append('        });')
        out += ['    }', '}', '']
        return '\n'.join(out)


# ============================================================================ shape helpers

def model_cube(m, name, texname=None):
    return {name: {'parent': 'minecraft:block/cube_all', 'textures': {'all': '%s:block/%s' % (m.modid, texname or name)}}}


def cube(m, name, texture, java=None, **kw):
    return m.block(name, java=java, textures={'block/' + name: texture}, **kw)


def column(m, name, side_tex, end_tex, java=None, side_name=None, end_name=None, **kw):
    sn, en = side_name or name + '_side', end_name or name + '_end'
    models = {name: {'parent': 'minecraft:block/cube_column', 'textures': {'side': '%s:block/%s' % (m.modid, sn), 'end': '%s:block/%s' % (m.modid, en)}},
              name + '_horizontal': {'parent': 'minecraft:block/cube_column_horizontal', 'textures': {'side': '%s:block/%s' % (m.modid, sn), 'end': '%s:block/%s' % (m.modid, en)}}}
    state = {'variants': {
        'axis=y': {'model': '%s:block/%s' % (m.modid, name)},
        'axis=z': {'model': '%s:block/%s_horizontal' % (m.modid, name), 'x': 90},
        'axis=x': {'model': '%s:block/%s_horizontal' % (m.modid, name), 'x': 90, 'y': 90}}}
    textures = {}
    if side_tex is not None:
        textures['block/' + sn] = side_tex
    if end_tex is not None:
        textures['block/' + en] = end_tex
    return m.block(name, java=java or 'new RotatedPillarBlock(%s)' % kw.pop('props', 'FWProps.stone(1.5f)'),
                   state=state, models=models, textures=textures, **kw)


ORE_BASES = {
    'stone': 'minecraft:block/stone', 'deepslate': 'minecraft:block/deepslate', 'netherrack': 'minecraft:block/netherrack',
    'end_stone': 'minecraft:block/end_stone', 'sand': 'minecraft:block/sand', 'red_sand': 'minecraft:block/red_sand',
    'gravel': 'minecraft:block/gravel', 'blackstone': 'minecraft:block/blackstone', 'tuff': 'minecraft:block/tuff',
}


def overlay_model(base_tex, overlay_tex):
    faces = lambda t: {d: {'texture': t, 'cullface': d} for d in ('down', 'up', 'north', 'south', 'west', 'east')}
    return {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
            'textures': {'particle': base_tex, 'base': base_tex, 'overlay': overlay_tex},
            'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': faces('#base')},
                         {'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': faces('#overlay')}]}


def ore(m, name, color, base='stone', drop=None, xp=None, hardness=3.0, style='blobs', level='stone', counts=None,
        overlay_name=None, base_tex=None, tab='MATERIALS', **kw):
    """Ore block rendered as vanilla base texture + our transparent overlay."""
    ov = overlay_name or name.replace('deepslate_', '').replace('nether_', '').replace('end_', '') + '_overlay'
    base_ref = base_tex or ORE_BASES[base]
    if base == 'deepslate':
        props = 'FWProps.deepslateOre(%sf)' % hardness
    else:
        props = 'FWProps.ore(%sf)' % hardness
    if xp:
        java = 'new DropExperienceBlock(%s, UniformInt.of(%d, %d))' % (props, xp[0], xp[1])
    else:
        java = 'new DropExperienceBlock(%s)' % props
    loot = ('ore', drop) + tuple(counts) if counts else (('ore', drop) if drop else 'self')
    tx = {'block/' + ov: tex.ore_overlay(color, m.modid + ov, style)}
    return m.block(name, java=java, models={name: overlay_model(base_ref, '%s:block/%s' % (m.modid, ov))}, textures=tx,
                   loot=loot, level=level, tab=tab, **kw)


def horizontal_state(m, name, model_off, model_on=None, lit_prop=True):
    v = {}
    rot = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    for f, r in rot.items():
        if lit_prop:
            v['facing=%s,lit=false' % f] = {'model': '%s:block/%s' % (m.modid, model_off), 'y': r} if r else {'model': '%s:block/%s' % (m.modid, model_off)}
            v['facing=%s,lit=true' % f] = {'model': '%s:block/%s' % (m.modid, model_on or model_off), 'y': r} if r else {'model': '%s:block/%s' % (m.modid, model_on or model_off)}
        else:
            v['facing=%s' % f] = {'model': '%s:block/%s' % (m.modid, model_off), 'y': r} if r else {'model': '%s:block/%s' % (m.modid, model_off)}
    return {'variants': v}


def machine_models(m, name, tier, glyph, share_casing=True):
    """Orientable machine models + textures; casing textures are shared per tier inside a mod."""
    t = tex.machine_textures(tier, glyph, m.modid + tier)
    side, top, bottom = 'casing_%s_side' % tier, 'casing_%s_top' % tier, 'casing_%s_bottom' % tier
    textures = {'block/' + side: t['side'], 'block/' + top: t['top'], 'block/' + bottom: t['bottom'],
                'block/%s_front' % name: t['front'], 'block/%s_front_on' % name: t['front_on']}
    mk = lambda front: {'parent': 'minecraft:block/orientable_with_bottom', 'textures': {
        'top': '%s:block/%s' % (m.modid, top), 'bottom': '%s:block/%s' % (m.modid, bottom),
        'side': '%s:block/%s' % (m.modid, side), 'front': '%s:block/%s' % (m.modid, front)}}
    models = {name: mk(name + '_front'), name + '_on': mk(name + '_front_on')}
    return models, textures


def machine(m, name, be_expr, tier='basic', glyph='gear', tab='MACHINES', desc=None, lang=None, block_class='MachineBlock',
            be_class='ProcessingMachineBE', props='FWProps.machine()', extra_be_blocks=()):
    """A MachineBlock + block entity type. be_expr is a Java expression using (pos, state) and BE_TYPE."""
    C = const(name)
    models, textures = machine_models(m, name, tier, glyph)
    m.block(name, java='new %s(%s, () -> Content.%s_BE.get())' % (block_class, props, C), jtype=block_class, tab=tab,
            state=horizontal_state(m, name, name, name + '_on'), models=models, textures=textures, mine='pickaxe',
            level='stone', desc=desc, lang=lang)
    be = be_expr.replace('BE_TYPE', 'Content.%s_BE.get()' % C)
    m.field('public static final RegistryObject<BlockEntityType<%s>> %s_BE = REG.blockEntity("%s", (pos, state) -> %s, %s);'
            % (be_class, C, name, be, ', '.join(['Content.' + C] + list(extra_be_blocks))))
    return 'Content.' + C


def processing_machine(m, name, kind_ref, inputs=1, outputs=1, tier='basic', glyph='gear', capacity=20000, max_in=200,
                       tanks_in=0, tanks_out=0, tank_cap=8000, desc=None, lang=None, cooking=None, energy_factor=1.0,
                       time_factor=1.0):
    """Generic processing machine using a MachineRecipeKind field (kind_ref) or vanilla cooking recipes."""
    C = const(name)
    if cooking:
        src = 'RecipeSource.cooking(() -> RecipeType.%s, %sf, 20)' % (cooking, time_factor)
    else:
        src = 'RecipeSource.machine(() -> %s)' % kind_ref
    layout = 'MachineLayout.processing(%d, %d, %d, %d, %d, true, 3, true)' % (inputs, outputs, tanks_in, tanks_out, tank_cap)
    m.field('public static final MachineSpec %s_SPEC = new MachineSpec(%s, %s, %d, %d, %sf, %sf);'
            % (C, layout, src, capacity, max_in, energy_factor, 1.0 if cooking else time_factor))
    return machine(m, name, 'new ProcessingMachineBE(BE_TYPE, pos, state, Content.%s_SPEC)' % C, tier, glyph, desc=desc, lang=lang)


def stairs_slab_wall(m, base_name, tex_ref, tab='DECOR', wall=True, props='FWProps.stone(1.5f)', mine='pickaxe', level=None):
    """Stairs, slab and (optionally) wall for an existing block, using vanilla parent models."""
    T = tex_ref
    base = 'Content.' + const(base_name)
    stairs, slab, wall_n = base_name + '_stairs', base_name + '_slab', base_name + '_wall'
    if base_name.endswith('bricks'):
        stairs, slab, wall_n = base_name[:-1] + '_stairs', base_name[:-1] + '_slab', base_name[:-1] + '_wall'
    elif base_name.endswith('planks'):
        stairs, slab = base_name[:-7] + '_stairs', base_name[:-7] + '_slab'
    mid = m.modid
    # stairs
    sm = {stairs: {'parent': 'minecraft:block/stairs', 'textures': {'bottom': T, 'top': T, 'side': T}},
          stairs + '_inner': {'parent': 'minecraft:block/inner_stairs', 'textures': {'bottom': T, 'top': T, 'side': T}},
          stairs + '_outer': {'parent': 'minecraft:block/outer_stairs', 'textures': {'bottom': T, 'top': T, 'side': T}}}
    m.block(stairs, java='new StairBlock(() -> %s.get().defaultBlockState(), %s)' % (base, props), jtype='StairBlock',
            tab=tab, state=stairs_state(mid, stairs), models=sm, mine=mine, level=level)
    m.block(slab, java='new SlabBlock(%s)' % props, jtype='SlabBlock', tab=tab, state={'variants': {
        'type=bottom': {'model': '%s:block/%s' % (mid, slab)}, 'type=top': {'model': '%s:block/%s_top' % (mid, slab)},
        'type=double': {'model': '%s:block/%s_double' % (mid, slab)}}},
            models={slab: {'parent': 'minecraft:block/slab', 'textures': {'bottom': T, 'top': T, 'side': T}},
                    slab + '_top': {'parent': 'minecraft:block/slab_top', 'textures': {'bottom': T, 'top': T, 'side': T}},
                    slab + '_double': {'parent': 'minecraft:block/cube_all', 'textures': {'all': T}}},
            loot='slab', mine=mine, level=level)
    base_item = m.rl(base_name)
    m.shaped(stairs, m.rl(stairs), ['#  ', '## ', '###'], {'#': base_item}, 4)
    m.shaped(slab, m.rl(slab), ['###'], {'#': base_item}, 6)
    if wall:
        m.block(wall_n, java='new WallBlock(%s)' % props, jtype='WallBlock', tab=tab, state=wall_state(mid, wall_n),
                models={wall_n + '_post': {'parent': 'minecraft:block/template_wall_post', 'textures': {'wall': T}},
                        wall_n + '_side': {'parent': 'minecraft:block/template_wall_side', 'textures': {'wall': T}},
                        wall_n + '_side_tall': {'parent': 'minecraft:block/template_wall_side_tall', 'textures': {'wall': T}},
                        wall_n + '_inventory': {'parent': 'minecraft:block/wall_inventory', 'textures': {'wall': T}}},
                item_model='block:' + wall_n + '_inventory', mine=mine, level=level, tags=['minecraft:walls'], item_tags=['minecraft:walls'])
        m.shaped(wall_n, m.rl(wall_n), ['###', '###'], {'#': base_item}, 6)
    m.tag('blocks', 'minecraft:stairs', m.rl(stairs))
    m.tag('items', 'minecraft:stairs', m.rl(stairs))
    m.tag('blocks', 'minecraft:slabs', m.rl(slab))
    m.tag('items', 'minecraft:slabs', m.rl(slab))


def stairs_state(mid, n):
    v = {}
    for facing, base_y in (('east', 0), ('north', 270), ('south', 90), ('west', 180)):
        for half in ('bottom', 'top'):
            for shape in ('straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right'):
                model = n if shape == 'straight' else (n + '_inner' if shape.startswith('inner') else n + '_outer')
                y = base_y
                if shape in ('inner_left', 'outer_left'):
                    y = (base_y + 270) % 360
                x = 0
                if half == 'top':
                    x = 180
                    if shape in ('inner_left', 'outer_left'):
                        y = base_y
                    elif shape in ('inner_right', 'outer_right'):
                        y = (base_y + 90) % 360
                e = {'model': '%s:block/%s' % (mid, model)}
                if x:
                    e['x'] = x
                if y:
                    e['y'] = y
                if x or y:
                    e['uvlock'] = True
                v['facing=%s,half=%s,shape=%s' % (facing, half, shape)] = e
    return {'variants': v}


def wall_state(mid, n):
    parts = [{'when': {'up': 'true'}, 'apply': {'model': '%s:block/%s_post' % (mid, n)}}]
    for d, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        for h, suffix in (('low', '_side'), ('tall', '_side_tall')):
            a = {'model': '%s:block/%s%s' % (mid, n, suffix), 'uvlock': True}
            if y:
                a['y'] = y
            parts.append({'when': {d: h}, 'apply': a})
    return {'multipart': parts}


def cross_block(m, name, texture, java, tab='WORLD', loot='self', item_model='generated_block', mine=None, **kw):
    models = {name: {'parent': 'minecraft:block/cross', 'render_type': 'minecraft:cutout', 'textures': {'cross': '%s:block/%s' % (m.modid, name)}}}
    im = 'blocktex:' + name if item_model == 'generated_block' else item_model
    return m.block(name, java=java, tab=tab, models=models, item_model=im, textures={'block/' + name: texture}, loot=loot, mine=mine, **kw)


def wood_set(m, prefix, bark, inner, plank_color, leaf_color, tree_feature, tab='WORLD', fruit=None, leaf_extra_drop=None):
    """log, stripped log, wood, planks, stairs, slab, leaves, sapling (+ recipes, tags)."""
    mid = m.modid
    log, slog, wood, planks_n, leaves_n, sap = prefix + '_log', 'stripped_' + prefix + '_log', prefix + '_wood', prefix + '_planks', prefix + '_leaves', prefix + '_sapling'
    column(m, slog, tex.log_side(inner, mid + slog), tex.log_top(inner, inner, mid + slog + 't'), java='new FWBlocks.LogBlock(FWProps.wood(), null)',
           jtype='FWBlocks.LogBlock', tab=tab, mine='axe', side_name=slog, end_name=slog + '_top')
    column(m, log, tex.log_side(bark, mid + log), tex.log_top(bark, inner, mid + log + 't'),
           java='new FWBlocks.LogBlock(FWProps.wood(), () -> Content.%s.get())' % const(slog), jtype='FWBlocks.LogBlock', tab=tab, mine='axe',
           side_name=log, end_name=log + '_top')
    column(m, wood, None, None, java='new FWBlocks.LogBlock(FWProps.wood(), null)', jtype='FWBlocks.LogBlock', tab=tab, mine='axe',
           side_name=log, end_name=log)
    m.block(planks_n, java='new FWBlocks.FlammableBlock(FWProps.wood(), 20, 5)', jtype='FWBlocks.FlammableBlock', tab=tab, mine='axe',
            textures={'block/' + planks_n: tex.planks(plank_color, mid + planks_n)}, tags=['minecraft:planks'], item_tags=['minecraft:planks'])
    stairs_slab_wall(m, planks_n, '%s:block/%s' % (mid, planks_n), tab=tab, wall=False, props='FWProps.wood()', mine='axe')
    m.block(leaves_n, java='new FWBlocks.Leaves(FWProps.leaves())', jtype='FWBlocks.Leaves', tab=tab, mine='hoe',
            models={leaves_n: {'parent': 'minecraft:block/leaves', 'render_type': 'minecraft:cutout_mipped', 'textures': {'all': '%s:block/%s' % (mid, leaves_n)}}},
            textures={'block/' + leaves_n: tex.leaves(leaf_color, mid + leaves_n, fruit)}, loot=('leaves', m.rl(sap), leaf_extra_drop),
            tags=['minecraft:leaves'], item_tags=['minecraft:leaves'])
    cross_block(m, sap, tex.cross_plant(leaf_color, leaf_color, mid + sap, 'sapling'),
                'new FWBlocks.Sapling(new ResourceLocation("%s"), FWProps.sapling())' % tree_feature, tab=tab, jtype='FWBlocks.Sapling',
                tags=['minecraft:saplings'], item_tags=['minecraft:saplings'])
    for t in ('minecraft:logs', 'minecraft:logs_that_burn'):
        for n in (log, slog, wood):
            m.tag('blocks', t, m.rl(n))
            m.tag('items', t, m.rl(n))
    m.shapeless(planks_n, m.rl(planks_n), [m.rl(log)], 4)
    m.shapeless(planks_n + '_from_wood', m.rl(planks_n), [m.rl(wood)], 4)
    m.shaped(wood, m.rl(wood), ['##', '##'], {'#': m.rl(log)}, 3)
    # flower pot not included; tree feature JSON is written by caller (needs leaf/log names)
    return log, leaves_n, sap


def tree_feature(m, name, log, leaves, height=5, rand_a=2, rand_b=0, radius=2, foliage='blob', trunk='straight', decorators=None):
    fp = {'blob': {'type': 'minecraft:blob_foliage_placer', 'radius': radius, 'offset': 0, 'height': 3},
          'fancy': {'type': 'minecraft:fancy_foliage_placer', 'radius': radius, 'offset': 4, 'height': 4},
          'spruce': {'type': 'minecraft:spruce_foliage_placer', 'radius': {'type': 'minecraft:uniform', 'value': {'min_inclusive': 2, 'max_inclusive': 3}},
                     'offset': {'type': 'minecraft:uniform', 'value': {'min_inclusive': 0, 'max_inclusive': 2}},
                     'trunk_height': {'type': 'minecraft:uniform', 'value': {'min_inclusive': 1, 'max_inclusive': 2}}},
          'acacia': {'type': 'minecraft:acacia_foliage_placer', 'radius': radius, 'offset': 0},
          'cherry': {'type': 'minecraft:cherry_foliage_placer', 'radius': 4, 'offset': 0, 'height': 5, 'wide_bottom_layer_hole_chance': 0.25,
                     'corner_hole_chance': 0.25, 'hanging_leaves_chance': 0.16666667, 'hanging_leaves_extension_chance': 0.33333334}}[foliage]
    tp = {'straight': 'minecraft:straight_trunk_placer', 'fancy': 'minecraft:fancy_trunk_placer', 'forking': 'minecraft:forking_trunk_placer',
          'giant': 'minecraft:giant_trunk_placer'}[trunk]
    m.configured(name, {'type': 'minecraft:tree', 'config': {
        'ignore_vines': True, 'force_dirt': False,
        'minimum_size': {'type': 'minecraft:two_layers_feature_size', 'limit': 1, 'lower_size': 0, 'upper_size': 1},
        'dirt_provider': {'type': 'minecraft:simple_state_provider', 'state': {'Name': 'minecraft:dirt'}},
        'trunk_provider': {'type': 'minecraft:simple_state_provider', 'state': {'Name': log, 'Properties': {'axis': 'y'}}},
        'foliage_provider': {'type': 'minecraft:simple_state_provider', 'state': {'Name': leaves, 'Properties': {'distance': '7', 'persistent': 'false', 'waterlogged': 'false'}}},
        'trunk_placer': {'type': tp, 'base_height': height, 'height_rand_a': rand_a, 'height_rand_b': rand_b},
        'foliage_placer': fp, 'decorators': decorators or []}})


def tree_placement(m, name, feature, sapling, chance=8, biomes='#minecraft:is_forest'):
    m.placed(name, feature, [
        {'type': 'minecraft:rarity_filter', 'chance': chance}, {'type': 'minecraft:in_square'},
        {'type': 'minecraft:surface_water_depth_filter', 'max_water_depth': 0},
        {'type': 'minecraft:heightmap', 'heightmap': 'OCEAN_FLOOR'}, {'type': 'minecraft:biome'},
        {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:would_survive', 'state': {'Name': sapling, 'Properties': {'stage': '0'}}}}])
    m.add_feature(m.rl(name), biomes, 'vegetal_decoration')


def fluid(m, name, color, lang=None, gas=False, block=True, bucket=True, temperature=300, density=1000, viscosity=1000, tab='MATERIALS'):
    """Registers a tinted FWFluid (type, source, flowing, bucket, block) with models, textures and lang."""
    C = const(name)
    argb = '0xFF' + color.lstrip('#').upper()
    props = 'FWFluid.gas(%d)' % temperature if gas else 'FWFluid.liquid(%d, %d, %d)' % (density, viscosity, temperature)
    tabref = 'FWTab.%s' % tab
    m.field('public static final FWFluid %s = new FWFluid(REG, "%s", %s, %s, %s, %s, %s);'
            % (C, name, argb, props, 'true' if (block and not gas) else 'false', 'true' if bucket else 'false', tabref))
    nice = lang or title(name)
    m.tr('fluid_type.%s.%s' % (m.modid, name), nice)
    if block and not gas:
        m.asset('blockstates/%s.json' % name, {'variants': {'': {'model': '%s:block/%s' % (m.modid, name)}}})
        m.asset('models/block/%s.json' % name, {'textures': {'particle': 'minecraft:block/water_still'}})
        m.tr('block.%s.%s' % (m.modid, name), nice)
    if bucket:
        bn = name + '_bucket'
        m.asset('models/item/%s.json' % bn, {'parent': 'minecraft:item/generated', 'textures': {'layer0': '%s:item/%s' % (m.modid, bn)}})
        m.texture('item/' + bn, tex.item('bucket', '#9aa0a8', None, color))
        m.tr('item.%s.%s' % (m.modid, bn), nice + ' Bucket')
    m.tag('fluids', 'forge:' + name, m.rl(name), m.rl('flowing_' + name))
    return 'Content.' + C


# ============================================================================ writing

def write_file(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    if isinstance(content, Image.Image):
        content.save(str(path), optimize=True)
    elif isinstance(content, (dict, list)):
        path.write_text(json.dumps(content, indent=2) + '\n')
    elif isinstance(content, bytes):
        path.write_bytes(content)
    else:
        path.write_text(content)


def build_all():
    mods_dir = REPO / 'mods'
    merged = REPO / 'generated' / 'merged'
    if merged.exists():
        shutil.rmtree(merged)
    merged_json = {}
    merged_lang = defaultdict(dict)
    toml_entries = []
    for m in ALL_MODS:
        m.finalize()
        mdir = mods_dir / m.modid
        for sub in ('gen', 'resources'):
            if (mdir / sub).exists():
                shutil.rmtree(mdir / sub)
        jdir = mdir / 'gen' / 'com' / 'frontierworks' / m.pkg
        write_file(jdir / 'Content.java', m.java_content())
        write_file(jdir / ('%sMod.java' % m.pascal), m.java_main())
        for rel, src in m.java_files.items():
            write_file(jdir / rel, src)
        res = mdir / 'resources'
        toml = 'modLoader="javafml"\nloaderVersion="[47,)"\nlicense="MIT"\n\n' + m.mods_toml_entry()
        write_file(res / 'META-INF' / 'mods.toml', toml)
        write_file(res / 'pack.mcmeta', {'pack': {'description': 'Frontierworks: %s resources' % m.name, 'pack_format': 15,
                                                   'forge:resource_pack_format': 15, 'forge:data_pack_format': 15}})
        toml_entries.append(m.mods_toml_entry())
        for rel, content in m.files.items():
            write_file(res / rel, content)
        static = mdir / 'static'
        if static.exists():
            for p in static.rglob('*'):
                if p.is_file():
                    dest = res / p.relative_to(static)
                    dest.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copy2(p, dest)
        # merge into dev resources
        for p in res.rglob('*'):
            if not p.is_file():
                continue
            rel = p.relative_to(res).as_posix()
            if rel in ('META-INF/mods.toml', 'pack.mcmeta'):
                continue
            if rel.endswith('lang/en_us.json'):
                merged_lang[rel].update(json.loads(p.read_text()))
                continue
            dest = merged / rel
            if rel.endswith('.json') and dest.exists():
                a = json.loads(dest.read_text())
                b = json.loads(p.read_text())
                if 'values' in a and 'values' in b:
                    a['values'] = list(OrderedDict.fromkeys(a['values'] + b['values']))
                    write_file(dest, a)
                    continue
                if 'entries' in a and 'entries' in b:
                    a['entries'] = list(OrderedDict.fromkeys(a['entries'] + b['entries']))
                    write_file(dest, a)
                    continue
                raise RuntimeError('conflicting resource %s (from %s)' % (rel, m.modid))
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(p, dest)
    for rel, d in merged_lang.items():
        write_file(merged / rel, d)
    write_file(merged / 'META-INF' / 'mods.toml', 'modLoader="javafml"\nloaderVersion="[47,)"\nlicense="MIT"\n\n' + '\n'.join(toml_entries))
    write_file(merged / 'pack.mcmeta', {'pack': {'description': 'Frontierworks (dev, merged)', 'pack_format': 15,
                                                  'forge:resource_pack_format': 15, 'forge:data_pack_format': 15}})
    (REPO / 'mods.txt').write_text('# generated by tools/build.py - mod ids in load order\n' + '\n'.join(m.modid for m in ALL_MODS) + '\n')
    return ALL_MODS


def recipe_kind(m, name):
    """Registers a MachineRecipeKind field named after the recipe type and returns its Java reference."""
    m.field('public static final MachineRecipeKind %s = REG.machineRecipe("%s");' % (const(name), name))
    return 'Content.' + const(name)


def fuel_machine(m, name, kind_ref, inputs=1, outputs=1, tier='steam', glyph='flame', desc=None, lang=None, time_factor=1.0):
    """Furnace-style machine that burns fuel instead of Forge Energy."""
    C = const(name)
    m.field('public static final MachineSpec %s_SPEC = new MachineSpec(MachineLayout.fuel(%d, %d), RecipeSource.machine(() -> %s), 0, 0, 1.0f, %sf);'
            % (C, inputs, outputs, kind_ref, time_factor))
    return machine(m, name, 'new FuelProcessingBE(BE_TYPE, pos, state, Content.%s_SPEC)' % C, tier, glyph, desc=desc, lang=lang,
                   be_class='FuelProcessingBE', tab='MACHINES')


def custom_machine(m, name, be_class, tier='basic', glyph='gear', desc=None, lang=None, tab='MACHINES', block_class='MachineBlock',
                   ctor_args=''):
    """Machine with a hand-written block entity class taking (type, pos, state[, args])."""
    extra = (', ' + ctor_args) if ctor_args else ''
    return machine(m, name, 'new %s(BE_TYPE, pos, state%s)' % (be_class, extra), tier, glyph, desc=desc, lang=lang,
                   be_class=be_class, tab=tab, block_class=block_class)


def custom_model_machine(m, name, be_expr, be_class, textures, top=None, side=None, bottom=None, front=None, front_on=None,
                         tab='MACHINES', desc=None, lang=None, block_class='MachineBlock', props='FWProps.machine()', level='stone'):
    """MachineBlock whose model uses explicit texture names (e.g. solar panel top). textures: dict path->image."""
    C = const(name)
    t = lambda n: '%s:block/%s' % (m.modid, n)
    def mk(fr):
        return {'parent': 'minecraft:block/orientable_with_bottom', 'textures': {'top': t(top), 'bottom': t(bottom or top), 'side': t(side), 'front': t(fr or side)}}
    models = {name: mk(front), name + '_on': mk(front_on or front)}
    m.block(name, java='new %s(%s, () -> Content.%s_BE.get())' % (block_class, props, C), jtype=block_class, tab=tab,
            state=horizontal_state(m, name, name, name + '_on'), models=models, textures=textures, mine='pickaxe', level=level, desc=desc, lang=lang)
    be = be_expr.replace('BE_TYPE', 'Content.%s_BE.get()' % C)
    m.field('public static final RegistryObject<BlockEntityType<%s>> %s_BE = REG.blockEntity("%s", (pos, state) -> %s, Content.%s);' % (be_class, C, name, be, C))
    return 'Content.' + C


CONN_ROT = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}


def connector_block(m, name, java, jtype, radius, body_img, end_img, tab='LOGISTICS', desc=None, lang=None, mine='pickaxe', render='cutout'):
    """Six-way connecting block (pipes, ducts): multipart core + rotated arm models, inventory model as a straight piece."""
    a, b = 8 - radius, 8 + radius
    t = lambda n: '%s:block/%s' % (m.modid, n)
    tx = {'particle': t(name), 'body': t(name), 'end': t(name + '_end')}
    core = {'parent': 'minecraft:block/block', 'render_type': 'minecraft:' + render, 'textures': tx,
            'elements': [{'from': [a, a, a], 'to': [b, b, b], 'faces': {d: {'texture': '#end'} for d in ('north', 'south', 'east', 'west', 'up', 'down')}}]}
    side = {'parent': 'minecraft:block/block', 'render_type': 'minecraft:' + render, 'textures': tx,
            'elements': [{'from': [a, a, 0], 'to': [b, b, a], 'faces': {
                'north': {'texture': '#end', 'cullface': 'north'}, 'east': {'texture': '#body', 'uv': [0, a, a, b]},
                'west': {'texture': '#body', 'uv': [0, a, a, b]}, 'up': {'texture': '#body', 'uv': [a, 0, b, a], 'rotation': 90},
                'down': {'texture': '#body', 'uv': [a, 0, b, a], 'rotation': 90}}}]}
    inv = {'parent': 'minecraft:block/block', 'render_type': 'minecraft:' + render, 'textures': tx,
           'display': {'gui': {'rotation': [30, 225, 0], 'scale': [0.9, 0.9, 0.9]}},
           'elements': [{'from': [a, a, 0], 'to': [b, b, 16], 'faces': {
               'north': {'texture': '#end'}, 'south': {'texture': '#end'}, 'east': {'texture': '#body', 'uv': [0, a, 16, b]},
               'west': {'texture': '#body', 'uv': [0, a, 16, b]}, 'up': {'texture': '#body', 'uv': [0, a, 16, b], 'rotation': 90},
               'down': {'texture': '#body', 'uv': [0, a, 16, b], 'rotation': 90}}}]}
    parts = [{'apply': {'model': t(name + '_core')}}]
    for d, r in CONN_ROT.items():
        parts.append({'when': {d: 'true'}, 'apply': dict({'model': t(name + '_side')}, **r)})
    return m.block(name, java=java, jtype=jtype, tab=tab, state={'multipart': parts},
                   models={name + '_core': core, name + '_side': side, name + '_inventory': inv}, item_model='block:' + name + '_inventory',
                   textures={'block/' + name: body_img, 'block/%s_end' % name: end_img}, mine=mine, level=None, desc=desc, lang=lang)


def pipe_tex(body, band, seed, glass=False):
    img = tex.noise_fill(body, seed, 0.06)
    px = img.load()
    c = tex.rgb(band)
    for x in range(16):
        for y in (0, 15):
            px[x, y] = tex.shade(c, 0.7)
    for x in range(0, 16, 5):
        for y in range(16):
            px[x, y] = tex.shade(c, 1.0)
    if glass:
        for y in range(3, 13):
            for x in range(16):
                if x % 5:
                    r, g, b, a = px[x, y]
                    px[x, y] = (r, g, b, 120)
    return img


def pipe_end(body, band, seed):
    img = tex.noise_fill(body, seed + 'e', 0.06)
    px = img.load()
    c = tex.rgb(band)
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d > 6:
                px[x, y] = tex.shade(c, 0.8)
            elif d < 3:
                px[x, y] = (30, 30, 34, 255)
    return img
