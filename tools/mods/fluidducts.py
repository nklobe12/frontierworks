from gen.fw import Mod, connector_block, pipe_tex, pipe_end, const

DUCTS = [
    ('fluid_duct', 0, '#b87333', '#6e4524'),
    ('pump_duct', 200, '#b87333', '#2980b9'),
    ('high_flow_pump_duct', 1000, '#7b8591', '#16a085'),
    ('elite_pump_duct', 5000, '#3d3f48', '#9b5de5'),
]


def build():
    m = Mod('fw_fluidducts', 'Fluid Ducts', 'Move water, lava, oil, steam and every other fluid: pump ducts drain adjacent tanks '
                                            'and machines and fill everything else on the network.', tab='LOGISTICS')
    rl = m.rl
    for name, rate, body, band in DUCTS:
        C = const(name)
        connector_block(m, name, 'new DuctBlock(FWProps.metal(0.8f).noOcclusion(), %d, () -> Content.%s_BE.get())' % (rate, C), 'DuctBlock', 3,
                        pipe_tex(body, band, name, glass=True), pipe_end(body, band, name), render='translucent')
        m.field('public static final RegistryObject<BlockEntityType<DuctBE>> %s_BE = REG.blockEntity("%s", (pos, state) -> new DuctBE(Content.%s_BE.get(), pos, state), Content.%s);'
                % (C, name, C, C))
    m.shaped('fluid_duct', rl('fluid_duct'), ['PGP'], {'P': '#forge:plates/copper', 'G': '#forge:glass'}, 8)
    m.shaped('pump_duct', rl('pump_duct'), [' R ', 'BDB'], {'R': '#forge:dusts/redstone', 'B': 'minecraft:bucket', 'D': rl('fluid_duct')}, 2)
    m.shaped('high_flow_pump_duct', rl('high_flow_pump_duct'), ['SPS'], {'S': '#forge:plates/steel', 'P': rl('pump_duct')})
    m.shaped('elite_pump_duct', rl('elite_pump_duct'), ['CPC'], {'C': 'fw_circuits:advanced_circuit', 'P': rl('high_flow_pump_duct')})
    m.tr('tooltip.fw_fluidducts.pump', 'Pumps up to %s mB/t out of adjacent tanks. Redstone pauses.')
    m.tr('tooltip.fw_fluidducts.duct', 'Carries fluids between pump ducts and tanks')
    return m
