# Frontierworks

A 114-mod tech, exploration and adventure modpack for **Minecraft 1.20.1** with **Forge 47.3.0**. Every mod in it was written from scratch for this pack. The full list with descriptions is in [MODS.md](MODS.md), and the in-game **Field Guide** (handed to you on first join) has a page for each mod.

## What's in it

- **Tech**: new metals, alloys, gems and minerals. Kilns, then powered machines (pulverizer, compressor, alloy smelter, centrifuge, ore washer, electrolyzer, refinery and more). Power from combustion, steam, solar, wind, water, geothermal, fission and fusion. Batteries, cables and wireless power, item pipes, fluid ducts, tanks, crates, conveyors, a quarry, pumps, farms, mob grinders, teleporters, elevators and chunk loaders.
- **Gear**: tools and armor in 13 materials, hammers, drills, spears, daggers, warhammers, battleaxes, energy blasters, jetpacks, a grappling hook, a glider, backpacks, magnets, an ore scanner and explosives.
- **Exploration**: waystones, treasure maps, ruins, dungeons, towers, sky islands, outposts, bunkers, temples, camps and meteor craters. New trees, flowers, glowing caves and villager professions, plus two dimensions, **the Depths** (a vast cave world) and **the Skylands** (floating islands).
- **Adventure**: new creatures (deer, boars, rust beetles, cave crawlers, scrap drones, bog lurkers, sand mummies, frost wraiths, ember imps) and three bosses (the Iron Colossus, the Wraith King and the Sky Wyvern). Also relics, heart containers, enchantments, potions, frontier cooking, coins, a bounty board and trial altars.
- **Space**: rockets, a space suit with oxygen, oxygen generators, **the Moon** (helium-3) and **Mars** (titanium).

## Installing

Download from the [latest release](../../releases/latest):

| File | Use it with |
|---|---|
| `Frontierworks-<version>-curseforge.zip` | CurseForge app: *Create Custom Profile → Import* |
| `Frontierworks-<version>-prism.zip` | Prism Launcher / MultiMC: *Add Instance → Import* |
| `Frontierworks-<version>.mrpack` | Modrinth App: *Add instance → Import from file* |
| `Frontierworks-<version>-server.zip` | Dedicated server: unzip and run `start.sh` / `start.bat` |
| `Frontierworks-<version>-mods.zip` | Any Forge 1.20.1 install: copy `mods/` into your game folder |

Give the game at least 6 GB of RAM.

## Building from source

Requires JDK 17 and Python 3 with Pillow (`pip install pillow`).

```bash
python3 tools/build.py      # generates Java registration code, models, textures, recipes and worldgen from tools/mods/*.py
./gradlew modJars           # compiles everything once, then splits it into one jar per mod in build/modjars/
python3 tools/package.py 47.3.0 build/modjars dist   # builds the modpack zips
```

Each mod lives in `mods/<modid>/`: hand-written Java in `src/`, generated Java in `gen/`, and generated plus static resources in `resources/`. The specs in `tools/mods/` describe each mod's items, blocks, recipes, worldgen and structures. The generator in `tools/gen/` turns those specs into code and assets, including all textures and the structure NBT files.

GitHub Actions compiles every push, boots a real Forge server with every mod (placing every block and structure, generating every dimension and spawning every entity), boots the client, takes showcase screenshots and publishes the modpack files to the release.
