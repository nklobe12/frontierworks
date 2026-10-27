from gen.fw import Mod, custom_model_machine
from gen import tex

TIERS = [
    # name, peak FE/t, night factor, casing tier, cell colour
    ('solar_panel_basic', 8, 0.0, 'basic', '#1f3a6b'),
    ('solar_panel_advanced', 32, 0.0, 'advanced', '#1a2f5c'),
    ('solar_panel_hybrid', 128, 0.25, 'elite', '#2b1f5c'),
    ('solar_panel_ultimate', 512, 0.5, 'ultimate', '#3a1050'),
]


def solar_top(color, seed):
    img = tex.blank(tex.rgb(color) + (255,))
    px = img.load()
    c = tex.rgb(color)
    for y in range(16):
        for x in range(16):
            if x % 5 == 0 or y % 5 == 0:
                px[x, y] = (190, 196, 204, 255)
            else:
                f = 1.0 + 0.3 * ((x + y) % 5 == 1)
                px[x, y] = (tex.clamp(c[0] * f), tex.clamp(c[1] * f), tex.clamp(c[2] * f + 20), 255)
    for i in range(16):
        px[i, 0] = px[0, i] = (120, 124, 130, 255)
        px[i, 15] = px[15, i] = (80, 84, 90, 255)
    return img


def build():
    m = Mod('fw_solar', 'Solar Power', 'Four tiers of solar panels, from an 8 FE/t starter panel to the 512 FE/t ultimate array. '
                                       'Hybrid and ultimate panels keep producing at night.', tab='MACHINES')
    rl = m.rl
    prev = None
    for name, peak, night, tier, color in TIERS:
        c = tex.casing(tier, m.modid + tier, 'side')
        textures = {'block/%s_top' % name: solar_top(color, name), 'block/%s_side' % name: c,
                    'block/%s_bottom' % name: tex.casing(tier, m.modid + tier, 'bottom')}
        night_txt = ' Works at %d%% at night.' % int(night * 100) if night else ''
        custom_model_machine(m, name, 'new SolarPanelBE(BE_TYPE, pos, state, %d, %sf)' % (peak, night), 'SolarPanelBE', textures,
                             top=name + '_top', side=name + '_side', bottom=name + '_bottom', desc='Up to %d FE/t in sunlight.%s' % (peak, night_txt))
        if prev is None:
            m.shaped(name, rl(name), ['CCC', 'PWP', 'PRP'], {'C': 'fw_circuits:solar_cell', 'P': '#forge:plates/iron', 'W': '#forge:wires/copper',
                                                             'R': '#forge:dusts/redstone'})
        else:
            key = {'basic': None, 'advanced': 'fw_circuits:advanced_circuit', 'elite': 'fw_circuits:elite_circuit', 'ultimate': 'fw_circuits:quantum_circuit'}[tier]
            m.shaped(name, rl(name), ['SSS', 'SCS', 'SSS'], {'S': rl(prev), 'C': key})
        prev = name
    m.tr('gui.fw_solar.peak', 'Peak: %s FE/t')
    return m
