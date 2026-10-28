package com.lodestar.lapidary;

import com.lodestar.core.test.SelfTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.function.Supplier;

/** Functional self-tests: cutting a gem, socketing gear, cluster growth and geode generation. */
final class LapidaryTests {
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    private LapidaryTests() {}

    static void register() {
        SelfTest.scenario("lapidary:cutting", 160, LapidaryTests::cutting);
        SelfTest.scenario("lapidary:socketing", 20, LapidaryTests::socketing);
        SelfTest.scenario("lapidary:clusters", 20, LapidaryTests::clusters);
        SelfTest.scenario("lapidary:geode", 20, LapidaryTests::geode);
    }

    private static Block block(String name) {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation("ls_lapidary", name));
    }

    private static Supplier<String> cutting(ServerLevel l, BlockPos o) {
        l.setBlock(o.below(), Blocks.STONE.defaultBlockState(), 3);
        l.setBlock(o, Content.CUTTING_WHEEL.get().defaultBlockState(), 3);
        CuttingWheelBE be = (CuttingWheelBE) l.getBlockEntity(o);
        if (be == null) return () -> "no cutting wheel block entity";
        be.getEnergy().setEnergy(20000);
        be.setCut(Cut.CABOCHON);
        be.getItems().setStackInSlot(CuttingWheelBE.SLOT_IN, new ItemStack(GemType.RUBY.roughItem(), 3));
        be.getItems().setStackInSlot(CuttingWheelBE.SLOT_GRIT, new ItemStack(Content.GEM_GRIT.get(), 1));
        return () -> {
            int cut = 0, rough = be.getItems().getStackInSlot(CuttingWheelBE.SLOT_IN).getCount();
            StringBuilder qs = new StringBuilder();
            for (int i = 0; i < be.getItems().getSlots(); i++) {
                ItemStack st = be.getItems().getStackInSlot(i);
                if (st.getItem() == GemType.RUBY.cutItem() && GemData.isCut(st)) {
                    cut += st.getCount();
                    qs.append(GemData.cut(st).id()).append('/').append(GemData.quality(st)).append(' ');
                }
            }
            int fe = be.getEnergy().getEnergyStored();
            LOG.info("lapidary:cutting cut rubies={} [{}] rough left={} energy left={} FE", cut, qs.toString().trim(), rough, fe);
            if (cut < 1) return "no cut ruby after 160 ticks (rough left " + rough + ", energy " + fe + ")";
            return fe < 20000 ? null : "the wheel used no energy";
        };
    }

    private static double modifier(ItemStack st, EquipmentSlot slot, net.minecraft.world.entity.ai.attributes.Attribute attr, GemEffect effect) {
        for (AttributeModifier m : st.getAttributeModifiers(slot).get(attr)) {
            if (m.getId().equals(GemEvents.uuid(slot, effect))) return m.getAmount();
        }
        return 0;
    }

    private static Supplier<String> socketing(ServerLevel l, BlockPos o) {
        l.setBlock(o, Content.JEWELERS_TABLE.get().defaultBlockState(), 3);
        JewelerTableBE be = (JewelerTableBE) l.getBlockEntity(o);
        if (be == null) return () -> "no jeweler's table block entity";
        var player = FakePlayerFactory.getMinecraft(l);
        var items = be.getItems();
        items.setStackInSlot(JewelerTableBE.SLOT_TOOL, new ItemStack(Items.DIAMOND_SWORD));
        items.setStackInSlot(JewelerTableBE.SLOT_SETTING, new ItemStack(Content.SILVER_SETTING.get()));
        items.setStackInSlot(JewelerTableBE.SLOT_GEM, LapidarySetup.cutStack(GemType.RUBY, Cut.BRILLIANT, 80));
        be.onCustomButton(JewelerTableBE.BTN_ADD, player);
        be.onCustomButton(JewelerTableBE.BTN_SOCKET, player);
        ItemStack sword = items.getStackInSlot(JewelerTableBE.SLOT_TOOL).copy();
        List<GemData.Socket> s = GemData.sockets(sword);
        double dmg = modifier(sword, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE, GemEffect.ATTACK_DAMAGE);
        double off = modifier(sword, EquipmentSlot.OFFHAND, Attributes.ATTACK_DAMAGE, GemEffect.ATTACK_DAMAGE);
        double want = GemEffect.ATTACK_DAMAGE.perScale * GemData.scale(Cut.BRILLIANT, 80);
        // second socket needs gold: silver must be refused
        items.setStackInSlot(JewelerTableBE.SLOT_SETTING, new ItemStack(Content.SILVER_SETTING.get()));
        boolean refused = !be.addSocket();
        // take the gem back out
        be.onCustomButton(JewelerTableBE.BTN_SOCKET, player);
        boolean back = items.getStackInSlot(JewelerTableBE.SLOT_GEM).getItem() == GemType.RUBY.cutItem()
                && GemData.quality(items.getStackInSlot(JewelerTableBE.SLOT_GEM)) == 80;
        // armor: sapphire in a chestplate adds armor in the chest slot
        ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
        GemData.setSockets(chest, List.of(new GemData.Socket(GemType.SAPPHIRE, Cut.STEP, 60)));
        double armor = modifier(chest, EquipmentSlot.CHEST, Attributes.ARMOR, GemEffect.ARMOR);
        LOG.info("lapidary:socketing sockets={} damage bonus={} (want {}), offhand={}, gold refused silver={}, gem returned={}, chest armor bonus={}",
                s.size(), dmg, want, off, refused, back, armor);
        String err = null;
        if (s.size() != 1 || s.get(0).gem() != GemType.RUBY) err = "sword sockets wrong: " + s;
        else if (Math.abs(dmg - want) > 0.01) err = "attack damage bonus " + dmg + ", wanted " + want;
        else if (off != 0) err = "bonus also applied in the off hand";
        else if (!refused) err = "second socket accepted a silver setting";
        else if (!back) err = "gem was not returned";
        else if (armor <= 0) err = "no armor bonus from sapphire";
        final String r = err;
        return () -> r;
    }

    private static Supplier<String> clusters(ServerLevel l, BlockPos o) {
        BlockPos b = o.above();
        l.setBlock(b, block("budding_topaz").defaultBlockState(), 3);
        GemBuddingBlock bud = (GemBuddingBlock) block("budding_topaz");
        int steps = 0;
        for (int i = 0; i < 6; i++) if (bud.grow(l, b, Direction.UP)) steps++;
        BlockState c = l.getBlockState(b.above());
        int stage = c.is(block("topaz_cluster")) ? c.getValue(GemClusterBlock.STAGE) : -1;
        List<ItemStack> drops = Block.getDrops(c, l, b.above(), null, null, new ItemStack(Items.DIAMOND_PICKAXE));
        int rough = 0;
        for (ItemStack d : drops) if (d.getItem() == GemType.TOPAZ.roughItem()) rough += d.getCount();
        LOG.info("lapidary:clusters growth steps={} stage={} rough topaz dropped={}", steps, stage, rough);
        final String r = stage != 3 ? "cluster reached stage " + stage : steps != 4 ? "grew " + steps + " times, wanted 4" : rough < 2 ? "full cluster dropped " + rough + " rough topaz" : null;
        return () -> r;
    }

    private static Supplier<String> geode(ServerLevel l, BlockPos o) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 20; x++)
            for (int y = 0; y < 20; y++)
                for (int z = 0; z < 20; z++) l.setBlock(p.set(o.getX() + x, o.getY() + y, o.getZ() + z), Blocks.STONE.defaultBlockState(), 2);
        ConfiguredFeature<?, ?> f = l.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(new ResourceLocation("ls_lapidary", "geode_sapphire"));
        if (f == null) return () -> "configured feature ls_lapidary:geode_sapphire missing";
        boolean placed = f.place(l, l.getChunkSource().getGenerator(), l.random, o.offset(10, 10, 10));
        int budding = 0, druse = 0, crust = 0, clusters = 0;
        for (int x = 0; x < 20; x++)
            for (int y = 0; y < 20; y++)
                for (int z = 0; z < 20; z++) {
                    BlockState s = l.getBlockState(p.set(o.getX() + x, o.getY() + y, o.getZ() + z));
                    if (s.is(block("budding_sapphire"))) budding++;
                    else if (s.is(block("sapphire_druse"))) druse++;
                    else if (s.is(block("geode_crust"))) crust++;
                    else if (s.is(block("sapphire_cluster"))) clusters++;
                }
        LOG.info("lapidary:geode placed={} budding={} druse={} crust={} clusters={}", placed, budding, druse, crust, clusters);
        final String r = !placed ? "geode feature did not place" : budding + druse == 0 || crust == 0 ? "geode has no gem lining or shell" : null;
        return () -> r;
    }
}
