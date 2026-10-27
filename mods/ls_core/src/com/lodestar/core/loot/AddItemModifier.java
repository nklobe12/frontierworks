package com.lodestar.core.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/** Data-driven loot injection: {"type":"ls_core:add_item","conditions":[...],"item":"x","chance":0.2,"min":1,"max":2} */
public class AddItemModifier extends LootModifier {
    public static final Supplier<Codec<AddItemModifier>> CODEC = Suppliers.memoize(() -> RecordCodecBuilder.create(inst -> codecStart(inst)
            .and(ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(m -> m.item))
            .and(Codec.FLOAT.optionalFieldOf("chance", 1.0f).forGetter(m -> m.chance))
            .and(Codec.INT.optionalFieldOf("min", 1).forGetter(m -> m.min))
            .and(Codec.INT.optionalFieldOf("max", 1).forGetter(m -> m.max))
            .apply(inst, AddItemModifier::new)));

    private final Item item;
    private final float chance;
    private final int min;
    private final int max;

    public AddItemModifier(LootItemCondition[] conditions, Item item, float chance, int min, int max) {
        super(conditions);
        this.item = item;
        this.chance = chance;
        this.min = min;
        this.max = Math.max(min, max);
    }

    @Nonnull
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext ctx) {
        if (ctx.getRandom().nextFloat() < chance) {
            int n = min + (max > min ? ctx.getRandom().nextInt(max - min + 1) : 0);
            if (n > 0) loot.add(new ItemStack(item, n));
        }
        return loot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
