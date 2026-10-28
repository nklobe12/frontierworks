"""Field Codex: the in-game manual. Reads every mod's generated codex/chapter.json on the client."""
import math
import random

from PIL import Image, ImageDraw

from gen.ls import Mod
from gen import tex
from plan import TABS


# ----------------------------------------------------------------------------- textures

def tileable_noise(size, seed, cells=8, octaves=3):
    """Periodic value noise in [0, 1] (wraps at the edges, so GUI backgrounds tile seamlessly)."""
    rnd = random.Random(seed)
    out = [[0.0] * size for _ in range(size)]
    amp, total = 1.0, 0.0
    for o in range(octaves):
        n = cells * (2 ** o)
        grid = [[rnd.random() for _ in range(n)] for _ in range(n)]
        for y in range(size):
            for x in range(size):
                fx, fy = x * n / size, y * n / size
                x0, y0 = int(fx) % n, int(fy) % n
                x1, y1 = (x0 + 1) % n, (y0 + 1) % n
                tx, ty = fx - int(fx), fy - int(fy)
                tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
                a = grid[y0][x0] * (1 - tx) + grid[y0][x1] * tx
                b = grid[y1][x0] * (1 - tx) + grid[y1][x1] * tx
                out[y][x] += (a * (1 - ty) + b * ty) * amp
        total += amp
        amp *= 0.5
    return [[v / total for v in row] for row in out]


