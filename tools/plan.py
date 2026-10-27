"""The Lodestar master plan: every planned mod, its creative tab, build wave and what it does.

This file is the source of truth for scope. tools/build.py checks that each built mod is listed here, and
tools/plan.py --md writes PLAN.md (status per mod). A mod counts as done when tools/mods/<pkg>.py exists.
Depth bar for every themed mod: a coherent theme, several hand-written mechanics (block entities, items with
behaviour, entities, worldgen or events), supporting content and recipes, a Codex chapter and quests.
"""
import sys
from pathlib import Path

TABS = [
    ('CORE', 'Lodestar'), ('MATERIALS', 'Materials & Metallurgy'), ('INDUSTRY', 'Industry'), ('POWER', 'Power'),
    ('LOGISTICS', 'Logistics & Storage'), ('AUTOMATION', 'Automation & Computing'), ('TOOLS', 'Tools & Weapons'),
    ('GEAR', 'Gear & Gadgets'), ('ALCHEMY', 'Alchemy & Transmutation'), ('EXPLORATION', 'Exploration'),
    ('NATURE', 'Nature'), ('CREATURES', 'Creatures & Adventure'), ('DIMENSIONS', 'Dimensions'), ('SPACE', 'Space'),
    ('FARMING', 'Farming & Food'), ('BUILDING', 'Building'), ('TRANSPORT', 'Transport'),
]

