"""Field Guide: an in-game book describing every Frontierworks mod and its items, plus a progression advancement tree."""
from gen import fw
from gen.fw import Mod
from gen import tex

M = 'fw_guide'

CATS = [
    ('MATERIALS', 'Materials', 'fw_metals:tin_ingot', 'Ores, metals, alloys, gems and crafting parts - the foundation of everything you build.'),
    ('MACHINES', 'Machines & Power', 'fw_machines:pulverizer', 'Generators, batteries and processing machines. Start with the kiln and a combustion generator, then work up to fusion.'),
    ('LOGISTICS', 'Logistics', 'fw_pipes:item_pipe', 'Move items, fluids and energy around, store them, and automate farms and mining.'),
    ('TOOLS', 'Tools & Gear', 'fw_jetpack:jetpack_basic', 'Tools, weapons, armor and gadgets for getting around the frontier.'),
    ('ADVENTURE', 'Adventure', 'fw_relics:amulet_of_vigor', 'Creatures, bosses, relics, bounties, trials and treasure.'),
    ('WORLD', 'Exploration', 'fw_rubber:rubber_sapling', 'Structures to discover, new plants and caves, and gates to other dimensions.'),
    ('SPACE', 'Space', 'fw_rocketry:rocket_t1', 'Rockets, space suits, the Moon and Mars.'),
    ('DECOR', 'Decoration', 'fw_lamps:lamp_white', 'Lamps, glass and building blocks.'),
]

# advancement tree: (id, parent, icon, title, description, trigger)
ADV = [
    ('root', None, 'fw_guide:field_guide', 'Frontierworks', 'Open a new frontier', ('item', ['fw_guide:field_guide'])),
    ('first_metal', 'root', 'fw_metals:tin_ingot', 'Frontier Metals', 'Smelt a tin, lead, silver or nickel ingot',
     ('item', ['fw_metals:tin_ingot', 'fw_metals:lead_ingot', 'fw_metals:silver_ingot', 'fw_metals:nickel_ingot'])),
    ('kiln', 'first_metal', 'fw_kiln:alloy_kiln', 'Hot Work', 'Build an alloy kiln or coke oven', ('item', ['fw_kiln:alloy_kiln', 'fw_kiln:coke_oven'])),
    ('power', 'kiln', 'fw_generators:combustion_generator', 'Let There Be Power', 'Build any generator',
     ('item', ['fw_generators:combustion_generator', 'fw_solar:solar_panel_basic', 'fw_wind:wind_turbine', 'fw_hydro:water_wheel',
               'fw_steam:steam_engine', 'fw_geothermal:geothermal_generator'])),
    ('pulverizer', 'power', 'fw_machines:pulverizer', 'Ore Doubling', 'Build a pulverizer', ('item', ['fw_machines:pulverizer'])),
    ('jetpack', 'pulverizer', 'fw_jetpack:jetpack_basic', 'Rocket Man', 'Craft a jetpack', ('item', ['fw_jetpack:jetpack_basic'])),
    ('quarry', 'pulverizer', 'fw_quarry:quarry', 'Strip Mining', 'Build a quarry', ('item', ['fw_quarry:quarry'])),
    ('fusion', 'quarry', 'fw_fusion:fusion_reactor', 'A Star in a Box', 'Build a fusion reactor', ('item', ['fw_fusion:fusion_reactor'])),
    ('explore', 'root', 'fw_treasure:explorers_map', 'Off the Map', 'Find a treasure map', ('item', ['fw_treasure:explorers_map', 'fw_treasure:dungeon_map',
                                                                                                'fw_treasure:ruins_map', 'fw_treasure:tech_map', 'fw_treasure:stash_map'])),
    ('waystone', 'explore', 'fw_waystones:waystone', 'Fast Travel', 'Craft or find a waystone', ('item', ['fw_waystones:waystone'])),
    ('depths', 'explore', 'fw_depths:depths_gate', 'Into the Depths', 'Travel to the Depths', ('dim', 'fw_depths:depths')),
    ('skylands', 'explore', 'fw_skylands:skylands_gate', 'Head in the Clouds', 'Travel to the Skylands', ('dim', 'fw_skylands:skylands')),
    ('relic', 'root', 'fw_relics:amulet_of_vigor', 'Relic Hunter', 'Find a relic', ('item', ['fw_relics:amulet_of_vigor', 'fw_relics:ring_of_haste',
                                                                                         'fw_relics:ring_of_swiftness', 'fw_relics:featherfall_charm',
                                                                                         'fw_relics:ember_heart', 'fw_relics:tidal_pearl', 'fw_relics:eye_of_the_owl',
                                                                                         'fw_relics:phoenix_feather', 'fw_relics:lucky_coin', 'fw_relics:thorn_ring',
                                                                                         'fw_relics:band_of_regeneration', 'fw_relics:warding_amulet'])),
    ('heart', 'relic', 'fw_hearts:heart_container', 'Big Hearted', 'Obtain a heart container', ('item', ['fw_hearts:heart_container'])),
    ('bounty', 'relic', 'fw_bounties:bounty_board', 'Bounty Hunter', 'Take a bounty contract', ('item', ['fw_bounties:bounty_contract'])),
    ('colossus', 'heart', 'fw_colossus:colossus_core', 'Scrap Metal', 'Defeat the Iron Colossus', ('kill', 'fw_colossus:iron_colossus')),
    ('wraith', 'heart', 'fw_wraithking:cursed_crown', 'Regicide', 'Defeat the Wraith King', ('kill', 'fw_wraithking:wraith_king')),
    ('wyvern', 'heart', 'fw_wyvern:wyvern_lure', 'Dragon Slayer', 'Defeat the Sky Wyvern', ('kill', 'fw_wyvern:sky_wyvern')),
    ('rocket', 'power', 'fw_rocketry:rocket_t1', 'Ready for Launch', 'Build a Tier 1 rocket', ('item', ['fw_rocketry:rocket_t1'])),
    ('moon', 'rocket', 'fw_moon:moon_rock', 'One Small Step', 'Land on the Moon', ('dim', 'fw_moon:moon')),
    ('mars', 'moon', 'fw_mars:mars_sand', 'The Red Planet', 'Land on Mars', ('dim', 'fw_mars:mars')),
    ('titanium', 'mars', 'fw_mars:titanium_ingot', 'Space Age Metal', 'Smelt a titanium ingot', ('item', ['fw_mars:titanium_ingot'])),
]


