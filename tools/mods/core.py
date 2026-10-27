from gen.fw import Mod


def build():
    m = Mod('fw_core', 'Core', 'Shared framework for every Frontierworks mod: Forge Energy, the machine system, '
                               'data-driven machine recipes, loot injection and the creative tabs.', tab='MACHINES')
    m.init('CoreHooks.init(bus)')
    m.client('com.frontierworks.core.client.CoreClient.init(bus)')

    up = {'speed': ('#e74c3c', 'Speed Upgrade'), 'efficiency': ('#2ecc71', 'Efficiency Upgrade'),
          'capacity': ('#3498db', 'Capacity Upgrade'), 'range': ('#f1c40f', 'Range Upgrade')}
    for t, (col, nice) in up.items():
        m.item('%s_upgrade' % t, ('upgrade', col), java='new UpgradeItem(new Item.Properties(), UpgradeItem.Type.%s)' % t.upper(),
               jtype='UpgradeItem', tab='MACHINES', lang=nice)
    m.item('wrench', ('hook', '#9aa3ad'), java='new WrenchItem(new Item.Properties())', jtype='WrenchItem', tab='MACHINES',
           lang="Engineer's Wrench", desc='Right-click: rotate a block\nSneak + right-click: dismantle Frontierworks blocks')

    m.shaped('wrench', 'fw_core:wrench', [' I ', ' II', 'I  '], {'I': '#forge:ingots/iron'})
    m.shaped('speed_upgrade', 'fw_core:speed_upgrade', ['RGR', 'CBC', 'RGR'],
             {'R': '#forge:dusts/redstone', 'G': '#forge:ingots/gold', 'C': 'fw_circuits:basic_circuit', 'B': 'fw_parts:blank_card'})
    m.shaped('efficiency_upgrade', 'fw_core:efficiency_upgrade', ['RGR', 'CBC', 'RGR'],
             {'R': '#forge:dusts/redstone', 'G': '#forge:gems/emerald', 'C': 'fw_circuits:basic_circuit', 'B': 'fw_parts:blank_card'})
    m.shaped('capacity_upgrade', 'fw_core:capacity_upgrade', ['RGR', 'CBC', 'RGR'],
             {'R': '#forge:dusts/redstone', 'G': 'fw_batteries:battery_basic', 'C': 'fw_circuits:basic_circuit', 'B': 'fw_parts:blank_card'})
    m.shaped('range_upgrade', 'fw_core:range_upgrade', ['RGR', 'CBC', 'RGR'],
             {'R': '#forge:dusts/redstone', 'G': '#forge:ender_pearls', 'C': 'fw_circuits:basic_circuit', 'B': 'fw_parts:blank_card'})

    for k, v in {
        'itemGroup.fw_core.materials': 'Frontierworks: Materials', 'itemGroup.fw_core.machines': 'Frontierworks: Machines & Power',
        'itemGroup.fw_core.logistics': 'Frontierworks: Logistics', 'itemGroup.fw_core.tools': 'Frontierworks: Tools & Gear',
        'itemGroup.fw_core.adventure': 'Frontierworks: Adventure', 'itemGroup.fw_core.world': 'Frontierworks: World',
        'itemGroup.fw_core.space': 'Frontierworks: Space', 'itemGroup.fw_core.decor': 'Frontierworks: Decoration',
        'gui.fw_core.generating': 'Generating: %s FE/t', 'gui.fw_core.empty': 'Empty',
        'gui.fw_core.on': 'On', 'gui.fw_core.off': 'Off', 'gui.fw_core.start': 'Start', 'gui.fw_core.stop': 'Stop',
        'tooltip.fw_core.upgrade.speed': 'Machines work 30% faster per card, using more energy',
        'tooltip.fw_core.upgrade.efficiency': 'Machines use 20% less energy per card',
        'tooltip.fw_core.upgrade.capacity': 'Adds the base energy capacity again per card',
        'tooltip.fw_core.upgrade.range': 'Extends the working area of area machines',
        'msg.fw_core.empty_bag': 'There was nothing useful inside', 'key.categories.frontierworks': 'Frontierworks',
        'msg.fw_core.destination_unavailable': 'That destination is unavailable',
    }.items():
        m.tr(k, v)
    return m