# (id, name, tab, wave, pitch)
P = [
    # ------------------------------------------------------------------ core & navigation
    ('ls_core', 'Core', 'CORE', 1, 'Shared engine: energy, machines, screens, multiblocks, duct networks, player data, worldgen helpers, self-test.'),
    ('ls_lens', 'Lens', 'CORE', 1, 'Recipe viewer: searchable item list beside every inventory, recipes and uses for every recipe type, bookmarks, fill-the-grid transfer.'),
    ('ls_codex', 'Field Codex', 'CORE', 1, 'In-game manual: a chapter per mod with linked recipes, item lookup from Lens, progress-aware chapters and search.'),
    ('ls_quests', 'Expedition Log', 'CORE', 2, 'Quest book: chapters per tier and theme, obtain/craft/kill/visit tasks, rewards, a tracked-quest HUD.'),
    ('ls_probe', 'Probe', 'CORE', 2, 'Crosshair info: block and entity name, owning mod, tool needed, machine energy/progress/fluids/inventory, mob health and effects.'),
    ('ls_cartograph', 'Cartograph', 'CORE', 3, 'Minimap and fullscreen world map with terrain and cave modes, waypoints, death markers and discovered structure markers.'),
    ('ls_hud', 'Explorer HUD', 'CORE', 3, 'Compass bar with waypoint pins, coordinates/biome/time, armor and tool durability, status effect timers.'),
    ('ls_packrat', 'Packrat', 'CORE', 7, 'Inventory sorting, hotbar auto-refill for blocks and tools, quick-stack to nearby chests and slot locking.'),
    ('ls_proficiency', 'Proficiency', 'CORE', 7, 'Skills (mining, combat, engineering, exploring, farming, smithing) that level from actions and unlock perk trees.'),
    ('ls_research', 'Research', 'CORE', 3, 'Research table and notebooks; field samples from biomes, creatures and dimensions unlock advanced recipes across the pack.'),
    ('ls_lodestar', 'Lodestar', 'CORE', 3, 'The storyline: a lost expedition\'s journals, a lodestar compass pointing to story sites, constellation altars and a final vault.'),
    ('ls_graves', 'Tombstones', 'CORE', 7, 'Graves keep your items on death, a grave compass leads back, and old graves in the world hide ghosts and loot.'),

    # ------------------------------------------------------------------ materials & metallurgy
    ('ls_veins', 'Deep Veins', 'MATERIALS', 1, 'Ten base metals that generate as huge ore veins with surface outcrops, a prospector\'s pick and seismic surveyor, and vein maps.'),
    ('ls_metalwork', 'Metalwork', 'MATERIALS', 1, 'Bloomery multiblock for steel, alloy kiln, smithing anvil with a hammer-strike minigame, heated metal and tongs, eight alloys.'),
    ('ls_lapidary', 'Lapidary', 'MATERIALS', 2, 'Rough gems from geodes, a cutting wheel with cut-quality choices, gem sockets on tools and armor, jeweler\'s table.'),
    ('ls_resonance', 'Resonance', 'MATERIALS', 2, 'Crystals grown from seeds in nutrient vats, crystal clusters, tuning-fork frequencies and resonance links for wireless redstone.'),
    ('ls_strata', 'Strata', 'MATERIALS', 3, 'Rock types in layered strata (marble, slate, limestone, schist, basalts) with full building sets, rock samples and a geological survey.'),
    ('ls_paleo', 'Paleontology', 'MATERIALS', 3, 'Fossils in sedimentary strata, brush excavation, a cleaning bench, DNA sequencing, six revivable prehistoric creatures and museum stands.'),
    ('ls_starfall', 'Starfall', 'MATERIALS', 3, 'Meteors fall as world events leaving craters, meteoric iron and starmetal, crater cores, and a telescope that predicts impact sites.'),
    ('ls_salvage', 'Salvage', 'MATERIALS', 2, 'Scrap from broken gear and junk piles, a recycler, scrap boxes, junkyard structures and salvage tools that refund materials.'),
    ('ls_chemworks', 'Chemworks', 'MATERIALS', 2, 'Twenty chemical compounds, electrolyzer, chemical reactor and mixer, a lab bench with test tubes, corrosive and toxic hazards.'),
    ('ls_petroleum', 'Petroleum', 'MATERIALS', 2, 'Oil reservoirs and tar pits, pumpjack and distillation-column multiblocks, diesel, kerosene, naphtha, plastics, rubber and asphalt.'),
    ('ls_arboretum', 'Arboretum', 'MATERIALS', 2, 'Tree tapping for rubber, maple, resin and amber, an evaporator, six new trees with fruit and a growth accelerator.'),
    ('ls_glassworks', 'Glassworks', 'MATERIALS', 2, 'Glass furnace with molten glass, glassblowing bench, lenses and flasks, framed, tempered, one-way and insulated glass.'),
    ('ls_ceramics', 'Ceramics', 'MATERIALS', 6, 'Pottery wheel, kiln firing, glazes, ceramic tiles, storage jars, crucibles and refractory bricks.'),
    ('ls_electronics', 'Electronics', 'MATERIALS', 2, 'Silicon wafers, lithography, PCB etcher and circuit assembler making four tiers of circuits and processors used across the pack.'),
    ('ls_textiles', 'Textiles', 'MATERIALS', 6, 'Flax and cotton, spinning wheel, loom with patterns, dye vat, cloth types and insulated clothing for Climate.'),
    ('ls_tannery', 'Tannery', 'MATERIALS', 6, 'Hides from animals, tanning rack and vat, leather grades, saddlery, leather armor upgrades and bags.'),

    # ------------------------------------------------------------------ industry
    ('ls_machina', 'Machina', 'INDUSTRY', 1, 'Fourteen processing machines (pulverizer, smelter, sawmill, insolator, centrifuge, crucible, chiller, press, refinery, bottler...), augments, side configuration and redstone control.'),
    ('ls_kinetics', 'Kinetics', 'INDUSTRY', 2, 'Rotational power: shafts, gears, belts, water wheels and windmills driving a millstone, mechanical press, mixer and fan processing.'),
    ('ls_steamworks', 'Steamworks', 'INDUSTRY', 2, 'Multiblock boilers sized by footprint, steam pipes with pressure, steam engines and turbines, a steam hammer and overpressure explosions.'),
    ('ls_clockwork', 'Clockwork', 'INDUSTRY', 6, 'Springs and wind-up mechanisms, clockwork tools, timers, music boxes and wind-up automatons that do simple chores.'),
    ('ls_pneumatics', 'Pneumatics', 'INDUSTRY', 4, 'Air compressors, pressure tubes, pneumatic drill and nail gun, air cannons that launch items, and a pressure chamber.'),
    ('ls_hydraulics', 'Hydraulics', 'INDUSTRY', 2, 'Multiblock tanks, area pumps that drain lakes, valves, sprinklers, fluid cells, drains and fluid voids.'),
    ('ls_cryogenics', 'Cryogenics', 'INDUSTRY', 5, 'Air separation into liquid nitrogen and oxygen, a cryo freezer, superconductors, a freeze ray and stasis pods.'),
    ('ls_heavyindustry', 'Heavy Industry', 'INDUSTRY', 4, 'Large animated multiblocks: crusher, arc furnace, rolling mill and a bucket-wheel excavator.'),
    ('ls_replicator', 'Replicator', 'INDUSTRY', 5, 'Scan items into patterns, extract matter from scrap, and replicate items with matter and energy.'),
    ('ls_quarry', 'Quarry', 'INDUSTRY', 2, 'Frame quarry with a moving drill head, a laser quarry, ore filters and land restoration.'),
    ('ls_terraform', 'Terraform', 'INDUSTRY', 4, 'Terrain leveler, filler, biome converter and island raiser.'),
    ('ls_agritech', 'Agritech', 'INDUSTRY', 2, 'Planters, harvesters, sprinklers, fertilizer, and animal feeders, breeders and ranchers.'),
    ('ls_mechanisms', 'Mechanisms', 'INDUSTRY', 2, 'Block breaker, block placer, activator that uses items, fan, item chute and an auto-crafter.'),
    ('ls_blueprints', 'Blueprints', 'INDUSTRY', 4, 'Area scanner, blueprint items, a builder that constructs blueprints from supplied materials, and hologram previews.'),
    ('ls_optics', 'Optics', 'INDUSTRY', 4, 'Laser emitters, mirrors, splitters and lenses, a laser drill multiblock, laser power transmission and light-beam puzzles.'),
    ('ls_electromag', 'Electromagnetics', 'INDUSTRY', 5, 'Electromagnets, magnetic levitation platforms, a coilgun, induction heating and maglev rails.'),
    ('ls_fabricator', 'Fabricator', 'INDUSTRY', 5, '3D printers that print parts and tools from plastic filament using printable design cards.'),
    ('ls_pollution', 'Pollution', 'INDUSTRY', 5, 'Machines emit pollution into chunks; smog, acid rain and dying plants follow unless you build scrubbers, filters and catalytic converters.'),
    ('ls_tunnelborer', 'Tunnel Borer', 'INDUSTRY', 5, 'A tunnel boring machine that moves through terrain, lining tunnels and collecting what it digs.'),

    # ------------------------------------------------------------------ power
    ('ls_dynamos', 'Dynamos', 'POWER', 1, 'Seven generators (stirling, combustion, magmatic, steam, lapidary, biofuel, numismatic) with fuel tables and augments.'),
    ('ls_photovoltaics', 'Photovoltaics', 'POWER', 2, 'Solar panel tiers, sun-tracking panels, solar array multiblock and panels that need cleaning.'),
    ('ls_aeolus', 'Wind & Water', 'POWER', 2, 'Height-dependent wind turbines with wearing rotors, water wheels on flowing water and tidal generators.'),
    ('ls_geothermal', 'Geothermal', 'POWER', 3, 'Thermal vents in the world, magma taps, heat exchangers, binary-cycle generators and geysers.'),
    ('ls_fission', 'Fission', 'POWER', 4, 'Reactor multiblock with fuel-rod layouts, coolant loops, heat and meltdown, radiation, hazmat gear and reprocessing.'),
    ('ls_fusion', 'Fusion', 'POWER', 5, 'Tokamak multiblock, deuterium, tritium and helium-3 fuels, laser ignition and containment stability.'),
    ('ls_capacitors', 'Capacitors', 'POWER', 2, 'Energy cell tiers, capacitor bank multiblock, flywheel storage, energy meters and chargers.'),
    ('ls_tesla', 'Tesla', 'POWER', 4, 'Tesla coils with visible arcs, wireless charging, lightning harvesters and arc defenses.'),
    ('ls_bioenergy', 'Bioenergy', 'POWER', 4, 'Fermenters, a biogas digester, ethanol and biomass power.'),
    ('ls_antimatter', 'Antimatter', 'POWER', 8, 'Particle accelerator ring multiblock, antimatter containment and an annihilation reactor.'),
    ('ls_powergrid', 'Power Grid', 'POWER', 4, 'High-voltage pylons with hanging wires spanning long distances, transformers and substations.'),
    ('ls_fuelcells', 'Fuel Cells', 'POWER', 5, 'Hydrogen from electrolysis, fuel cell generators, hydrogen storage and hydrogen-powered tools.'),

    # ------------------------------------------------------------------ logistics & storage
    ('ls_ducts', 'Ducts', 'LOGISTICS', 1, 'Energy fluxducts, fluid ducts and item ducts with servos, filters and retrievers and routing, plus viaducts for player travel.'),
    ('ls_datastore', 'Datastore', 'LOGISTICS', 2, 'Digital storage network with drives, disks and searchable terminals, importers/exporters and pattern autocrafting.'),
    ('ls_drawers', 'Drawers', 'LOGISTICS', 2, 'High-capacity drawers, compacting drawers, drawer controllers, upgrades and framed drawers.'),
    ('ls_conveyors', 'Conveyors', 'LOGISTICS', 2, 'Belts that move items and entities, splitters, sorters, lifts, chutes and extractors.'),
    ('ls_enderlink', 'Enderlink', 'LOGISTICS', 4, 'Frequency-linked chests, tanks and energy cells set by color codes, plus ender pouches.'),
    ('ls_vacuum', 'Vacuum', 'LOGISTICS', 4, 'Vacuum hoppers, item magnets, XP collectors and filters.'),
    ('ls_postal', 'Postal Service', 'LOGISTICS', 6, 'Mailboxes, a post office, letters and parcels, and carrier pigeons that fly items to other mailboxes.'),
    ('ls_freight', 'Rail Freight', 'LOGISTICS', 6, 'Locomotives, cargo wagons, loaders and unloaders, signals, routing tickets and stations.'),
    ('ls_skyhaul', 'Skyhaul', 'LOGISTICS', 6, 'Cargo drones flying between drone pads on routes, delivery schedules and drone upgrades.'),
    ('ls_crates', 'Crates & Barrels', 'LOGISTICS', 4, 'Crate tiers, barrels for fluids, sorting crates, and pallets moved with hand trucks.'),
    ('ls_backpacks', 'Backpacks', 'LOGISTICS', 3, 'Tiered backpacks with upgrade modules (magnet, feeding, crafting, smelting, tanks).'),
    ('ls_telepads', 'Telepads', 'LOGISTICS', 4, 'Linked telepads, a personal teleporter and teleport beacons with energy cost by distance.'),
    ('ls_anchors', 'Anchors', 'LOGISTICS', 4, 'Chunk loaders with fuel, world anchors, personal anchors and a loaded-area display.'),

    # ------------------------------------------------------------------ automation & computing
    ('ls_logic', 'Logic', 'AUTOMATION', 3, 'Logic gates, bundled cables, timers, counters, latches, sequencers and numeric displays.'),
    ('ls_sensors', 'Sensors', 'AUTOMATION', 3, 'Player, entity and item detectors, weather and daylight sensors, block-update detectors and wireless redstone.'),
    ('ls_robotics', 'Robotics', 'AUTOMATION', 5, 'Programmable robots with instruction cards (mine, chop, farm, haul), charging docks and robot upgrades.'),
    ('ls_terminal', 'Terminal', 'AUTOMATION', 5, 'Programmable computers running a small scripting language, monitors, disk drives and machine peripherals.'),
    ('ls_drones', 'Drones', 'AUTOMATION', 5, 'Piloted camera drones, combat drones and mining drones with a first-person controller.'),
    ('ls_security', 'Security', 'AUTOMATION', 4, 'Keycards and readers, blast doors, security cameras viewable from monitors, alarms and laser tripwires.'),
    ('ls_turrets', 'Turrets', 'AUTOMATION', 4, 'Gun, laser, flame and tesla turrets with targeting rules, ammo and turret walls.'),
    ('ls_displays', 'Displays', 'AUTOMATION', 4, 'Info screens linked to machines, billboards, scrolling signs and energy and fluid gauges.'),
    ('ls_chronotech', 'Chronotech', 'AUTOMATION', 5, 'Tick accelerators, time-slowing fields, chrono crystals and a time-stop grenade.'),
    ('ls_sculktech', 'Sculk Engineering', 'AUTOMATION', 5, 'Sculk sensor networks, echo relays, vibration-powered machines and warden-proof shielding.'),
    ('ls_radio', 'Radio', 'AUTOMATION', 6, 'Radio transmitters and receivers carrying redstone and messages, walkie-talkies and signal towers.'),

    # ------------------------------------------------------------------ tools & weapons
    ('ls_toolsmith', 'Toolsmith', 'TOOLS', 1, 'Tools built from head, handle and binding parts in dozens of materials with traits, a part builder, tool station, smeltery multiblock with casting, modifiers and tool levels.'),
    ('ls_armory', 'Armory', 'TOOLS', 2, 'Armor assembled from plates, trim and padding in Toolsmith materials, armor traits and a plating station.'),
    ('ls_fletcher', 'Fletcher', 'TOOLS', 3, 'Bows, crossbows, slingshots and throwing weapons, quivers and special arrows (explosive, ender, rope).'),
    ('ls_blasters', 'Blasters', 'TOOLS', 4, 'Energy pistols, rifles, shotguns and a railgun with weapon mods (scopes, barrels, cells) and a weapon bench.'),
    ('ls_excavation', 'Excavation', 'TOOLS', 3, 'Mining laser, vein-mine toggle, seismic charges, a miner\'s helmet lamp and ore-highlighting goggles.'),
    ('ls_demolition', 'Demolition', 'TOOLS', 4, 'Dynamite, sticky bombs, C4 with detonators, ore-safe mining charges and an endgame nuke.'),
    ('ls_bulwark', 'Bulwark', 'TOOLS', 4, 'Shields from many materials, parry timing, shield bash and an energy shield projector.'),
    ('ls_trinkets', 'Trinkets', 'TOOLS', 3, 'Accessory slots (rings, amulet, belt, charm) and thirty trinkets from dungeons and crafting with set bonuses.'),
    ('ls_enchantworks', 'Enchantworks', 'TOOLS', 4, 'Liquid XP, a disenchanter, an enchantment library, an imbuement altar and fifteen new enchantments.'),
    ('ls_bladework', 'Bladework', 'TOOLS', 4, 'Spears, glaives, katanas and hammers with combo attacks, dodges and heavy strikes.'),
    ('ls_angling', 'Angling', 'TOOLS', 6, 'Fish species by biome, depth and time, rods, reels and lures, fish traps, aquariums and trophy fish.'),

    # ------------------------------------------------------------------ gear & gadgets
    ('ls_jetpacks', 'Jetpacks', 'GEAR', 3, 'Fuel and electric jetpack tiers with hover mode, thrusters and a flight HUD.'),
    ('ls_exosuit', 'Exosuit', 'GEAR', 4, 'Power armor with modules installed at a fabricator (night vision, step assist, shields, flight, jump boost) and energy management.'),
    ('ls_traversal', 'Traversal', 'GEAR', 3, 'Grappling hook with swing physics, hang glider with thermals, climbing picks, ziplines between posts and rope ladders.'),
    ('ls_diving', 'Diving', 'GEAR', 3, 'Diving suits with air hoses and pumps, rebreathers, underwater lights, a submarine pod and pearl beds.'),
    ('ls_lifesupport', 'Life Support', 'GEAR', 5, 'Spacesuits, oxygen tanks, an oxygen distributor that fills sealed rooms, airlocks and oxygen gardens.'),
    ('ls_climate', 'Climate', 'GEAR', 3, 'Body temperature from biome, weather, altitude and nearby blocks, insulated clothing, heaters and coolers.'),
    ('ls_camping', 'Camping', 'GEAR', 3, 'Tents that let you sleep without moving your spawn, sleeping bags, campfire grills, trail rations and hammocks.'),
    ('ls_buildkit', 'Builder\'s Kit', 'GEAR', 6, 'Building wand, exchanger, copy-paste gadget, ruler and shape tools and a terrain smoother.'),
    ('ls_gravitics', 'Gravitics', 'GEAR', 5, 'Gravity gun that picks up blocks and creatures, anti-grav plates, gravity lifts and magnetic boots.'),
    ('ls_mechs', 'Mechs', 'GEAR', 5, 'Pilotable mech suits (worker, mining, combat), a mech bay, mech weapons and fuel cores.'),
    ('ls_visors', 'Visors', 'GEAR', 4, 'Visors with night, thermal (see creatures through walls) and ore-scan vision modes.'),
    ('ls_medbay', 'Medbay', 'GEAR', 6, 'Bandages, splints, medkits, a defibrillator, bleeding and fractures, and a healing station.'),
    ('ls_cybernetics', 'Cybernetics', 'GEAR', 5, 'Implants for eyes, arms, legs and skin installed at a surgery table, powered by your body energy.'),
    ('ls_genetics', 'Genetics', 'GEAR', 5, 'Extract genes from creatures and splice traits (night vision, water breathing, fire resistance) into yourself.'),
    ('ls_nanotech', 'Nanotech', 'GEAR', 8, 'Nanobot swarms that repair gear, heal you and build structures.'),

    # ------------------------------------------------------------------ alchemy & transmutation
    ('ls_transmutation', 'Transmutation', 'ALCHEMY', 1, 'Every item gets an aether value from its recipes; a transmutation tablet with learned knowledge, condensers, collectors and relays, umbral and radiant matter tools and armor, a philosopher\'s stone and rings.'),
    ('ls_alchemy', 'Alchemy', 'ALCHEMY', 4, 'Distillery and alembic, ingredients with properties, custom potions by mixing, potion arrows and bombs.'),
    ('ls_soulcraft', 'Soulcraft', 'ALCHEMY', 4, 'Soul jars that capture creatures, essence extraction, spawner reconfiguration and soul-powered machines.'),
    ('ls_runecraft', 'Runecraft', 'ALCHEMY', 6, 'Runestones with area effects (growth, warding, haste), rune circles and a rune carving bench.'),

    # ------------------------------------------------------------------ exploration
    ('ls_waystones', 'Waystones', 'EXPLORATION', 3, 'Waystones in villages and the wild, a discovery network, warp scrolls and warp stones with XP cost by distance.'),
    ('ls_journal', 'Explorer\'s Journal', 'EXPLORATION', 3, 'Tracks the biomes, structures and creatures you discover with completion rewards, a biome compass and a structure compass.'),
    ('ls_cartography', 'Cartography', 'EXPLORATION', 6, 'Atlas books, a map-making table that scales and merges maps, survey markers and map copies.'),
    ('ls_ruins', 'Ancient Ruins', 'EXPLORATION', 3, 'Overgrown city ruins with glyph-locked vaults, an ancient language you decipher, lore tablets and ancient mechanisms.'),
    ('ls_dungeons', 'Dungeoneering', 'EXPLORATION', 3, 'Procedurally assembled multi-room dungeons with traps, keys and locked doors, mini-bosses and loot tiers.'),
    ('ls_spires', 'Spires', 'EXPLORATION', 4, 'Tall trial towers whose floors hold parkour, combat and puzzle challenges with rewards per floor.'),
    ('ls_labyrinth', 'Labyrinth', 'EXPLORATION', 4, 'Algorithmically generated underground mazes with a minotaur, maze maps and a thread spool to mark your path.'),
    ('ls_precursors', 'Precursors', 'EXPLORATION', 3, 'Buried high-tech vaults of an ancient civilization with sentinel guardians, energy cores, plasma tools and terminals.'),
    ('ls_crashsites', 'Crash Sites', 'EXPLORATION', 4, 'Alien crash sites with alien tech to salvage, escape pods, alien creatures and data pads to decrypt.'),
    ('ls_volcanism', 'Volcanism', 'EXPLORATION', 3, 'Volcanoes with lava lakes and eruption events, volcanic bombs, pumice and sulfur, and magma creatures.'),
    ('ls_glacier', 'Glacier', 'EXPLORATION', 4, 'Ice caves, frozen expedition camps, blizzards, frostbite and ice fishing.'),
    ('ls_dunes', 'Dunes', 'EXPLORATION', 4, 'Sandstorms, oases, buried tombs revealed by digging, scarab swarms and desert caravans.'),
    ('ls_bogland', 'Bogland', 'EXPLORATION', 4, 'Bog iron, peat fuel, will-o\'-wisps that lead to treasure, sunken huts and swamp gas.'),
    ('ls_goldrush', 'Gold Rush', 'EXPLORATION', 3, 'Gold panning in rivers, sluice boxes, western mining towns, mine shafts with carts and outlaws.'),
    ('ls_highseas', 'High Seas', 'EXPLORATION', 4, 'Pirate ships, pirate crews, cannons, treasure islands and a kraken.'),
    ('ls_underdark', 'Underdark', 'EXPLORATION', 3, 'Cave features (luminous fungi, crystal grottos, underground lakes), falling stalactites, cave creatures and spelunking gear.'),
    ('ls_highlands', 'Highlands', 'EXPLORATION', 4, 'Alpine features, avalanches, mountain shrines, eagles, summit beacons and altitude sickness.'),
    ('ls_archaeology', 'Archaeology', 'EXPLORATION', 3, 'Dig sites with layered suspicious blocks, artifact sets, a restoration bench and a museum with collection rewards.'),
    ('ls_treasure', 'Treasure Hunter', 'EXPLORATION', 3, 'Riddle treasure maps, a metal detector, buried caches, pirate coves and treasure goblins.'),
    ('ls_settlements', 'Settlements', 'EXPLORATION', 4, 'New settlements (trading post, mining camp, research outpost), new villager professions and reputation.'),
    ('ls_caravans', 'Caravans', 'EXPLORATION', 4, 'Travelling trader caravans with pack animals, rare goods and escort missions.'),
    ('ls_bounties', 'Bounties', 'EXPLORATION', 3, 'Bounty boards with hunt, gather and explore contracts, reputation tiers and rewards.'),
    ('ls_commerce', 'Commerce', 'EXPLORATION', 4, 'Coins, a minting press, vending machines, player shops and a bank vault.'),
    ('ls_celestial', 'Celestial', 'EXPLORATION', 4, 'Meteor showers, blood moons, aurora nights and eclipses, plus an observatory and star charts.'),
    ('ls_relics', 'Relics', 'EXPLORATION', 4, 'Legendary artifacts hidden in structures, each with an active power and lore, and relic pedestals.'),
    ('ls_outposts', 'Frontier Outposts', 'EXPLORATION', 3, 'Abandoned research stations and bunkers with power puzzles, logs and salvageable machines.'),
    ('ls_crypts', 'Crypts', 'EXPLORATION', 4, 'Catacombs, graveyards with night fog, necromancers, ghosts, crypt keys and holy water.'),
    ('ls_derelict', 'Derelict Industry', 'EXPLORATION', 4, 'Abandoned factories with rogue automatons, turrets, scrap, power-restore puzzles and a factory boss.'),
    ('ls_temples', 'Jungle Temples', 'EXPLORATION', 4, 'Overhauled temples with dart traps, rolling boulders and idol puzzles.'),
    ('ls_expeditions', 'Expeditions', 'EXPLORATION', 6, 'Hire explorers at an expedition table, send them out with supplies, and they return over time with loot and maps.'),
    ('ls_sunkencity', 'Sunken City', 'EXPLORATION', 5, 'Drowned city ruins under the ocean with guardians, tidal machines and trident tech.'),
    ('ls_dwarven', 'Dwarven Halls', 'EXPLORATION', 4, 'Abandoned dwarven halls with forges, mine rails, rune doors and hoards.'),
    ('ls_shrines', 'Shrines', 'EXPLORATION', 4, 'Small shrines across the world that grant blessings for offerings, and a shrine map.'),
    ('ls_puzzles', 'Puzzle Chambers', 'EXPLORATION', 5, 'Hidden chambers with light puzzles, block-pushing puzzles and combination locks guarding rewards.'),
    ('ls_trophies', 'Trophies', 'EXPLORATION', 6, 'Mount creature trophies and heads; a trophy room grants bonuses for its collection.'),

    # ------------------------------------------------------------------ nature
    ('ls_flora', 'Flora', 'NATURE', 4, 'Thirty plants and flowers placed by biome, flower dyes, herb gathering and a pressed-flower collection.'),
    ('ls_mycology', 'Mycology', 'NATURE', 5, 'Mushroom cultivation on logs, giant mushrooms and spore effects.'),
    ('ls_wildlife', 'Wildlife', 'NATURE', 3, 'Deer, boar, bears, moose and birds with herds and flight behaviours, tracks to follow, hides and venison.'),
    ('ls_aviary', 'Aviary', 'NATURE', 6, 'Birds you can spot with binoculars, a bird guide to complete, nests and feeders.'),
    ('ls_apiary', 'Apiary', 'NATURE', 5, 'Bee species with traits, breeding and mutations, apiaries, frames, honey products and an analyzer.'),
    ('ls_marine', 'Marine Life', 'NATURE', 5, 'Fish, whales, rays and sea turtles, coral types and kelp forests.'),
    ('ls_meteorology', 'Meteorology', 'NATURE', 5, 'Weather station forecasts, fog, hail, heatwaves, rain gauges and a weather controller.'),
    ('ls_seasons', 'Seasons', 'NATURE', 6, 'Four seasons that change crop growth, foliage color, temperature and snowfall.'),
    ('ls_biomes', 'Biomes', 'NATURE', 4, 'New overworld biomes woven into the vanilla biome layout.'),
    ('ls_netherworks', 'Netherworks', 'NATURE', 4, 'New nether biomes, nether creatures, expanded fortresses and soul-fire tech.'),
    ('ls_endreach', 'End Reach', 'NATURE', 5, 'End island features and structures, voidtouched creatures and chorus tech.'),
    ('ls_slimetech', 'Slimetech', 'NATURE', 6, 'Slime breeding, slime launchers, bouncy blocks and slime farms.'),

    # ------------------------------------------------------------------ creatures & adventure
    ('ls_nightfall', 'Nightfall', 'CREATURES', 3, 'Biome-specific hostile creatures with special abilities and elite variants with random affixes.'),
    ('ls_titans', 'Titans', 'CREATURES', 4, 'Six progression bosses, each with an arena, a summoning ritual, multi-phase fights and unique drops.'),
    ('ls_golemancy', 'Golemancy', 'CREATURES', 5, 'Custom golems built from material blocks with guard, farm, sort and haul behaviours and upgrades.'),
    ('ls_companions', 'Companions', 'CREATURES', 5, 'Robot companions that follow, fight, carry and mine, with upgrade chips and a dock.'),
    ('ls_wyverns', 'Wyvern Riders', 'CREATURES', 5, 'Wyvern nests and eggs, incubation, taming, rideable flying wyverns, saddles and armor.'),
    ('ls_stables', 'Stables', 'CREATURES', 6, 'Horse breeds with genetics, saddlebags, mount stamina and new mounts.'),
    ('ls_provinggrounds', 'Proving Grounds', 'CREATURES', 4, 'Arenas with wave battles, difficulty tiers, rewards and a leaderboard.'),
    ('ls_marauders', 'Marauders', 'CREATURES', 4, 'Outlaw camps, raids on villages and bases, bounty hunters and a marauder warlord.'),
    ('ls_pets', 'Pets', 'CREATURES', 6, 'Small pets that give perks, pet beds and name tags.'),

    # ------------------------------------------------------------------ dimensions
    ('ls_depths', 'The Depths', 'DIMENSIONS', 3, 'A cave dimension with glowing biomes, deep ores, depth creatures and the Deep Warden boss.'),
    ('ls_skylands', 'The Skylands', 'DIMENSIONS', 3, 'A floating-island dimension with sky creatures, wind currents, sky ores and the Tempest boss.'),
    ('ls_everdusk', 'Everdusk', 'DIMENSIONS', 5, 'A twilight forest dimension with progression bosses and locked areas.'),
    ('ls_wasteland', 'The Wasteland', 'DIMENSIONS', 5, 'An irradiated ruined world with scavenging, mutants and old tech.'),
    ('ls_dreamscape', 'Dreamscape', 'DIMENSIONS', 6, 'A dream dimension reached through dream beds, with surreal biomes and nightmares.'),
    ('ls_pocketrooms', 'Pocket Rooms', 'DIMENSIONS', 4, 'Personal rooms inside a block in a pocket dimension, with room upgrades.'),
    ('ls_mineworld', 'Mining World', 'DIMENSIONS', 4, 'An ore-rich resource dimension that can be reset.'),
    ('ls_gateways', 'Gateways', 'DIMENSIONS', 4, 'Custom-size portal frames dialed by address, forming a gateway network.'),
    ('ls_abyss', 'The Abyss', 'DIMENSIONS', 6, 'A deep ocean dimension with hydrothermal vents and leviathans.'),

    # ------------------------------------------------------------------ space
    ('ls_rocketry', 'Rocketry', 'SPACE', 5, 'Rocket tiers assembled on launch pads, fuels, a navigation computer for destinations and landers.'),
    ('ls_orbital', 'Orbital', 'SPACE', 5, 'A space station dimension: station construction, zero gravity, stronger solar and docking.'),
    ('ls_luna', 'Luna', 'SPACE', 5, 'The Moon: low gravity, regolith, helium-3, lunar ruins and moon creatures.'),
    ('ls_mars', 'Mars', 'SPACE', 5, 'The red planet: dust storms, titanium, Martian ruins and creatures.'),
    ('ls_venus', 'Venus', 'SPACE', 6, 'Acid clouds, sulfur lakes and floating platforms.'),
    ('ls_europa', 'Europa', 'SPACE', 6, 'An ice moon with a subsurface ocean.'),
    ('ls_titan', 'Titan', 'SPACE', 6, 'Methane lakes and hydrocarbon dunes.'),
    ('ls_mercury', 'Mercury', 'SPACE', 6, 'Scorching days and freezing nights.'),
    ('ls_asteroids', 'Asteroid Belt', 'SPACE', 6, 'Floating asteroids full of rare ores.'),
    ('ls_astronomy', 'Astronomy', 'SPACE', 5, 'Telescopes and star charts; discovering planets unlocks rocket destinations.'),
    ('ls_rovers', 'Rovers', 'SPACE', 6, 'Drivable planetary rovers with drill and scanner modules.'),
    ('ls_xenobiology', 'Xenobiology', 'SPACE', 6, 'Alien life samples, alien plants grown in labs and xenoceutical potions.'),
    ('ls_satellites', 'Satellites', 'SPACE', 6, 'Launch satellites for map scanning, ore scanning, weather control and orbital strikes.'),

    # ------------------------------------------------------------------ farming & food
    ('ls_harvest', 'Harvest', 'FARMING', 4, 'Twenty-five crops including trellis crops and paddy rice, crop quality tiers and seed bags.'),
    ('ls_kitchen', 'Kitchen', 'FARMING', 4, 'Cutting board, stove, pots and pans, multi-step recipes and sixty meals with buffs.'),
    ('ls_brewery', 'Brewery', 'FARMING', 6, 'Aging barrels, mash tun, fermentation and distillery, drinks with effects and quality.'),
    ('ls_hydroponics', 'Hydroponics', 'FARMING', 5, 'Hydroponic beds, grow lights, nutrient solution and crop cloning.'),
    ('ls_husbandry', 'Husbandry', 'FARMING', 6, 'Animal genetics and traits, troughs and feeders, animal happiness and product quality.'),
    ('ls_soilcraft', 'Soilcraft', 'FARMING', 6, 'Soil fertility, compost bins, worm farms, fertilizers and mulch.'),
    ('ls_preserves', 'Preserves', 'FARMING', 6, 'Food spoilage with drying racks, smokers, pickling jars, ice boxes and fridges.'),
    ('ls_creamery', 'Creamery', 'FARMING', 7, 'Butter churns, cheese presses and aging cheese wheels.'),

    # ------------------------------------------------------------------ building
    ('ls_catwalks', 'Catwalks', 'BUILDING', 4, 'Auto-connecting catwalks, railings, caged ladders, industrial stairs and scaffolding.'),
    ('ls_construction', 'Construction', 'BUILDING', 5, 'Concrete mixer, rebar and reinforced concrete, asphalt roads that speed you up and construction foam.'),
    ('ls_furnishings', 'Furnishings', 'BUILDING', 6, 'Chairs you can sit on, tables, cabinets with storage, shelves that display items and sofas.'),
    ('ls_illumination', 'Illumination', 'BUILDING', 5, 'Colored lamps, spotlights that light distant blocks, floodlights and light strips.'),
    ('ls_lifts', 'Lifts', 'BUILDING', 5, 'Elevator shafts with moving platforms, call buttons, escalators and freight elevators.'),
    ('ls_doors', 'Doors & Gates', 'BUILDING', 6, 'Sliding doors, garage doors, drawbridges and portcullises.'),
    ('ls_masonry', 'Masonry', 'BUILDING', 6, 'A chisel with hundreds of block variants, mosaics, pillars and arches.'),
    ('ls_printing', 'Printing Press', 'BUILDING', 7, 'Paper mill, a printing press that copies books and maps, posters and wallpaper.'),
    ('ls_timber', 'Timber', 'BUILDING', 5, 'Whole-tree felling, a lumber mill, beams and frames and timber framing.'),
    ('ls_aqueducts', 'Aqueducts', 'BUILDING', 6, 'Aqueducts that carry water across distance, wells, fountains and rain barrels.'),
    ('ls_beacons', 'Beacons', 'BUILDING', 6, 'Beacon upgrades with new effects, range extenders and effect towers.'),

    # ------------------------------------------------------------------ transport
    ('ls_airships', 'Airships', 'TRANSPORT', 5, 'Airships with propellers, balloons, steering, cargo and fuel.'),
    ('ls_motorworks', 'Motor Works', 'TRANSPORT', 6, 'Cars and trucks with fuel, a garage and trailers.'),
    ('ls_shipwright', 'Shipwright', 'TRANSPORT', 6, 'Motorboats, sailboats driven by wind and barges with storage.'),
    ('ls_hoverbikes', 'Hoverbikes', 'TRANSPORT', 6, 'Hoverbikes for fast travel over any terrain.'),
    ('ls_balloons', 'Ballooning', 'TRANSPORT', 5, 'Hot-air balloons for early flight and exploration.'),

    # ------------------------------------------------------------------ endgame & misc
    ('ls_quantum', 'Quantum', 'INDUSTRY', 8, 'Quantum entanglement links across dimensions, quantum armor and a singularity compressor.'),
    ('ls_omega', 'Omega', 'INDUSTRY', 8, 'Endgame: an omega crafting table with a large grid, omega alloy and ultimate tools.'),
    ('ls_deepmining', 'Deep Mining', 'MATERIALS', 5, 'Cave-ins without supports, methane pockets, mine carts, miner\'s lamps and ventilation.'),
    ('ls_plasma', 'Plasma', 'INDUSTRY', 7, 'Plasma torches that cut blocks, plasma cannons and plasma containment.'),
    ('ls_villagerworks', 'Villager Works', 'EXPLORATION', 7, 'Villager transport, trading halls, breeding pens and job assignment.'),
    ('ls_photography', 'Photography', 'GEAR', 7, 'A camera that takes real pictures of the world, printed photos, frames and albums.'),
    ('ls_seismic', 'Seismology', 'NATURE', 7, 'Earthquakes along fault lines, a seismograph that warns of them and quake-proof foundations.'),
    ('ls_tempest', 'Storm Chasers', 'NATURE', 7, 'Tornadoes and supercell storms, storm essence harvesting and storm shelters.'),
    ('ls_claims', 'Land Claims', 'CORE', 7, 'Survey stakes and land deeds to claim chunks, trusted players and claim flags.'),
    ('ls_garrison', 'Garrison', 'CREATURES', 7, 'Hire guards for villages and bases, barracks, patrol routes and guard gear.'),
    ('ls_colony', 'Colony', 'CREATURES', 8, 'Colonists who live at your base and work jobs: builder, miner, farmer, smith and courier.'),
    ('ls_locksmith', 'Locksmith', 'EXPLORATION', 4, 'Keys and locks for chests and doors, and a lockpicking minigame for locked dungeon chests.'),
    ('ls_trapsmith', 'Trapsmith', 'TOOLS', 6, 'Spike pits, bear traps, flame jets, net launchers and tripwire triggers for base defense.'),
    ('ls_stealth', 'Stealth', 'GEAR', 7, 'Noise and visibility meters, sneaking bonuses, smoke bombs and a camouflage cloak.'),
    ('ls_parkour', 'Parkour', 'GEAR', 4, 'Wall jumps, ledge climbing, slides and vaults.'),
    ('ls_artillery', 'Artillery', 'TOOLS', 6, 'Mortars, cannons and ballistas that fire along real trajectories, with aiming and shell types.'),
    ('ls_airdrops', 'Airdrops', 'EXPLORATION', 5, 'Supply drops parachute down as world events with flares to call them, guarded by creatures.'),
    ('ls_aquaculture', 'Aquaculture', 'FARMING', 6, 'Fish ponds, fish feed, oyster beds and shrimp traps.'),
    ('ls_toolbelt', 'Toolbelt', 'GEAR', 4, 'A tool belt with a radial quick-swap menu and portable crafting, anvil and smithing tables.'),
    ('ls_portaldevice', 'Portal Device', 'GEAR', 6, 'A handheld device that fires a linked pair of portals onto surfaces.'),
    ('ls_powertools', 'Power Tools', 'TOOLS', 4, 'Chainsaw that fells whole trees, electric drill, angle grinder and jackhammer with battery packs.'),
    ('ls_railworks', 'Railworks', 'TRANSPORT', 5, 'High-speed rails, boosters, junction switches, track layers and powered carts.'),
    ('ls_skydiving', 'Skydiving', 'TRANSPORT', 6, 'Parachutes, wingsuits and orbital drop pods.'),
    ('ls_wagons', 'Wagons', 'TRANSPORT', 6, 'Horse-drawn carts and wagons with storage.'),
    ('ls_sonics', 'Sonics', 'AUTOMATION', 6, 'Sonar pings that reveal caves and ores, sonic emitters and ultrasonic creature repellers.'),
    ('ls_cafe', 'Tea & Coffee', 'FARMING', 7, 'Tea and coffee plantations, roasting and brewing, and caffeinated buffs.'),
    ('ls_pyrotechnics', 'Pyrotechnics', 'GEAR', 7, 'A firework designer, flare guns and smoke signals.'),
    ('ls_rifts', 'Rifts', 'EXPLORATION', 5, 'Dimensional rifts open in the world spawning otherworldly creatures; closing them yields rift shards for tech.'),
    ('ls_blight', 'Blight', 'NATURE', 6, 'A spreading corruption you must contain with purifiers and wards, and that rewards you for cleansing it.'),
    ('ls_cloning', 'Cloning', 'GEAR', 6, 'Clone pods that grow backup bodies so you respawn at your base with a spare set of gear.'),
    ('ls_corerig', 'Core Rigs', 'INDUSTRY', 5, 'Deep core drilling rigs that pull ores from far below bedrock, with drill heads and ore focus lenses.'),
    ('ls_missiles', 'Missiles', 'AUTOMATION', 7, 'Radar stations, guided missiles, launch silos and anti-missile defense.'),
    ('ls_canopy', 'Ancient Canopy', 'EXPLORATION', 5, 'Colossal ancient trees with canopy villages, rope bridges and canopy creatures.'),
    ('ls_aerie', 'Aerie', 'EXPLORATION', 5, 'Floating islands in the overworld sky with sky shrines and nesting griffins.'),
    ('ls_heatworks', 'Heatworks', 'POWER', 5, 'Heat as a resource: heat pipes, radiators, heat sinks and machines that run on heat instead of energy.'),
    ('ls_bestiary', 'Bestiary', 'CREATURES', 4, 'Study creatures to fill a bestiary that reveals weaknesses and grants damage bonuses against studied creatures.'),
]


