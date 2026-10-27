package com.lodestar.toolsmith;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Supplier;

/** Block drops from modular tools: the searing trait smelts them, voidtouched sends them straight to the inventory. */
public class ToolTraitsModifier extends LootModifier {
    public static final Supplier<Codec<ToolTraitsModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(i -> codecStart(i).apply(i, ToolTraitsModifier::new)));

    public ToolTraitsModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Nonnull
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext ctx) {
        ItemStack tool = ctx.getParamOrNull(LootContextParams.TOOL);
        if (tool == null || !(tool.getItem() instanceof ModularToolItem) || !ctx.hasParam(LootContextParams.BLOCK_STATE)) return loot;
        ObjectArrayList<ItemStack> out = loot;
        if (ToolData.trait(tool, "searing") > 0) {
            out = new ObjectArrayList<>();
            for (ItemStack st : loot) {
                Optional<SmeltingRecipe> r = ctx.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(st), ctx.getLevel());
                if (r.isPresent()) {
                    ItemStack res = r.get().getResultItem(ctx.getLevel().registryAccess()).copy();
                    res.setCount(res.getCount() * st.getCount());
                    out.add(res);
                } else {
                    out.add(st);
                }
            }
        }
        Entity e = ctx.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (ToolData.trait(tool, "voidtouched") > 0 && e instanceof Player p) {
            ObjectArrayList<ItemStack> rest = new ObjectArrayList<>();
            for (ItemStack st : out) {
                if (!p.getInventory().add(st) && !st.isEmpty()) rest.add(st);
            }
            out = rest;
        }
        return out;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
