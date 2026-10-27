package com.lodestar.metalwork;

import com.lodestar.core.recipe.display.LensRegistry;
import com.lodestar.core.recipe.display.RecipeCategory;
import com.lodestar.core.recipe.display.RecipeView;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/** Hand-written registrations and events for Metalwork. */
public final class MetalworkSetup {
    private MetalworkSetup() {}

    public static final RegistryObject<RecipeType<SmithingRecipe>> SMITHING_TYPE = Content.REG.recipeTypes().register("smithing",
            () -> RecipeType.simple(new ResourceLocation("ls_metalwork", "smithing")));
    public static final RegistryObject<RecipeSerializer<SmithingRecipe>> SMITHING_SERIALIZER = Content.REG.recipeSerializers().register("smithing",
            SmithingRecipe.Serializer::new);

    public static void init(IEventBus bus) {
        MinecraftForge.EVENT_BUS.addListener(MetalworkSetup::playerTick);
        MinecraftForge.EVENT_BUS.addListener(MetalworkSetup::quench);
        LensRegistry.machine(Content.ALLOYING, () -> new ItemStack(Content.ALLOY_KILN.get()), 120);

        ResourceLocation smithing = new ResourceLocation("ls_metalwork", "smithing");
        LensRegistry.register(new RecipeCategory(smithing, Component.translatable("lens.category.ls_metalwork.smithing"),
                () -> new ItemStack(Content.SMITHING_ANVIL.get()),
                () -> List.of(new ItemStack(Content.SMITHING_ANVIL.get()), new ItemStack(Content.SMITHING_HAMMER.get()), new ItemStack(Content.SMITHS_FORGE.get())), 110),
                level -> {
                    List<RecipeView> out = new ArrayList<>();
                    for (SmithingRecipe r : level.getRecipeManager().getAllRecipesFor(SMITHING_TYPE.get())) {
                        RecipeView.Builder b = RecipeView.builder(smithing, 150, 40).id(r.getId());
                        b.input(4, 12, r.input);
                        b.arrow(26, 12);
                        b.output(56, 12, r.result);
                        b.text(80, 6, AnvilBE.shapeName(r.shape));
                        b.text(80, 17, Component.translatable("lens.ls_metalwork.strikes", r.strikes), 0xFF606060);
                        b.text(80, 28, Component.translatable("lens.ls_metalwork.min_heat", r.minHeat), 0xFFB04020);
                        out.add(b.build());
                    }
                    return out;
                });

        ResourceLocation bloom = new ResourceLocation("ls_metalwork", "bloomery");
        LensRegistry.register(new RecipeCategory(bloom, Component.translatable("lens.category.ls_metalwork.bloomery"),
                () -> new ItemStack(Content.BLOOMERY.get()), () -> List.of(new ItemStack(Content.BLOOMERY.get()), new ItemStack(Content.FIRE_BRICKS.get())), 115),
                level -> List.of(RecipeView.builder(bloom, 150, 48)
                        .input(4, 4, Ingredient.of(BloomeryBE.ORE))
                        .input(4, 24, Ingredient.of(BloomeryBE.FUEL).getItems().length > 0 ? twoOf(Ingredient.of(BloomeryBE.FUEL)) : List.of(new ItemStack(Items.CHARCOAL, 2)))
                        .arrow(28, 13).output(60, 13, new ItemStack(Content.STEEL_BLOOM.get()))
                        .text(84, 8, Component.translatable("lens.ls_metalwork.bloomery_time"), 0xFF606060)
                        .text(84, 20, Component.translatable("lens.ls_metalwork.bloomery_hint"), 0xFF606060)
                        .build()));
    }

    private static List<ItemStack> twoOf(Ingredient ing) {
        List<ItemStack> l = new ArrayList<>();
        for (ItemStack s : ing.getItems()) {
            ItemStack c = s.copy();
            c.setCount(2);
            l.add(c);
        }
        return l;
    }

    /** Hot items cool in inventories and burn hands unless tongs are held in the off hand. */
    private static void playerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.player.level().isClientSide || e.player.tickCount % 20 != 0) return;
        Player p = e.player;
        boolean tongs = p.getOffhandItem().is(Content.TONGS.get()) || p.getMainHandItem().is(Content.TONGS.get());
        boolean burned = false;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            int h = Heat.get(st);
            if (h <= 0) continue;
            Heat.set(st, h - 30);
            if (h > 300 && !tongs && !p.isCreative() && !burned) {
                burned = true;
                p.setSecondsOnFire(1);
                p.displayClientMessage(Component.translatable("msg.ls_metalwork.too_hot").withStyle(ChatFormatting.RED), true);
            }
        }
    }

    /** Using a hot item on a water cauldron quenches it instantly (and uses up a little water). */
    private static void quench(PlayerInteractEvent.RightClickBlock e) {
        ItemStack st = e.getItemStack();
        if (Heat.get(st) <= 0) return;
        BlockState state = e.getLevel().getBlockState(e.getPos());
        if (!state.is(Blocks.WATER_CAULDRON)) return;
        if (!e.getLevel().isClientSide) {
            Heat.set(st, 0);
            LayeredCauldronBlock.lowerFillLevel(state, e.getLevel(), e.getPos());
            e.getLevel().playSound(null, e.getPos(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 1.2f);
            if (e.getLevel() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CLOUD, e.getPos().getX() + 0.5, e.getPos().getY() + 1.0, e.getPos().getZ() + 0.5, 8, 0.2, 0.1, 0.2, 0.02);
            }
        }
        e.setCanceled(true);
        e.setCancellationResult(net.minecraft.world.InteractionResult.sidedSuccess(e.getLevel().isClientSide));
    }
}