def counted():
    return [p for p in P if p[0] != 'ls_core']


def done_ids(repo):
    mods_dir = repo / 'tools' / 'mods'
    return {p[0] for p in P if (mods_dir / (p[0][3:] + '.py')).exists()}


def markdown(repo):
    done = done_ids(repo)
    themed = counted()
    out = ['# Lodestar plan', '', '%d themed mods planned (plus the shared engine). %d built so far.' %
           (len(themed), len([p for p in themed if p[0] in done])), '']
    for key, title in TABS:
        group = [p for p in P if p[2] == key]
        if not group:
            continue
        out += ['## %s (%d)' % (title, len(group)), '', '| Mod | ID | Wave | Status | What it does |', '|---|---|---|---|---|']
        for (mid, name, _tab, wave, pitch) in group:
            out.append('| %s | `%s` | %d | %s | %s |' % (name, mid, wave, 'built' if mid in done else 'planned', pitch))
        out.append('')
    return '\n'.join(out) + '\n'


if __name__ == '__main__':
    ids = [p[0] for p in P]
    dup = {i for i in ids if ids.count(i) > 1}
    assert not dup, dup
    root = Path(__file__).resolve().parents[1]
    themed = counted()
    waves = {}
    for p in themed:
        waves[p[3]] = waves.get(p[3], 0) + 1
    print('themed mods:', len(themed), 'by wave:', dict(sorted(waves.items())))
    if '--md' in sys.argv:
        (root / 'PLAN.md').write_text(markdown(root))
