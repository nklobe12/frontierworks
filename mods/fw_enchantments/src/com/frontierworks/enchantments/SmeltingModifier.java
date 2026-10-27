package com.frontierworks.enchantments;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Supplier;

/** Smelting enchantment: replaces block drops with their furnace result. */
public class SmeltingModifier extends LootModifier {
    public static final Supplier<Codec<SmeltingModifier>> CODEC = Suppliers.memoize(() -> RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, SmeltingModifier::new)));

    public SmeltingModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Nonnull
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext ctx) {
        ObjectArrayList<ItemStack> out = new ObjectArrayList<>();
        for (ItemStack stack : loot) {
            Optional<SmeltingRecipe> r = ctx.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(stack), ctx.getLevel());
            if (r.isPresent()) {
                ItemStack res = r.get().getResultItem(ctx.getLevel().registryAccess()).copy();
                if (!res.isEmpty()) {
                    res.setCount(Math.min(res.getMaxStackSize(), res.getCount() * stack.getCount()));
                    out.add(res);
                    continue;
                }
            }
            out.add(stack);
        }
        return out;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
