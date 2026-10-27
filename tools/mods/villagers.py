"""Frontier Villagers: four new professions - engineer, surveyor, miner and botanist - each with a workstation and trades."""
from PIL import Image

from gen.fw import Mod, const, horizontal_state
from gen import tex

M = 'fw_villagers'

PROFS = {
    # profession: (workstation, coat, accent, hat colour or None, extra, work sound, station textures)
    'engineer': ('engineering_bench', '#d2691e', '#3b3b3b', None, 'goggles', 'VILLAGER_WORK_TOOLSMITH'),
    'surveyor': ('survey_table', '#6b7f3a', '#8b5a2b', '#5a3d1e', 'brim', 'VILLAGER_WORK_CARTOGRAPHER'),
    'miner': ('ore_cart', '#5a5f66', '#c9a227', '#e2b007', 'lamp', 'VILLAGER_WORK_ARMORER'),
    'botanist': ('potting_bench', '#4f8a3a', '#e8d9a8', '#d9c27a', 'brim', 'VILLAGER_WORK_FARMER'),
}


def overlay(coat, accent, hat, extra, seed):
    """Villager profession overlay (vanilla villager UV layout)."""
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    px = img.load()
    c, a = tex.rgb(coat), tex.rgb(accent)

    def put(x, y, col, k=1.0):
        n = k * (0.92 + tex.rnd(seed, x, y) * 0.16)
        px[x, y] = (tex.clamp(col[0] * n), tex.clamp(col[1] * n), tex.clamp(col[2] * n), 255)

    def cuboid(u, v, w, h, d, col, rows=None):
        faces = [(u + d, v, w, d), (u + d + w, v, w, d), (u, v + d, d + w + d + w, h)]
        for (x0, y0, ww, hh) in faces:
            for y in range(y0, y0 + hh):
                if rows and not (rows[0] <= y - (v + d) < rows[1]) and y >= v + d:
                    continue
                for x in range(x0, x0 + ww):
                    put(x, y, col)

    # robe (jacket layer): u0 v38, 8x20x6
    cuboid(0, 38, 8, 20, 6, c)
    for x in range(0, 28):
        put(x, 44 + 7, a, 0.7)         # belt
    for y in range(44, 64):
        for x in (9, 10):
            put(x, y, a, 0.9)          # front placket / apron strip
    # sleeves (arms): u44 v22 4x8x4 and u40 v38 8x4x4 (crossed arms)
    cuboid(44, 22, 4, 8, 4, c, rows=(0, 5))
    cuboid(40, 38, 8, 4, 4, c)
    # hat layer: u32 v0, 8x10x8
    if hat:
        h = tex.rgb(hat)
        cuboid(32, 0, 8, 10, 8, h, rows=(0, 3))
        if extra == 'lamp':
            for (x, y) in ((43, 9), (44, 9), (43, 10), (44, 10)):
                px[x, y] = (255, 250, 200, 255)
    if extra == 'goggles':
        for x in range(32, 64):
            put(x, 8 + 3, (40, 40, 44))
        for (x, y) in ((41, 11), (42, 11), (45, 11), (46, 11)):
            px[x, y] = (120, 210, 255, 255)
    if extra == 'brim' and hat:
        # brim: u30 v47, 16x16x1 plane (front face 31..47, 48..64)
        h = tex.rgb(hat)
        for y in range(48, 64):
            for x in range(31, 47):
                if (x - 39) ** 2 + (y - 56) ** 2 <= 60:
                    put(x, y, h, 0.95)
    return img


