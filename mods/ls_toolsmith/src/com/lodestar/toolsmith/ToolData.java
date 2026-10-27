package com.lodestar.toolsmith;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads and writes everything a modular tool stores: the material of each part, modifiers, tool level and
 * experience, and the stats computed from them. Stats are recomputed whenever parts or modifiers change.
 */
public final class ToolData {
    public static final String TAG = "ls_tool";
    public static final int[] LEVEL_XP = {0, 100, 300, 700, 1500, 3000};

    private ToolData() {}

    public static CompoundTag tag(ItemStack stack) {
        return stack.getOrCreateTagElement(TAG);
    }

    public static boolean has(ItemStack stack) {
        return stack.getTagElement(TAG) != null;
    }

    public static List<ToolMaterial> materials(ItemStack stack) {
        List<ToolMaterial> out = new ArrayList<>();
        CompoundTag t = stack.getTagElement(TAG);
        if (t == null) return out;
        for (Tag e : t.getList("Mats", Tag.TAG_STRING)) {
            ToolMaterial m = Materials.get(e.getAsString());
            out.add(m == null ? Materials.FALLBACK : m);
        }
        return out;
    }

    public static Map<String, Integer> modifiers(ItemStack stack) {
        Map<String, Integer> out = new LinkedHashMap<>();
        CompoundTag t = stack.getTagElement(TAG);
        if (t == null) return out;
        CompoundTag mods = t.getCompound("Mods");
        for (String k : mods.getAllKeys()) out.put(k, mods.getInt(k));
        return out;
    }

    public static int modifier(ItemStack stack, String id) {
        CompoundTag t = stack.getTagElement(TAG);
        return t == null ? 0 : t.getCompound("Mods").getInt(id);
    }