def parchment(seed, size=64, base='#efe2c2'):
    c = tex.rgb(base)
    n = tileable_noise(size, seed, 4, 4)
    fib = tileable_noise(size, seed + 'f', 16, 2)
    img = Image.new('RGBA', (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            v = 0.93 + 0.1 * n[y][x] + (0.03 if fib[y][x] > 0.68 else 0) - (0.025 if fib[y][x] < 0.3 else 0)
            px[x, y] = tuple(max(0, min(255, int(ch * v))) for ch in c) + (255,)
    return img


def leather(seed, size=64, base='#6a3f22'):
    c = tex.rgb(base)
    n = tileable_noise(size, seed, 8, 3)
    grain = tileable_noise(size, seed + 'g', 32, 1)
    img = Image.new('RGBA', (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            v = 0.82 + 0.3 * n[y][x] - (0.12 if grain[y][x] > 0.72 else 0)
            px[x, y] = tuple(max(0, min(255, int(ch * v))) for ch in c) + (255,)
    return img


def compass_rose(d, cx, cy, r, gold, red):
    """Four-pointed star with a red north point."""
    d.polygon([(cx, cy - r), (cx + 1, cy), (cx, cy + 1), (cx - 1, cy)], fill=red)
    d.polygon([(cx, cy + r), (cx + 1, cy), (cx, cy - 1), (cx - 1, cy)], fill=gold)
    d.polygon([(cx - r, cy), (cx, cy - 1), (cx + 1, cy), (cx, cy + 1)], fill=gold)
    d.polygon([(cx + r, cy), (cx, cy - 1), (cx - 1, cy), (cx, cy + 1)], fill=gold)


def book_item(cover, trim, emblem, seed, ribbon='#b03a2e', pages='#efe4c8'):
    """A closed hardback seen from the front: spine on the left, page edges on the right, corner caps and an emblem."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    cv, tr, rb, pg = tex.rgb(cover), tex.rgb(trim), tex.rgb(ribbon), tex.rgb(pages)
    outline = tex.shade(cv, 0.45)
    # page block (behind the cover on the right and bottom)
    for y in range(2, 15):
        for x in range(12, 15):
            px[x, y] = tex.shade(pg, 0.92 if (y % 2) else 1.0)
    for x in range(3, 15):
        px[x, 14] = tex.shade(pg, 0.85)
    # cover with leather noise
    for y in range(1, 14):
        for x in range(2, 13):
            v = 0.9 + 0.2 * tex.rnd(seed, x, y)
            px[x, y] = tex.shade(cv, v)
    # spine: darker band with two raised bands
    for y in range(1, 15):
        px[1, y] = outline
        px[2, y] = tex.shade(cv, 0.7)
        px[3, y] = tex.shade(cv, 0.82)
    for y in (3, 11):
        px[2, y] = tex.shade(tr, 0.9)
        px[3, y] = tex.shade(tr, 0.75)
    # outline
    for x in range(2, 13):
        px[x, 0] = outline
        px[x, 14] = outline if x < 4 else px[x, 14]
    for y in range(0, 14):
        px[13, y] = outline if y < 2 else px[13, y]
    for y in range(2, 15):
        px[15, y] = tex.shade(pg, 0.55)
    for x in range(3, 15):
        px[x, 15] = tex.shade(pg, 0.5)
    px[1, 0] = (0, 0, 0, 0)
    # corner caps in trim metal
    for (x, y) in ((11, 1), (12, 1), (12, 2), (11, 13), (12, 13), (12, 12)):
        px[x, y] = tex.shade(tr, 1.1 if y < 3 else 0.85)
    # emblem
    d = ImageDraw.Draw(img)
    emblem(d, img)
    # ribbon hanging out of the bottom
    px[9, 14] = tex.shade(rb, 1.0)
    px[9, 15] = tex.shade(rb, 0.8)
    px[10, 15] = tex.shade(rb, 0.65)
    return img


def codex_emblem(d, img):
    """Compass rose: gold cardinal points with a red north needle and four ring studs."""
    px = img.load()
    gold, dark, red = tex.rgb('#e8c25a'), tex.rgb('#a4782a'), tex.rgb('#d0402f')
    cx, cy = 7, 7
    for i in range(1, 4):
        px[cx, cy - i] = tex.shade(red, 1.1 - 0.1 * i)
        px[cx, cy + i] = tex.shade(gold, 1.0 - 0.08 * i)
        px[cx - i, cy] = tex.shade(gold, 1.0 - 0.08 * i)
        px[cx + i, cy] = tex.shade(gold, 0.9 - 0.08 * i)
    for (x, y) in ((cx - 1, cy - 1), (cx + 1, cy - 1), (cx - 1, cy + 1), (cx + 1, cy + 1)):
        px[x, y] = tex.shade(dark, 1.0)
    for (x, y) in ((cx - 3, cy - 3), (cx + 3, cy - 3), (cx - 3, cy + 3), (cx + 3, cy + 3)):
        px[x, y] = tex.shade(dark, 0.9)
    px[cx, cy] = (255, 244, 200, 255)


def codex_textures(m):
    m.texture('item/field_codex', book_item('#7a4524', '#d9a93c', codex_emblem, m.modid + 'book'))
    m.texture('gui/paper', parchment(m.modid + 'paper'))
    m.texture('gui/leather', leather(m.modid + 'leather'))


# ----------------------------------------------------------------------------- spec

def build():
    m = Mod('ls_codex', 'Field Codex', 'The in-game manual: a chapter for every mod with linked recipes, 3D structure previews, '
                                       'search, item lookup from any inventory and per-world reading progress.', tab='CORE')
    m.init('CodexSetup.init(bus)')
    m.client('com.lodestar.codex.client.CodexClient.init(bus)')
    codex_textures(m)
    m.item('field_codex', None, java='new CodexItem(new Item.Properties().stacksTo(1))', jtype='CodexItem', tab='CORE',
           desc='Right-click to read about everything in Lodestar\nHold [G] over any item in an inventory to open its page')
    m.shapeless('field_codex', m.rl('field_codex'), ['minecraft:book', 'minecraft:compass'])

    for key, name in TABS:
        m.tr('gui.ls_codex.tab.' + key.lower(), name)
    for k, v in {
        'key.ls_codex.lookup': 'Codex: look up item (hold)',
        'gui.ls_codex.title': 'Field Codex',
        'gui.ls_codex.search': 'Search the Codex...',
        'gui.ls_codex.index': 'Index',
        'gui.ls_codex.contents': 'Contents',
        'gui.ls_codex.summary': '%s chapters, %s pages',
        'gui.ls_codex.no_results': 'Nothing in the Codex matches that.',
        'gui.ls_codex.results': '%s results',
        'gui.ls_codex.welcome_title': 'Welcome, Explorer',
        'gui.ls_codex.welcome': 'This Codex describes every Lodestar mod: what its blocks do, how its machines are built and '
                                'what they make. Pick a chapter on the left or search for anything by name.\n\n'
                                'Pages you have not read yet carry a red mark. The Codex also notes which items you have '
                                'carried, so each chapter shows how far you have got.',
        'gui.ls_codex.hint_lookup': 'Hold [%s] over an item in any inventory to open its page. Press it while looking at a block to read about that block.',
        'gui.ls_codex.stat_pages': 'Pages read: %s / %s',
        'gui.ls_codex.stat_items': 'Items carried: %s / %s',
        'gui.ls_codex.stat_chapters': 'Chapters finished: %s / %s',
        'gui.ls_codex.items_held': 'Items carried: %s / %s',
        'gui.ls_codex.page_of': '%s / %s',
        'gui.ls_codex.unread': 'Not read yet',
        'gui.ls_codex.recipe': 'Recipe',
        'gui.ls_codex.recipe_of': 'Recipe %s of %s',
        'gui.ls_codex.no_recipe': 'Found in the world or made in ways the Codex cannot show.',
        'gui.ls_codex.structure': 'Structure',
        'gui.ls_codex.layer': 'Layer %s of %s',
        'gui.ls_codex.all_layers': 'All layers',
        'gui.ls_codex.materials': 'Materials',
        'gui.ls_codex.drag': 'Drag to turn, scroll to zoom',
        'gui.ls_codex.missing_structure': 'Structure preview unavailable.',
        'gui.ls_codex.click_recipes': 'Left-click: recipes',
        'gui.ls_codex.click_uses': 'Right-click: uses',
        'gui.ls_codex.click_show': 'Click: show its recipe here',
        'gui.ls_codex.shift_show': 'Shift-click: show its recipe here',
        'gui.ls_codex.click_page': 'Click: open its Codex page',
        'gui.ls_codex.hold_lookup': 'Hold [%s]: Codex page',
        'gui.ls_codex.not_carried': 'Not carried yet',
        'gui.ls_codex.cat.crafting': 'Crafting',
        'gui.ls_codex.cat.smelting': 'Smelting',
        'gui.ls_codex.cat.blasting': 'Blasting',
        'gui.ls_codex.cat.smoking': 'Smoking',
        'gui.ls_codex.cat.campfire': 'Campfire',
        'gui.ls_codex.cat.stonecutting': 'Stonecutting',
        'gui.ls_codex.cook_time': '%ss, %s XP',
        'gui.ls_codex.shapeless': 'Shapeless',
        'msg.ls_codex.granted': 'You have been given a Field Codex. Right-click it to read about everything in Lodestar.',
        'msg.ls_codex.need_codex': 'You need a Field Codex in your inventory.',
    }.items():
        m.tr(k, v)

    m.codex_intro('The Field Codex is this book. Every Lodestar mod writes a chapter into it: what the mod is for, how its blocks '
                  'and machines work and what they need. Chapters are grouped like the creative tabs, and every item on a page '
                  'links to its recipe.')
    m.codex_page('Reading the Codex', 'Pick a chapter in the index to see its contents, then a page to read it. The arrows at the '
                                      'bottom corners turn pages; the arrow keys do too, and Backspace goes back to where you were.\n\n'
                                      'Pages you have not opened yet in this world carry a red mark, and every chapter counts how '
                                      'many of its items you have carried so far.',
                 items=['ls_codex:field_codex'])
    m.codex_page('Recipes and Items', 'The items under a page\'s text are what it is about. Hover one to see its tooltip. With the '
                                      'Lens recipe viewer, left-click an item to see how it is made and right-click it to see what it '
                                      'is used in; shift-click shows its recipe on the facing page. Click an ingredient of a recipe '
                                      'to jump to the page that describes it.',
                 items=['minecraft:book', 'minecraft:compass'])
    m.codex_page('Looking Things Up', 'Hold the Codex key (G unless you changed it) while pointing at an item in any inventory: a '
                                      'ring fills and the Codex opens at the page about that item. Closing the Codex returns you to '
                                      'the inventory. Press the key while looking at a block in the world to read about that block.\n\n'
                                      'The search box in the index finds chapters, page titles, words in the text and item names.')
    m.codex_page('Structures', 'Pages about multiblock structures show the structure in three dimensions. It turns slowly on its '
                               'own; drag it to turn it yourself and scroll over it to zoom. The slider beside it picks a single layer '
                               'so you can see inside, and the materials list below counts every block you need.')
    m.codex_page("Engineer's Wrench", 'Almost every Lodestar machine responds to the wrench. Right-click a block to rotate it, and '
                                      'sneak-right-click a machine to pick it up with its contents and settings. Wrench a side of a '
                                      'duct to cut or rejoin it.',
                 items=['ls_core:wrench'])
    return m
