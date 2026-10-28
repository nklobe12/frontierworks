package com.lodestar.lapidary;

import com.lodestar.core.recipe.MachineRecipe;
import com.lodestar.core.recipe.display.LensRegistry;
import com.lodestar.core.recipe.display.RecipeCategory;
import com.lodestar.core.recipe.display.RecipeView;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** Hand-written setup for Lapidary: gem events, recipe viewer categories and self-tests. */
public final class LapidarySetup {
    private LapidarySetup() {}

    public static final ResourceLocation CUTTING = new ResourceLocation("ls_lapidary", "gem_cutting");
    public static final ResourceLocation SOCKETING = new ResourceLocation("ls_lapidary", "socketing");
    public static final ResourceLocation GEODES = new ResourceLocation("ls_lapidary", "geodes");

    public static void init(IEventBus bus) {
        GemEvents.init();
        lens();
        if (com.lodestar.core.test.SelfTest.enabled()) LapidaryTests.register();
    }

    public static ItemStack cutStack(GemType g, Cut cut, int quality) {
        return GemData.setCut(new ItemStack(g.cutItem()), cut, quality);
    }

    private static Block block(String name) {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation("ls_lapidary", name));
    }

    /** A cluster item showing one growth stage (as dropped with Silk Touch). */
    public static ItemStack clusterStack(GemType g, int stage) {
        ItemStack st = new ItemStack(block(g.id() + "_cluster"));
        CompoundTag bs = new CompoundTag();
        bs.putString("stage", Integer.toString(stage));
        st.getOrCreateTag().put("BlockStateTag", bs);
        return st;
    }

    private static List<ItemStack> abrasives() {
        List<ItemStack> l = new ArrayList<>();
        l.add(new ItemStack(Content.EMERY_POWDER.get()));
        l.add(new ItemStack(Content.GEM_GRIT.get()));
        ItemStack dd = com.lodestar.core.util.LS.stack("ls_veins:diamond_dust", 1);
        if (!dd.isEmpty()) l.add(dd);
        return l;
    }

    private static void lens() {
        LensRegistry.register(new RecipeCategory(CUTTING, Component.translatable("lens.category.ls_lapidary.gem_cutting"),
                () -> new ItemStack(Content.CUTTING_WHEEL.get()), () -> List.of(new ItemStack(Content.CUTTING_WHEEL.get())), 300), level -> {
            List<RecipeView> out = new ArrayList<>();
            for (MachineRecipe r : Content.GEM_CUTTING.all(level)) {
                if (r.inputs.isEmpty() || r.outputs.isEmpty()) continue;
                ItemStack res = r.outputs.get(0).stack;
                RecipeView.Builder b = RecipeView.builder(CUTTING, 160, 58).id(r.getId());
                b.input(4, 4, r.inputs.get(0).ingredient);
                b.catalyst(4, 26, abrasives(), Component.translatable("lens.ls_lapidary.abrasive"));
                b.arrow(26, 14);
                int x = 56;
                for (Cut c : Cut.values()) {
                    ItemStack st = GemData.setCut(res.copy(), c, c.base + 10);
                    b.output(x, 4, st);
                    x += 20;
                }
                b.text(56, 26, Component.translatable("lens.ls_lapidary.cuts"), 0xFF404040);
                b.text(56, 37, Component.translatable("lens.ls_lapidary.harder"), 0xFF606060);
                b.text(4, 48, Component.literal((r.energy * r.time) + " FE  " + String.format("%.1fs", r.time / 20f)), 0xFF606060);
                out.add(b.build());
            }
            return out;
        });

        LensRegistry.register(new RecipeCategory(SOCKETING, Component.translatable("lens.category.ls_lapidary.socketing"),
                () -> new ItemStack(Content.JEWELERS_TABLE.get()), () -> List.of(new ItemStack(Content.JEWELERS_TABLE.get())), 301), level -> {
            List<RecipeView> out = new ArrayList<>();
            RecipeView.Builder sb = RecipeView.builder(SOCKETING, 170, 52);
            sb.input(4, 4, List.of(new ItemStack(Items.IRON_SWORD), new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.IRON_CHESTPLATE)));
            for (int i = 0; i < 3; i++) {
                sb.input(30 + i * 20, 4, new ItemStack(JewelerTableBE.settingFor(i)));
                sb.text(32 + i * 20, 24, Component.literal(Integer.toString(i + 1)), 0xFF404040);
            }
            sb.text(4, 34, Component.translatable("lens.ls_lapidary.max_sockets"), 0xFF606060);
            out.add(sb.build());
            for (GemType g : GemType.values()) {
                float s = GemData.scale(Cut.STEP, 60);
                RecipeView.Builder b = RecipeView.builder(SOCKETING, 170, 52);
                b.input(4, 14, cutStack(g, Cut.STEP, 60));
                b.arrow(24, 14);
                ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                GemData.setSockets(sword, List.of(new GemData.Socket(g, Cut.STEP, 60)));
                ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
                GemData.setSockets(chest, List.of(new GemData.Socket(g, Cut.STEP, 60)));
                b.outputs(52, 14, List.of(sword, chest));
                b.text(74, 4, Component.translatable("lens.ls_lapidary.tools", g.toolEffect.describe(g.toolEffect.perScale * s)), 0xFF404040);
                b.text(74, 16, Component.translatable("lens.ls_lapidary.armor", g.armorEffect.describe(g.armorEffect.perScale * s)), 0xFF404040);
                b.text(74, 30, Component.translatable("lens.ls_lapidary.per_gem"), 0xFF707070);
                out.add(b.build());
            }
            return out;
        });

        LensRegistry.register(new RecipeCategory(GEODES, Component.translatable("lens.category.ls_lapidary.geodes"),
                () -> new ItemStack(block("budding_ruby")), () -> List.of(new ItemStack(block("geode_crust")), new ItemStack(block("geode_lining"))), 302), level -> {
            List<RecipeView> out = new ArrayList<>();
            for (GemType g : GemType.values()) {
                RecipeView.Builder b = RecipeView.builder(GEODES, 160, 48);
                b.input(4, 12, new ItemStack(block("budding_" + g.id())));
                b.arrow(24, 12);
                for (int s = 0; s < 4; s++) b.output(52 + s * 18, 2, clusterStack(g, s));
                b.output(52 + 4 * 18 + 6, 12, new ItemStack(g.roughItem(), 2));
                b.text(52, 24, Component.translatable("lens.ls_lapidary.random_ticks"), 0xFF606060);
                b.text(52, 35, Component.translatable("lens.ls_lapidary.full_only"), 0xFF606060);
                out.add(b.build());
            }
            return out;
        });
    }
}
