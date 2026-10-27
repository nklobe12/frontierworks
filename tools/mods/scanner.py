from gen.fw import Mod


def build():
    m = Mod('fw_scanner', 'Ore Scanner', 'Handheld ore scanners that outline every ore around you through solid rock for 15 seconds '
                                         'and list what they found. Runs on Forge Energy.', tab='TOOLS')
    rl = m.rl
    m.init('OreScannerItem.init()')
    m.client('com.frontierworks.scanner.client.ScannerClient.init(bus)')
    m.item('ore_scanner', ('tablet', '#2f7d32', None, '#7ee67e'), java='new OreScannerItem(new Item.Properties(), 100000, 8, 2000)',
           jtype='OreScannerItem', desc='Right-click to reveal ores within 8 blocks (2,000 FE)')
    m.item('advanced_ore_scanner', ('tablet', '#1d4fa3', None, '#7ec8ff'), java='new OreScannerItem(new Item.Properties(), 1000000, 16, 8000)',
           jtype='OreScannerItem', desc='Right-click to reveal ores within 16 blocks (8,000 FE)')
    m.shaped('ore_scanner', rl('ore_scanner'), ['GCG', 'PDP', 'PBP'], {'G': '#forge:glass_panes', 'C': 'fw_circuits:basic_circuit', 'P': '#forge:plates/iron',
                                                                       'D': 'minecraft:compass', 'B': 'fw_batteries:battery_basic'})
    m.shaped('advanced_ore_scanner', rl('advanced_ore_scanner'), ['ECE', 'PSP', 'PBP'], {'E': 'fw_crystals:energite_shard', 'C': 'fw_circuits:advanced_circuit',
                                                                                        'P': '#forge:plates/steel', 'S': rl('ore_scanner'), 'B': 'fw_batteries:battery_advanced'})
    m.tr('msg.fw_scanner.no_power', 'Scanner needs charging')
    m.tr('msg.fw_scanner.none', 'No ores nearby')
    return m