def station_textures(name, coat, accent):
    wood = '#9c7a4a'
    top = tex.planks(wood, name + 't')
    side = tex.planks('#7a5a34', name + 's')
    front = tex.planks('#7a5a34', name + 'f')
    pt, pf = top.load(), front.load()
    a, c = tex.rgb(accent), tex.rgb(coat)
    if name == 'engineering_bench':
        for x in range(2, 14):
            pt[x, 4] = (90, 90, 96, 255)
            pt[x, 11] = (90, 90, 96, 255)
        for (x, y) in ((4, 7), (5, 7), (6, 8), (10, 6), (11, 7), (10, 8)):
            pt[x, y] = (200, 120, 50, 255)
        for x in range(3, 13):
            pf[x, 3] = (60, 60, 64, 255)
            pf[x, 4] = (160, 160, 170, 255)
    elif name == 'survey_table':
        for y in range(3, 13):
            for x in range(2, 14):
                pt[x, y] = (230, 220, 180, 255) if (x + y) % 5 else (120, 160, 90, 255)
        pt[8, 7] = (200, 40, 40, 255)
        pt[9, 8] = (200, 40, 40, 255)
    elif name == 'ore_cart':
        for y in range(2, 14):
            for x in range(2, 14):
                pt[x, y] = (110, 110, 116, 255) if (x * 7 + y * 3) % 4 else (200, 170, 60, 255)
        for x in range(1, 15):
            pf[x, 12] = (50, 50, 54, 255)
        for (x, y) in ((4, 13), (11, 13)):
            pf[x, y] = (30, 30, 30, 255)
    elif name == 'potting_bench':
        for (cx, cy) in ((5, 5), (11, 6), (7, 11)):
            for dy in range(-1, 2):
                for dx in range(-1, 2):
                    pt[cx + dx, cy + dy] = (170, 90, 60, 255)
            pt[cx, cy] = (70, 140, 50, 255)
        for x in range(2, 14):
            pf[x, 6] = (90, 140, 60, 255)
    return top, side, front


def build():
    m = Mod(M, 'Frontier Villagers', 'Four new village professions - the Engineer, Surveyor, Miner and Botanist - who trade tech parts, '
                                     'treasure maps, ores and exotic plants. Give a villager their workstation to recruit one.', tab='DECOR')
    m.imp('com.google.common.collect.ImmutableSet', 'net.minecraft.world.entity.ai.village.poi.PoiType',
          'net.minecraft.world.entity.npc.VillagerProfession', 'net.minecraft.sounds.SoundEvents')
    rl = m.rl
    t = lambda n: '%s:block/%s' % (M, n)
    for prof, (station, coat, accent, hat, extra, sound) in PROFS.items():
        top, side, front = station_textures(station, coat, accent)
        model = {'parent': 'minecraft:block/orientable', 'textures': {'top': t(station + '_top'), 'side': t(station + '_side'), 'front': t(station + '_front')}}
        state = {'variants': {'facing=%s' % f: dict({'model': t(station)}, **({'y': y} if y else {})) for f, y in
                              (('north', 0), ('east', 90), ('south', 180), ('west', 270))}}
        m.block(station, java='new com.frontierworks.villagers.WorkstationBlock(FWProps.wood())', jtype='com.frontierworks.villagers.WorkstationBlock',
                models={station: model}, state=state, mine='axe', tab='DECOR', desc='Workstation of the %s' % prof.title(),
                textures={'block/%s_top' % station: top, 'block/%s_side' % station: side, 'block/%s_front' % station: front})
        P = const(prof)
        m.field('public static final RegistryObject<PoiType> %s_POI = REG.poiTypes().register("%s", () -> new PoiType(ImmutableSet.copyOf(Content.%s.get().getStateDefinition().getPossibleStates()), 1, 1));'
                % (P, prof, const(station)))
        m.field('public static final RegistryObject<VillagerProfession> %s = REG.professions().register("%s", () -> new VillagerProfession("%s", h -> h.is(Content.%s_POI.getKey()), h -> h.is(Content.%s_POI.getKey()), ImmutableSet.of(), ImmutableSet.of(), SoundEvents.%s));'
                % (P, prof, prof, P, P, sound))
        img = overlay(coat, accent, hat, extra, M + prof)
        m.texture('entity/villager/profession/' + prof, img)
        m.texture('entity/zombie_villager/profession/' + prof, img)
        m.tr('entity.minecraft.villager.%s.%s' % (M, prof), prof.title())
        m.tag('point_of_interest_type', 'minecraft:acquirable_job_site', rl(prof))
    m.shaped('engineering_bench', rl('engineering_bench'), ['II', 'PP'], {'I': '#forge:ingots/iron', 'P': '#minecraft:planks'})
    m.shaped('survey_table', rl('survey_table'), ['MC', 'PP'], {'M': 'minecraft:map', 'C': 'minecraft:compass', 'P': '#minecraft:planks'})
    m.shaped('ore_cart', rl('ore_cart'), ['I I', 'IRI'], {'I': '#forge:ingots/iron', 'R': '#forge:raw_materials'})
    m.shaped('potting_bench', rl('potting_bench'), ['FF', 'PP'], {'F': 'minecraft:flower_pot', 'P': '#minecraft:planks'})
    m.init('com.frontierworks.villagers.Trades.init()')
    return m
