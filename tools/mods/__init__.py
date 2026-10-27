# Spec modules in load order. Each defines build() which creates one fw.Mod.
ORDER = [
    'core', 'metals', 'alloys', 'parts', 'gems', 'minerals', 'stone', 'rubber', 'crystals', 'oil', 'meteors',
    'kiln', 'machines', 'alloysmelter', 'sifting', 'sawmill', 'centrifuge', 'orewasher', 'press', 'circuits',
    'chemistry', 'electrolysis', 'petro', 'freezer', 'recycler',
    'generators', 'steam', 'solar', 'wind', 'hydro', 'geothermal', 'batteries', 'cables', 'wireless', 'nuclear', 'fusion',
    'pipes', 'fluidducts', 'tanks', 'storage', 'conveyors', 'vacuum', 'quarry', 'pump', 'farming', 'mobgrinder', 'chunkloader',
    'redstone', 'teleporters', 'elevators', 'beacons',
    'tools', 'armor', 'hammers', 'drills', 'jetpack', 'grapple', 'backpacks', 'sleepingbags', 'compass', 'waystones', 'rope',
    'flares', 'scanner', 'glider', 'goggles', 'buildingtools', 'magnet', 'qol',
    'ruins', 'dungeons', 'towers', 'skyislands', 'outposts', 'bunkers', 'temples', 'camps',
    'caves', 'flora', 'treasure', 'villagers',
    'glass', 'lamps', 'decor', 'food', 'potions', 'enchantments',
    'weapons', 'blasters', 'explosives', 'relics', 'hearts', 'coins',
    'mobs', 'colossus', 'wraithking', 'wyvern',
    'bounties', 'arena',
    'depths', 'skylands', 'moon', 'mars', 'rocketry', 'spacesuit',
]