def build():
    m = Mod(M, 'Field Guide', 'The Frontierworks field guide: an in-game book (given on first join) with a page for every mod and the '
                              'items it adds, and a progression advancement tree from your first ingot to Mars.', tab='TOOLS')
    rl = m.rl
    m.init('com.frontierworks.guide.GuideEvents.init()')
    m.item('field_guide', ('tablet', '#6b4a2a', None, '#d4af37') if 'tablet' in tex.T else ('scroll', '#d4af37'),
           java='new com.frontierworks.guide.FieldGuideItem(new Item.Properties().stacksTo(1))', jtype='com.frontierworks.guide.FieldGuideItem',
           desc='Right-click to read about every Frontierworks mod')
    m.shapeless('field_guide', rl('field_guide'), ['minecraft:book', 'minecraft:compass'])
    for k, v in {'guide.fw_guide.title': 'Frontierworks Field Guide', 'guide.fw_guide.mods': '%s mods', 'guide.fw_guide.items': 'Items & blocks (%s)',
                 'guide.fw_guide.prev': '<', 'guide.fw_guide.next': '>', 'guide.fw_guide.pick': 'Pick a mod on the left to read about it.'}.items():
        m.tr(k, v)
    # guide data from every other mod
    cats = []
    for key, title, icon, intro in CATS:
        mods = []
        for mod in fw.ALL_MODS:
            if mod is m or mod.tab != key:
                continue
            items = []
            for k in mod.lang:
                parts = k.split('.')
                if len(parts) == 3 and parts[0] in ('item', 'block') and parts[1] == mod.modid and not k.endswith('_spawn_egg'):
                    items.append('%s:%s' % (mod.modid, parts[2]))
            mods.append({'id': mod.modid, 'name': mod.name, 'desc': mod.desc, 'items': list(dict.fromkeys(items))})
        cats.append({'id': key.lower(), 'title': title, 'icon': icon, 'intro': intro, 'mods': mods})
    m.asset('guide/guide.json', {'categories': cats})
    # advancements
    for (aid, parent, icon, title, desc, trig) in ADV:
        kind = trig[0]
        if kind == 'item':
            crit = {'has': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': trig[1]}]}}}
        elif kind == 'dim':
            crit = {'arrived': {'trigger': 'minecraft:changed_dimension', 'conditions': {'to': trig[1]}}}
        else:
            crit = {'killed': {'trigger': 'minecraft:player_killed_entity', 'conditions': {'entity': [
                {'condition': 'minecraft:entity_properties', 'entity': 'this', 'predicate': {'type': trig[1]}}]}}}
        disp = {'icon': {'item': icon}, 'title': {'translate': 'advancements.fw_guide.%s.title' % aid},
                'description': {'translate': 'advancements.fw_guide.%s.description' % aid},
                'frame': 'challenge' if kind == 'kill' or aid in ('mars', 'fusion') else ('goal' if kind == 'dim' else 'task'),
                'show_toast': True, 'announce_to_chat': True, 'hidden': False}
        if parent is None:
            disp['background'] = 'minecraft:textures/block/stone_bricks.png'
        adv = {'display': disp, 'criteria': crit}
        if parent:
            adv['parent'] = rl(parent)
        if kind == 'kill':
            adv['rewards'] = {'experience': 250}
        m.data('advancements/%s.json' % aid, adv)
        m.tr('advancements.fw_guide.%s.title' % aid, title)
        m.tr('advancements.fw_guide.%s.description' % aid, desc)
    return m
