package com.lodestar.kinetics;

import com.lodestar.core.recipe.ChanceStack;
import com.lodestar.core.recipe.MachineRecipe;
import com.lodestar.core.recipe.MachineRecipeKind;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What an air blower's stream does to items once it has passed through a block: water rinses them, fire or a lit
 * campfire roasts them (smoker recipes), lava sears them (furnace recipes) and soul fire steeps them.
 */
public final class AirProcessing {
    public enum Type { NONE, RINSE, ROAST, SEAR, STEEP }

    /** Ticks a stack must ride the stream (at 64 RPM; faster streams work faster). */
    public static final int TIME = 100;

    private AirProcessing() {}

    public static Type catalyst(BlockState s) {
        if (s.getFluidState().is(FluidTags.WATER)) return Type.RINSE;
        if (s.getFluidState().is(FluidTags.LAVA)) return Type.SEAR;
        if (s.is(Blocks.SOUL_FIRE)) return Type.STEEP;
        if (s.is(Blocks.FIRE)) return Type.ROAST;
        if (s.is(Blocks.SOUL_CAMPFIRE) && s.getValue(CampfireBlock.LIT)) return Type.STEEP;
        if (s.is(Blocks.CAMPFIRE) && s.getValue(CampfireBlock.LIT)) return Type.ROAST;
        return Type.NONE;
    }

    /** Results of processing a whole stack, or null if the stream does nothing to it. */
    @Nullable
    public static List<ItemStack> process(Level level, Type type, ItemStack stack) {
        return switch (type) {
            case RINSE -> machine(level, Content.RINSING, stack, level.random);
            case STEEP -> machine(level, Content.STEEPING, stack, level.random);
            case ROAST -> cooking(level, RecipeType.SMOKING, stack);
            case SEAR -> cooking(level, RecipeType.SMELTING, stack);
            case NONE -> null;
        };
    }

    public static boolean canProcess(Level level, Type type, ItemStack stack) {
        return switch (type) {
            case RINSE -> find(level, Content.RINSING, stack) != null;
            case STEEP -> find(level, Content.STEEPING, stack) != null;
            case ROAST -> level.getRecipeManager().getRecipeFor(RecipeType.SMOKING, new SimpleContainer(stack), level).isPresent();
            case SEAR -> level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(stack), level).isPresent();
            case NONE -> false;
        };
    }

    @Nullable
    private static MachineRecipe find(Level level, MachineRecipeKind kind, ItemStack stack) {
        for (MachineRecipe r : kind.all(level)) {
            if (!r.inputs.isEmpty() && r.inputs.get(0).ingredient.test(stack)) return r;
        }
        return null;
    }

    @Nullable
    private static List<ItemStack> machine(Level level, MachineRecipeKind kind, ItemStack stack, RandomSource rnd) {
        MachineRecipe r = find(level, kind, stack);
        if (r == null) return null;
        int per = Math.max(1, r.inputs.get(0).count);
        int runs = stack.getCount() / per;
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < runs; i++) {
            for (ChanceStack cs : r.outputs) add(out, cs.roll(rnd));
        }
        int left = stack.getCount() - runs * per;
        if (left > 0) add(out, stack.copyWithCount(left));
        return out;
    }

    private static <T extends AbstractCookingRecipe> List<ItemStack> cooking(Level level, RecipeType<T> type, ItemStack stack) {
        Optional<T> r = level.getRecipeManager().getRecipeFor(type, new SimpleContainer(stack), level);
        if (r.isEmpty()) return null;
        ItemStack res = r.get().getResultItem(level.registryAccess());
        if (res.isEmpty()) return null;
        List<ItemStack> out = new ArrayList<>();
        int total = res.getCount() * stack.getCount();
        while (total > 0) {
            int n = Math.min(total, res.getMaxStackSize());
            out.add(res.copyWithCount(n));
            total -= n;
        }
        return out;
    }

    private static void add(List<ItemStack> out, ItemStack s) {
        if (s.isEmpty()) return;
        for (ItemStack o : out) {
            if (ItemStack.isSameItemSameTags(o, s) && o.getCount() < o.getMaxStackSize()) {
                int move = Math.min(s.getCount(), o.getMaxStackSize() - o.getCount());
                o.grow(move);
                s.shrink(move);
                if (s.isEmpty()) return;
            }
        }
        out.add(s.copy());
    }
}
