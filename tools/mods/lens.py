from gen.ls import Mod


def build():
    m = Mod('ls_lens', 'Lens', 'Recipe viewer: a searchable list of every item beside each inventory, recipes and uses for every '
                               'recipe type in the pack, bookmarks and fill-the-crafting-grid transfer.', tab='CORE')
    m.init('LensSetup.init(bus)')
    m.client('com.lodestar.lens.client.LensClient.init(bus)')
    for k, v in {
        'lens.category.minecraft.crafting': 'Crafting', 'lens.category.minecraft.smelting': 'Smelting',
        'lens.category.minecraft.blasting': 'Blasting', 'lens.category.minecraft.smoking': 'Smoking',
        'lens.category.minecraft.campfire_cooking': 'Campfire Cooking', 'lens.category.minecraft.stonecutting': 'Stonecutting',
        'lens.category.minecraft.smithing': 'Smithing', 'lens.category.minecraft.fuel': 'Fuel',
        'lens.category.minecraft.composting': 'Composting',
        'lens.ls_core.not_consumed': 'Not consumed', 'lens.ls_core.chance': '%s%% chance',
        'lens.ls_core.alternatives': 'Any of %s items',
        'lens.ls_lens.title': 'Lens', 'lens.ls_lens.search': 'Search items', 'lens.ls_lens.search_hint': 'Search (@mod, #tag)',
        'lens.ls_lens.hint': 'Click: recipes  Right-click: uses  A: bookmark',
        'lens.ls_lens.pages': 'Scroll or click to turn pages',
        'lens.ls_lens.recipes_for': 'Recipes for %s', 'lens.ls_lens.uses_of': 'Uses of %s', 'lens.ls_lens.all_recipes': '%s recipes',
        'lens.ls_lens.count': '%s recipes', 'lens.ls_lens.shapeless': 'Shapeless',
        'lens.ls_lens.transfer': 'Put the ingredients in the crafting grid', 'lens.ls_lens.transfer_max': 'Shift-click: as many as possible',
        'lens.ls_lens.burns': 'Smelts %s items (%ss)', 'lens.ls_lens.compost': '%s%% chance to raise the level',
        'key.categories.ls_lens': 'Lens (recipe viewer)', 'key.ls_lens.recipes': 'Show recipes', 'key.ls_lens.uses': 'Show uses',
        'key.ls_lens.bookmark': 'Bookmark item', 'key.ls_lens.toggle': 'Hide or show the item list', 'key.ls_lens.focus': 'Search items',
    }.items():
        m.tr(k, v)
    m.codex_intro('Lens shows how anything is made. Open any inventory: the list of every item sits on the right, your bookmarks on '
                  'the left and a search box at the bottom.')
    m.codex_page('Finding recipes', 'Click an item in the list, or point at any item and press R, to see the recipes that make it. '
                                    'Right-click or press U to see what it is used for. Inside the viewer, click any ingredient to '
                                    'follow it, use the tabs along the top to switch machine types and press Backspace to go back.')
    m.codex_page('Searching', 'Type in the search box to filter the list. Words must all appear in the name; start a word with @ to '
                              'search by mod (@toolsmith) and with # to search by tag (#ingots). Ctrl+F jumps to the search box and '
                              'Ctrl+O hides the list.')
    m.codex_page('Bookmarks and crafting', 'Press A over an item to pin it to the left side of your screen. When you open the viewer '
                                           'from a crafting table or your inventory, crafting recipes get a + button that lays the '
                                           'ingredients out for you; shift-click it to fill as many as you can. In creative mode, '
                                           'shift-click an item in the list to take a stack.')
    return m