    /** Trait levels: one level per part made of a material with that trait. */
    public static Map<String, Integer> traits(ItemStack stack) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (ToolMaterial m : materials(stack)) out.merge(m.trait(), 1, Integer::sum);
        return out;
    }

    public static int trait(ItemStack stack, String id) {
        int n = 0;
        for (ToolMaterial m : materials(stack)) if (m.trait().equals(id)) n++;
        return n;
    }

    public static int level(ItemStack stack) {
        CompoundTag t = stack.getTagElement(TAG);
        return t == null ? 1 : Math.max(1, t.getInt("Level"));
    }

    public static int xp(ItemStack stack) {
        CompoundTag t = stack.getTagElement(TAG);
        return t == null ? 0 : t.getInt("XP");
    }

    public static int totalSlots(ItemStack stack) {
        return 3 + (level(stack) - 1) + trait(stack, "brilliant");
    }

    public static int usedSlots(ItemStack stack) {
        int n = 0;
        for (Map.Entry<String, Integer> e : modifiers(stack).entrySet()) {
            Modifiers.Def d = Modifiers.get(e.getKey());
            n += d == null ? e.getValue() : d.slots() * e.getValue();
        }
        return n;
    }

    public static int freeSlots(ItemStack stack) {
        return Math.max(0, totalSlots(stack) - usedSlots(stack));
    }

    public static CompoundTag stats(ItemStack stack) {
        CompoundTag t = stack.getTagElement(TAG);
        return t == null ? new CompoundTag() : t.getCompound("Stats");
    }

    public static boolean isBroken(ItemStack stack) {
        return stack.getMaxDamage() > 0 && stack.getDamageValue() >= stack.getMaxDamage() - 1;
    }

    /** Builds a new tool stack of the given type from part materials (in the type's part order). */
    public static ItemStack build(ToolType type, List<ToolMaterial> mats) {
        ItemStack stack = new ItemStack(ToolsmithSetup.toolItem(type));
        CompoundTag t = tag(stack);
        ListTag l = new ListTag();
        for (ToolMaterial m : mats) l.add(StringTag.valueOf(m.id()));
        t.put("Mats", l);
        t.putInt("Level", 1);
        recompute(stack);
        return stack;
    }

    public static void setModifier(ItemStack stack, String id, int level) {
        CompoundTag mods = tag(stack).getCompound("Mods");
        if (level <= 0) mods.remove(id);
        else mods.putInt(id, level);
        tag(stack).put("Mods", mods);
        recompute(stack);
    }

    /** Recomputes the stats tag from parts, traits and modifiers. */
    public static void recompute(ItemStack stack) {
        if (!(stack.getItem() instanceof ModularToolItem tool)) return;
        ToolType type = tool.type;
        List<ToolMaterial> mats = materials(stack);
        if (mats.size() != type.parts.size()) return;
        ToolMaterial head = null;
        float handleMult = 0;
        int handles = 0, bindingBonus = 0;
        for (int i = 0; i < mats.size(); i++) {
            ToolMaterial m = mats.get(i);
            switch (type.parts.get(i).role) {
                case HEAD -> {
                    if (head == null) head = m;
                }
                case HANDLE -> {
                    handleMult += m.handleMultiplier();
                    handles++;
                }
                case BINDING -> bindingBonus += m.bindingDurability();
            }
        }
        if (head == null) head = mats.get(0);
        handleMult = handles == 0 ? 1f : handleMult / handles;
        float dur = (head.headDurability() + bindingBonus) * handleMult * type.durabilityFactor;
        float speed = head.headSpeed() * type.speedFactor;
        float attack = head.headAttack() + type.baseAttack;
        float attackSpeed = type.attackSpeed;
        int tier = head.tier();
        Map<String, Integer> traits = traits(stack);
        Map<String, Integer> mods = modifiers(stack);
        // trait stat bonuses
        dur *= 1 + 0.15f * traits.getOrDefault("sturdy", 0) + 0.1f * traits.getOrDefault("supple", 0)
                + 0.5f * traits.getOrDefault("unyielding", 0) + 0.1f * traits.getOrDefault("tempered", 0);
        attack += traits.getOrDefault("tempered", 0) + 0.3f * attack * Math.min(1, traits.getOrDefault("heavy", 0));
        attackSpeed += 0.15f * traits.getOrDefault("balanced", 0) + 0.2f * traits.getOrDefault("airy", 0)
                + 0.1f * traits.getOrDefault("lightweight", 0) - 0.1f * traits.getOrDefault("heavy", 0);
        speed *= 1 + 0.1f * traits.getOrDefault("lightweight", 0) + 0.2f * traits.getOrDefault("swift", 0) - 0.05f * traits.getOrDefault("dense", 0);
        // modifiers
        speed *= 1 + 0.15f * mods.getOrDefault("haste", 0);
        attack += mods.getOrDefault("sharpness", 0);
        if (mods.getOrDefault("diamond", 0) > 0) {
            dur += 500;
            tier = Math.max(tier, 3);
        }
        if (mods.getOrDefault("emerald", 0) > 0) dur *= 1.5f;
        if (mods.getOrDefault("netherite", 0) > 0) {
            tier = 4;
            speed *= 1.2f;
            attack *= 1.2f;
            dur *= 1.25f;
        }
        CompoundTag s = new CompoundTag();
        s.putInt("Dur", Math.max(1, Math.round(dur)));
        s.putFloat("Speed", speed);
        s.putFloat("Attack", attack);
        s.putFloat("AtkSpd", attackSpeed);
        s.putInt("Tier", tier);
        tag(stack).put("Stats", s);
        if (stack.getDamageValue() >= s.getInt("Dur")) stack.setDamageValue(s.getInt("Dur") - 1);
    }

    /** Adds tool experience; returns true if the tool levelled up. */
    public static boolean addXp(ItemStack stack, int amount) {
        CompoundTag t = tag(stack);
        int xp = t.getInt("XP") + amount;
        t.putInt("XP", xp);
        int lvl = Math.max(1, t.getInt("Level"));
        if (lvl < LEVEL_XP.length && xp >= LEVEL_XP[lvl]) {
            t.putInt("Level", lvl + 1);
            return true;
        }
        return false;
    }

    /** XP needed for the next level, or -1 at max level. */
    public static int nextLevelXp(ItemStack stack) {
        int lvl = level(stack);
        return lvl < LEVEL_XP.length ? LEVEL_XP[lvl] : -1;
    }
}
