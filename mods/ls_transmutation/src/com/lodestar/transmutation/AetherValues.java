package com.lodestar.transmutation;

import com.lodestar.core.net.CoreNet;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aether value (AV) of every item. A table of base values seeds the map; the rest is worked out from crafting,
 * smelting, stonecutting and smithing recipes (the cheapest known way to make an item, divided by how many the
 * recipe makes). Computed on the server whenever recipes load, then sent to every client.
 */
public final class AetherValues {
    private static final Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private static Map<Item, Long> values = new IdentityHashMap<>();

    private AetherValues() {}

    public static long get(Item item) {
        Long v = values.get(item);
        return v == null ? 0 : v;
    }

    /** Value of a stack of one, scaled down for damaged tools. */
    public static long get(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        long v = get(stack.getItem());
        if (v > 0 && stack.isDamageableItem() && stack.getMaxDamage() > 0) {
            v = v * (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage();
        }
        if (stack.getItem() instanceof AetherCellItem) v += AetherCellItem.stored(stack);
        return v;
    }

    public static boolean has(ItemStack stack) {
        return !stack.isEmpty() && values.containsKey(stack.getItem());
    }

    public static int size() {
        return values.size();
    }

    public static Map<Item, Long> all() {
        return values;
    }

    // ------------------------------------------------------------------ computation

    public static void compute(MinecraftServer server) {
        long t0 = System.currentTimeMillis();
        Map<Item, Long> map = new IdentityHashMap<>();
        for (Object[] e : BaseValues.TABLE) {
            String id = (String) e[0];
            long v = ((Number) e[1]).longValue();
            if (id.startsWith("#")) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation(id.substring(1)));
                for (Holder<Item> h : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) map.putIfAbsent(h.value(), v);
            } else {
                Item it = BuiltInRegistries.ITEM.get(new ResourceLocation(id));
                if (it != Items.AIR) map.put(it, v);
            }
        }
        RegistryAccess access = server.registryAccess();
        List<Recipe<?>> recipes = new ArrayList<>();
        for (Recipe<?> r : server.getRecipeManager().getRecipes()) {
            RecipeType<?> t = r.getType();
            if (t == RecipeType.CRAFTING || t == RecipeType.SMELTING || t == RecipeType.STONECUTTING || t == RecipeType.SMITHING) recipes.add(r);
        }
        boolean changed = true;
        int passes = 0;
        while (changed && passes++ < 24) {
            changed = false;
            Map<Item, Long> found = new HashMap<>();
            for (Recipe<?> r : recipes) {
                ItemStack out;
                try {
                    out = r.getResultItem(access);
                } catch (Exception ex) {
                    continue;
                }
                if (out.isEmpty() || map.containsKey(out.getItem())) continue;
                long cost = 0;
                boolean known = true;
                List<Ingredient> ings = r.getType() == RecipeType.SMITHING ? com.lodestar.core.recipe.RecipeUtil.smithingInputs(r) : r.getIngredients();
                for (Ingredient ing : ings) {
                    if (ing.isEmpty()) continue;
                    long best = -1;
                    for (ItemStack s : ing.getItems()) {
                        Long v = map.get(s.getItem());
                        if (v == null) continue;
                        long net = v;
                        ItemStack rem = s.getCraftingRemainingItem();
                        if (!rem.isEmpty()) net -= map.getOrDefault(rem.getItem(), 0L);
                        if (best < 0 || net < best) best = net;
                    }
                    if (best < 0) {
                        known = false;
                        break;
                    }
                    cost += best;
                }
                if (!known || cost <= 0) continue;
                long each = Math.max(1, cost / out.getCount());
                Long prev = found.get(out.getItem());
                if (prev == null || each < prev) found.put(out.getItem(), each);
            }
            for (Map.Entry<Item, Long> e : found.entrySet()) {
                if (!map.containsKey(e.getKey())) {
                    map.put(e.getKey(), e.getValue());
                    changed = true;
                }
            }
        }
        values = map;
        LOG.info("Transmutation: {} items have aether values ({} passes, {} ms)", map.size(), passes, System.currentTimeMillis() - t0);
    }

    // ------------------------------------------------------------------ sync

    public static CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<Item, Long> e : values.entrySet()) {
            tag.putLong(BuiltInRegistries.ITEM.getKey(e.getKey()).toString(), e.getValue());
        }
        return tag;
    }

    public static void send(ServerPlayer player) {
        CoreNet.toPlayer(player, "ls_transmutation:values", toTag());
    }

    /** Client: replace the table with values received from the server. */
    public static void receive(CompoundTag tag) {
        Map<Item, Long> map = new IdentityHashMap<>();
        for (String k : tag.getAllKeys()) {
            Item it = BuiltInRegistries.ITEM.get(new ResourceLocation(k));
            if (it != Items.AIR) map.put(it, tag.getLong(k));
        }
        values = map;
    }

    public static String format(long v) {
        return String.format("%,d", v);
    }
}
