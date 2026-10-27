package com.lodestar.toolsmith;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.lodestar.core.util.AreaMining;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A tool assembled from parts. Its stats live in NBT (see {@link ToolData}). It never breaks: at zero durability it
 * becomes broken and stops working until repaired in a tool station. Large tools mine 3x3 (hammer, excavator),
 * fell whole trees (broad axe) or reap crops in an area (scythe).
 */
public class ModularToolItem extends Item {
    public final ToolType type;

    public ModularToolItem(Properties props, ToolType type) {
        super(props.durability(100));
        this.type = type;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        int d = ToolData.stats(stack).getInt("Dur");
        return d > 0 ? d : 100;
    }

    private static int tierNeeded(BlockState state) {
        if (state.is(Tags.Blocks.NEEDS_NETHERITE_TOOL)) return 4;
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return 3;
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) return 2;
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) return 1;
        return 0;
    }

    private boolean effectiveOn(BlockState state) {
        for (TagKey<Block> t : type.mineable) if (state.is(t)) return true;
        return type == ToolType.SWORD && (state.is(Blocks.COBWEB) || state.is(BlockTags.SWORD_EFFICIENT));
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (ToolData.isBroken(stack)) return false;
        if (type == ToolType.SWORD && state.is(Blocks.COBWEB)) return true;
        boolean mineable = false;
        for (TagKey<Block> t : type.mineable) if (state.is(t)) mineable = true;
        return mineable && ToolData.stats(stack).getInt("Tier") >= tierNeeded(state);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (ToolData.isBroken(stack)) return 0.3f;
        if (!effectiveOn(state)) return 1f;
        if (type == ToolType.SWORD) return state.is(Blocks.COBWEB) ? 15f : 1.5f;
        return Math.max(1f, ToolData.stats(stack).getFloat("Speed") * Traits.speedMultiplier(stack, state, null));
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction action) {
        return !ToolData.isBroken(stack) && type.actions.contains(action);
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
        int a = Traits.reduceDamage(stack, amount, entity.getRandom());
        int left = stack.getMaxDamage() - 1 - stack.getDamageValue();
        if (left <= 0) return 0;
        if (a >= left && entity instanceof Player p) {
            p.displayClientMessage(Component.translatable("msg.ls_toolsmith.broke", stack.getHoverName()).withStyle(ChatFormatting.RED), true);
            entity.level().playSound(null, entity.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 1.2f);
        }
        return Math.min(a, left);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity user) {
        if (level.isClientSide || ToolData.isBroken(stack)) return true;
        if (state.getDestroySpeed(level, pos) != 0f) stack.hurtAndBreak(1, user, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        Traits.onBlockBroken(stack, level, pos, state, user);
        if (user instanceof ServerPlayer sp) {
            gainXp(stack, sp, Traits.blockXp(stack, state));
            if (!sp.isShiftKeyDown() && !AreaMining.isBusy()) areaEffects(stack, sp, pos, state);
        }
        return true;
    }

    private void areaEffects(ItemStack stack, ServerPlayer sp, BlockPos pos, BlockState state) {
        int radius = 1 + ToolData.modifier(stack, "expanded");
        if (type == ToolType.HAMMER || type == ToolType.EXCAVATOR) {
            if (!effectiveOn(state)) return;
            Direction face = AreaMining.face(sp);
            float maxHard = state.getDestroySpeed(sp.level(), pos) + 3f;
            AreaMining.breakAll(sp, AreaMining.plane(pos, face, radius), s -> effectiveOn(s) && isCorrectToolForDrops(stack, s), maxHard);
        } else if (type == ToolType.BROAD_AXE && state.is(BlockTags.LOGS)) {
            AreaMining.breakAll(sp, connectedLogs(sp.level(), pos, 128), s -> s.is(BlockTags.LOGS), 10f);
        } else if (type == ToolType.SCYTHE && (state.getBlock() instanceof CropBlock || state.is(BlockTags.LEAVES) || state.is(BlockTags.REPLACEABLE))) {
            List<BlockPos> around = new java.util.ArrayList<>();
            for (int dx = -radius; dx <= radius; dx++)
                for (int dz = -radius; dz <= radius; dz++)
                    for (int dy = -1; dy <= 1; dy++)
                        if (dx != 0 || dz != 0 || dy != 0) around.add(pos.offset(dx, dy, dz));
            AreaMining.breakAll(sp, around, s -> (s.getBlock() instanceof CropBlock crop && crop.isMaxAge(s)) || s.is(BlockTags.LEAVES)
                    || s.is(BlockTags.REPLACEABLE) && !s.isAir() && s.getFluidState().isEmpty(), 1f);
        }
    }

    private static List<BlockPos> connectedLogs(Level level, BlockPos start, int max) {
        List<BlockPos> out = new java.util.ArrayList<>();
        Deque<BlockPos> q = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        q.add(start);
        seen.add(start);
        while (!q.isEmpty() && out.size() < max) {
            BlockPos p = q.poll();
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = 0; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos n = p.offset(dx, dy, dz);
                        if (seen.add(n) && level.getBlockState(n).is(BlockTags.LOGS)) {
                            q.add(n);
                            out.add(n);
                        }
                    }
        }
        return out;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (ToolData.isBroken(stack)) return false;
        stack.hurtAndBreak(type == ToolType.SWORD || type == ToolType.SCYTHE ? 1 : 2, attacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        Traits.onHit(stack, target, attacker);
        if (type == ToolType.SCYTHE) {
            for (LivingEntity e : attacker.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(1.5, 0.25, 1.5),
                    e -> e != attacker && e != target && e.isAlive() && !(e instanceof Player))) {
                e.hurt(attacker.damageSources().mobAttack(attacker), ToolData.stats(stack).getFloat("Attack") * 0.5f);
            }
        }
        if (attacker instanceof ServerPlayer sp) gainXp(stack, sp, target.isAlive() ? 1 : 5);
        return true;
    }

    private static void gainXp(ItemStack stack, ServerPlayer player, int xp) {
        if (ToolData.addXp(stack, xp)) {
            player.displayClientMessage(Component.translatable("msg.ls_toolsmith.level_up", stack.getHoverName(), ToolData.level(stack))
                    .withStyle(ChatFormatting.GOLD), false);
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        ItemStack stack = ctx.getItemInHand();
        if (ToolData.isBroken(stack)) return InteractionResult.PASS;
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        ToolAction[] actions = switch (type) {
            case AXE, BROAD_AXE -> new ToolAction[]{ToolActions.AXE_STRIP, ToolActions.AXE_SCRAPE, ToolActions.AXE_WAX_OFF};
            case SHOVEL, EXCAVATOR -> new ToolAction[]{ToolActions.SHOVEL_FLATTEN};
            case KAMA, SCYTHE -> new ToolAction[]{ToolActions.HOE_TILL};
            default -> new ToolAction[0];
        };
        for (ToolAction a : actions) {
            if (a == ToolActions.SHOVEL_FLATTEN && !level.getBlockState(pos.above()).isAir()) continue;
            BlockState modified = state.getToolModifiedState(ctx, a, false);
            if (modified == null) continue;
            level.playSound(ctx.getPlayer(), pos, a == ToolActions.HOE_TILL ? SoundEvents.HOE_TILL : a == ToolActions.SHOVEL_FLATTEN
                    ? SoundEvents.SHOVEL_FLATTEN : SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0f, 1.0f);
            if (!level.isClientSide) {
                level.setBlock(pos, modified, 11);
                if (ctx.getPlayer() != null) stack.hurtAndBreak(1, ctx.getPlayer(), p -> p.broadcastBreakEvent(ctx.getHand()));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (!level.isClientSide && ToolData.has(stack)) Traits.inventoryTick(stack, level, holder);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) return super.getAttributeModifiers(slot, stack);
        var s = ToolData.stats(stack);
        boolean broken = ToolData.isBroken(stack);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        b.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", broken ? 0 : s.getFloat("Attack"), AttributeModifier.Operation.ADDITION));
        b.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier", s.contains("AtkSpd") ? s.getFloat("AtkSpd") : type.attackSpeed, AttributeModifier.Operation.ADDITION));
        return b.build();
    }

    // ------------------------------------------------------------------ enchantment-like bonuses

    @Override
    public int getEnchantmentLevel(ItemStack stack, Enchantment ench) {
        int base = super.getEnchantmentLevel(stack, ench);
        if (ench == Enchantments.BLOCK_FORTUNE || ench == Enchantments.MOB_LOOTING) return base + Traits.fortune(stack);
        if (ench == Enchantments.SILK_TOUCH) return Math.max(base, ToolData.modifier(stack, "silky"));
        return base;
    }

    @Override
    public Map<Enchantment, Integer> getAllEnchantments(ItemStack stack) {
        Map<Enchantment, Integer> map = new HashMap<>(super.getAllEnchantments(stack));
        int f = Traits.fortune(stack);
        if (f > 0) {
            map.merge(Enchantments.BLOCK_FORTUNE, f, Integer::sum);
            map.merge(Enchantments.MOB_LOOTING, f, Integer::sum);
        }
        if (ToolData.modifier(stack, "silky") > 0) map.put(Enchantments.SILK_TOUCH, 1);
        return map;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return true;
    }

    // ------------------------------------------------------------------ display

    @Override
    public Component getName(ItemStack stack) {
        List<ToolMaterial> mats = ToolData.materials(stack);
        Component tool = Component.translatable("tool.ls_toolsmith." + type.id);
        if (mats.isEmpty()) return tool;
        return Component.translatable("tool.ls_toolsmith.named", mats.get(0).displayName(), tool);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        if (!ToolData.has(stack)) return;
        if (ToolData.isBroken(stack)) tip.add(Component.translatable("tooltip.ls_toolsmith.broken").withStyle(ChatFormatting.RED));
        var s = ToolData.stats(stack);
        int lvl = ToolData.level(stack), next = ToolData.nextLevelXp(stack);
        tip.add(Component.translatable("tooltip.ls_toolsmith.level", lvl, next < 0 ? "max" : ToolData.xp(stack) + "/" + next).withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable("tooltip.ls_toolsmith.durability", stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage()).withStyle(ChatFormatting.GRAY));
        if (type != ToolType.SWORD) {
            tip.add(Component.translatable("tooltip.ls_toolsmith.speed", String.format("%.2f", s.getFloat("Speed"))).withStyle(ChatFormatting.GRAY));
            tip.add(Component.translatable("tooltip.ls_toolsmith.tier", Component.translatable("tier.ls_toolsmith." + s.getInt("Tier"))).withStyle(ChatFormatting.GRAY));
        }
        tip.add(Component.translatable("tooltip.ls_toolsmith.slots", ToolData.freeSlots(stack), ToolData.totalSlots(stack)).withStyle(ChatFormatting.GRAY));
        for (Map.Entry<String, Integer> e : ToolData.traits(stack).entrySet()) {
            tip.add(Component.translatable("trait.ls_toolsmith." + e.getKey()).append(e.getValue() > 1 ? " " + roman(e.getValue()) : "")
                    .withStyle(ChatFormatting.AQUA));
            if (flag.isAdvanced() || ToolsmithSetup.SHIFT.getAsBoolean()) {
                tip.add(Component.literal("  ").append(Component.translatable("trait.ls_toolsmith." + e.getKey() + ".desc")).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        for (Map.Entry<String, Integer> e : ToolData.modifiers(stack).entrySet()) {
            tip.add(Component.translatable("modifier.ls_toolsmith." + e.getKey()).append(" " + roman(e.getValue())).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        List<ToolMaterial> mats = ToolData.materials(stack);
        for (int i = 0; i < Math.min(mats.size(), type.parts.size()); i++) {
            final int color = mats.get(i).color();
            tip.add(Component.literal("  ").append(type.parts.get(i).displayName()).append(": ").append(mats.get(i).displayName())
                    .withStyle(s2 -> s2.withColor(color)));
        }
        if (!ToolsmithSetup.SHIFT.getAsBoolean()) tip.add(Component.translatable("tooltip.ls_toolsmith.shift").withStyle(ChatFormatting.DARK_GRAY));
    }

    static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }
}
