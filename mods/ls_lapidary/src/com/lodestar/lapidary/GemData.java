package com.lodestar.lapidary;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * NBT of cut gems ({"Cut":"brilliant","Quality":87}) and of socketed equipment
 * ({"LapidarySockets":[{"Gem":"ruby","Cut":"step","Quality":64},{}]}; an empty compound is an empty socket).
 */
public final class GemData {
    public static final String SOCKETS = "LapidarySockets";

    private GemData() {}

    // ------------------------------------------------------------------ cut gems

    public static boolean isCut(ItemStack st) {
        return st.hasTag() && st.getTag().contains("Cut");
    }

    public static Cut cut(ItemStack st) {
        return st.hasTag() ? Cut.byId(st.getTag().getString("Cut")) : Cut.STEP;
    }

    /** Quality 1-100; uncut-looking stacks (no NBT, e.g. from creative) count as 50. */
    public static int quality(ItemStack st) {
        return st.hasTag() && st.getTag().contains("Quality") ? Math.max(1, Math.min(100, st.getTag().getInt("Quality"))) : 50;
    }

    public static ItemStack setCut(ItemStack st, Cut cut, int quality) {
        CompoundTag t = st.getOrCreateTag();
        t.putString("Cut", cut.id());
        t.putInt("Quality", Math.max(1, Math.min(100, quality)));
        return st;
    }

    public static float scale(Cut cut, int quality) {
        return cut.mult * (0.5f + quality / 100f);
    }

    public static String gradeKey(int quality) {
        if (quality < 25) return "chipped";
        if (quality < 50) return "flawed";
        if (quality < 75) return "fine";
        if (quality < 95) return "flawless";
        return "perfect";
    }

    public static Component grade(int quality) {
        String k = gradeKey(quality);
        ChatFormatting f = switch (k) {
            case "chipped" -> ChatFormatting.DARK_GRAY;
            case "flawed" -> ChatFormatting.GRAY;
            case "fine" -> ChatFormatting.WHITE;
            case "flawless" -> ChatFormatting.AQUA;
            default -> ChatFormatting.GOLD;
        };
        return Component.translatable("grade.ls_lapidary." + k).withStyle(f);
    }

    // ------------------------------------------------------------------ sockets

    public record Socket(@Nullable GemType gem, Cut cut, int quality) {
        public static final Socket EMPTY = new Socket(null, Cut.STEP, 0);

        public boolean empty() {
            return gem == null;
        }

        public float scale() {
            return gem == null ? 0 : GemData.scale(cut, quality);
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            if (gem != null) {
                t.putString("Gem", gem.id());
                t.putString("Cut", cut.id());
                t.putInt("Quality", quality);
            }
            return t;
        }

        static Socket load(CompoundTag t) {
            GemType g = t.contains("Gem") ? GemType.byId(t.getString("Gem")) : null;
            if (g == null) return EMPTY;
            return new Socket(g, Cut.byId(t.getString("Cut")), Math.max(1, Math.min(100, t.getInt("Quality"))));
        }

        /** The cut gem item this socket holds. */
        public ItemStack toStack() {
            if (gem == null) return ItemStack.EMPTY;
            Item it = gem.cutItem();
            return it == null ? ItemStack.EMPTY : setCut(new ItemStack(it), cut, quality);
        }

        public static Socket of(ItemStack gemStack) {
            GemType g = GemType.ofCut(gemStack);
            return g == null ? EMPTY : new Socket(g, GemData.cut(gemStack), GemData.quality(gemStack));
        }
    }

    /** Equipment that can take sockets: single, damageable items (tools, weapons, armor, shields, bows...). */
    public static boolean socketable(ItemStack st) {
        return !st.isEmpty() && st.getMaxStackSize() == 1 && st.isDamageableItem();
    }

    public static boolean isArmor(ItemStack st) {
        return LivingEntity.getEquipmentSlotForItem(st).getType() == EquipmentSlot.Type.ARMOR;
    }

    public static int maxSockets(ItemStack st) {
        return isArmor(st) ? 2 : 3;
    }

    public static boolean hasSockets(ItemStack st) {
        return st.hasTag() && st.getTag().contains(SOCKETS, Tag.TAG_LIST);
    }

    public static List<Socket> sockets(ItemStack st) {
        List<Socket> out = new ArrayList<>();
        if (!hasSockets(st)) return out;
        ListTag l = st.getTag().getList(SOCKETS, Tag.TAG_COMPOUND);
        for (int i = 0; i < l.size(); i++) out.add(Socket.load(l.getCompound(i)));
        return out;
    }

    public static void setSockets(ItemStack st, List<Socket> sockets) {
        ListTag l = new ListTag();
        for (Socket s : sockets) l.add(s.save());
        st.getOrCreateTag().put(SOCKETS, l);
    }

    /** Sum of each effect over the filled sockets, for the item used as armor or as a tool. */
    public static Map<GemEffect, Float> totals(ItemStack st, boolean armor) {
        Map<GemEffect, Float> out = new EnumMap<>(GemEffect.class);
        if (!hasSockets(st)) return out;
        for (Socket s : sockets(st)) {
            if (s.empty()) continue;
            GemEffect e = s.gem().effect(armor);
            out.merge(e, e.perScale * s.scale(), Float::sum);
        }
        return out;
    }

    /** Amount of one effect from an item, counting it as a tool (main hand) unless it is armor. */
    public static float amount(ItemStack st, GemEffect effect) {
        if (!hasSockets(st)) return 0;
        Float f = totals(st, isArmor(st)).get(effect);
        return f == null ? 0 : f;
    }

    /** Sum of an effect over all worn armor. */
    public static float worn(LivingEntity e, GemEffect effect) {
        float sum = 0;
        for (ItemStack st : e.getArmorSlots()) sum += amount(st, effect);
        return sum;
    }
}
