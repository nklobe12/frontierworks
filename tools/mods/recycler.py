from gen.fw import Mod, machine


def build():
    m = Mod('fw_recycler', 'Recycler', 'Turn junk into opportunity: the Recycler grinds any item into scrap, and scrap boxes '
                                       'hold random useful loot - from nuggets to circuits to the occasional diamond.', tab='MACHINES')
    rl = m.rl
    m.item('scrap', ('raw', '#7a6a5a'), desc='Recycled junk. Nine make a scrap box.')
    m.item('scrap_box', ('backpack', '#8a7a5a'), java='new ScrapBoxItem(new Item.Properties())', jtype='ScrapBoxItem',
           desc='Right-click to open. Sneak to open the whole stack.')
    m.field('public static final MachineSpec RECYCLER_SPEC = new MachineSpec(MachineLayout.processing(1, 1, 0, 0, 0, true, 3, true), new RecyclerSource(), 20000, 200, 1.0f, 1.0f);')
    machine(m, 'recycler', 'new ProcessingMachineBE(BE_TYPE, pos, state, Content.RECYCLER_SPEC)', 'basic', 'crush',
            desc='Grinds anything into scrap (1 in 8 chance)')
    m.shaped('scrap_box', rl('scrap_box'), ['SSS', 'SSS', 'SSS'], {'S': rl('scrap')})
    m.shaped('recycler', rl('recycler'), ['PCP', 'DMD', 'PWP'], {'P': '#forge:plates/iron', 'C': 'fw_machines:compressor', 'D': 'minecraft:dirt',
                                                                 'M': 'fw_parts:machine_frame', 'W': 'fw_circuits:basic_circuit'})
    entries = [('minecraft:iron_nugget', 20, 2, 6), ('minecraft:gold_nugget', 10, 1, 4), ('fw_metals:tin_nugget', 14, 2, 6),
               ('fw_metals:copper_dust', 12, 1, 3), ('minecraft:redstone', 12, 2, 6), ('minecraft:glowstone_dust', 6, 1, 4),
               ('fw_parts:iron_plate', 8, 1, 2), ('fw_parts:copper_wire', 8, 1, 3), ('fw_circuits:basic_circuit', 4, 1, 1),
               ('fw_circuits:silicon', 4, 1, 2), ('minecraft:bone', 8, 1, 3), ('minecraft:string', 8, 1, 3),
               ('minecraft:coal', 12, 1, 3), ('minecraft:slime_ball', 4, 1, 2), ('minecraft:diamond', 1, 1, 1),
               ('minecraft:emerald', 2, 1, 1), ('fw_rubber:rubber', 6, 1, 3), ('minecraft:bread', 6, 1, 2)]
    m.data('loot_tables/scrap_box.json', {'type': 'minecraft:gift', 'pools': [{'rolls': 1, 'bonus_rolls': 0, 'entries': [
        {'type': 'minecraft:item', 'name': it, 'weight': w, 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': a, 'max': b}, 'add': False}]}
        for it, w, a, b in entries]}]})
    return m
