package com.lodestar.core.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A fixed-shape multiblock described as layers of characters (bottom layer first; each layer is rows from
 * back to front, characters left to right as seen from the front). The controller character marks the
 * block that checks the structure; the structure faces the same way as the controller. ' ' means any block,
 * '_' means air. {@link #match} tries the controller's facing and returns every position by character.
 */
public class MultiblockPattern {
    public static final char ANY = ' ', AIR = '_';

    public static final class Key {
        final Predicate<BlockState> test;
        /** block shown in previews and counted in the bill of materials */
        final Supplier<Block> display;
        final String description;

        Key(Predicate<BlockState> test, Supplier<Block> display, String description) {
            this.test = test;
            this.display = display;
            this.description = description;
        }
    }

    public final String id;
    private final String[][] layers;
    private final Map<Character, Key> keys;
    private final char controller;
    public final int width, height, depth;
    private final int cx, cy, cz;

    private MultiblockPattern(String id, String[][] layers, Map<Character, Key> keys, char controller) {
        this.id = id;
        this.layers = layers;
        this.keys = keys;
        this.controller = controller;
        this.height = layers.length;
        this.depth = layers[0].length;
        this.width = layers[0][0].length();
        int fx = -1, fy = -1, fz = -1;
        for (int y = 0; y < height; y++)
            for (int z = 0; z < depth; z++)
                for (int x = 0; x < width; x++)
                    if (layers[y][z].charAt(x) == controller) {
                        fx = x;
                        fy = y;
                        fz = z;
                    }
        if (fx < 0) throw new IllegalArgumentException("multiblock " + id + " has no controller '" + controller + "'");
        cx = fx;
        cy = fy;
        cz = fz;
    }

    public char charAt(int x, int y, int z) {
        return layers[y][z].charAt(x);
    }

    /** World position of local (x, y, z) when the controller at ctrl faces the given direction. */
    public BlockPos toWorld(BlockPos ctrl, Direction facing, int x, int y, int z) {
        int dx = x - cx, dy = y - cy, dz = z - cz;
        // local frame: +x = right as seen from the front, +z = towards the front (facing)
        Direction right = facing.getCounterClockWise();
        return ctrl.offset(right.getStepX() * dx + facing.getStepX() * dz, dy, right.getStepZ() * dx + facing.getStepZ() * dz);
    }

    public static final class Match {
        public final Direction facing;
        public final Map<Character, List<BlockPos>> positions;
        public final List<BlockPos> all;

        Match(Direction facing, Map<Character, List<BlockPos>> positions, List<BlockPos> all) {
            this.facing = facing;
            this.positions = positions;
            this.all = all;
        }

        public List<BlockPos> get(char c) {
            return positions.getOrDefault(c, List.of());
        }
    }

    /** Checks the structure around a controller facing {@code facing}. Returns null if anything is wrong. */
    @Nullable
    public Match match(Level level, BlockPos ctrl, Direction facing) {
        Map<Character, List<BlockPos>> pos = new HashMap<>();
        List<BlockPos> all = new ArrayList<>();
        for (int y = 0; y < height; y++)
            for (int z = 0; z < depth; z++)
                for (int x = 0; x < width; x++) {
                    char c = layers[y][z].charAt(x);
                    if (c == ANY) continue;
                    BlockPos p = toWorld(ctrl, facing, x, y, z);
                    if (!level.isLoaded(p)) return null;
                    BlockState st = level.getBlockState(p);
                    if (c == AIR) {
                        if (!st.isAir()) return null;
                        continue;
                    }
                    if (c != controller) {
                        Key k = keys.get(c);
                        if (k == null || !k.test.test(st)) return null;
                    }
                    pos.computeIfAbsent(c, q -> new ArrayList<>()).add(p);
                    all.add(p);
                }
        return new Match(facing, pos, all);
    }

    /** First mismatching position (for "missing block at x y z" hints), or null if the structure is complete. */
    @Nullable
    public BlockPos firstProblem(Level level, BlockPos ctrl, Direction facing) {
        for (int y = 0; y < height; y++)
            for (int z = 0; z < depth; z++)
                for (int x = 0; x < width; x++) {
                    char c = layers[y][z].charAt(x);
                    if (c == ANY || c == controller) continue;
                    BlockPos p = toWorld(ctrl, facing, x, y, z);
                    BlockState st = level.getBlockState(p);
                    if (c == AIR ? !st.isAir() : !keys.get(c).test.test(st)) return p;
                }
        return null;
    }

    /** Describes what goes at a position, for hints ("Seared Bricks"). */
    public String describe(Level level, BlockPos ctrl, Direction facing, BlockPos p) {
        for (int y = 0; y < height; y++)
            for (int z = 0; z < depth; z++)
                for (int x = 0; x < width; x++)
                    if (toWorld(ctrl, facing, x, y, z).equals(p)) {
                        char c = layers[y][z].charAt(x);
                        if (c == AIR) return "air";
                        Key k = keys.get(c);
                        return k == null ? "?" : k.description;
                    }
        return "?";
    }

    /** Bill of materials for previews: display block and count per key (controller included). */
    public Map<Block, Integer> billOfMaterials(Block controllerBlock) {
        Map<Block, Integer> out = new LinkedHashMap<>();
        for (int y = 0; y < height; y++)
            for (int z = 0; z < depth; z++)
                for (int x = 0; x < width; x++) {
                    char c = layers[y][z].charAt(x);
                    if (c == ANY || c == AIR) continue;
                    Block b = c == controller ? controllerBlock : keys.get(c).display.get();
                    out.merge(b, 1, Integer::sum);
                }
        return out;
    }

    /** Display block at local coordinates (null for air/any), for structure previews. */
    @Nullable
    public Block displayAt(int x, int y, int z, Block controllerBlock) {
        char c = layers[y][z].charAt(x);
        if (c == ANY || c == AIR) return null;
        if (c == controller) return controllerBlock;
        return keys.get(c).display.get();
    }

    public List<ItemStack> materialStacks(Block controllerBlock) {
        List<ItemStack> list = new ArrayList<>();
        billOfMaterials(controllerBlock).forEach((b, n) -> list.add(new ItemStack(b, n)));
        return list;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private final List<String[]> layers = new ArrayList<>();
        private final Map<Character, Key> keys = new HashMap<>();
        private char controller = 'C';

        Builder(String id) {
            this.id = id;
        }

        /** Adds a layer (rows back to front). Call bottom layer first. */
        public Builder layer(String... rows) {
            layers.add(rows);
            return this;
        }

        public Builder controller(char c) {
            controller = c;
            return this;
        }

        public Builder key(char c, Supplier<Block> block) {
            keys.put(c, new Key(s -> s.is(block.get()), block, block.get().getDescriptionId()));
            return this;
        }

        /** Any of several blocks; the first is shown in previews. */
        @SafeVarargs
        public final Builder keyAny(char c, Supplier<? extends Block>... blocks) {
            keys.put(c, new Key(s -> {
                for (Supplier<? extends Block> b : blocks) if (s.is(b.get())) return true;
                return false;
            }, () -> blocks[0].get(), blocks[0].get().getDescriptionId()));
            return this;
        }

        public Builder keyTag(char c, String tag, Supplier<Block> display) {
            TagKey<Block> t = BlockTags.create(new ResourceLocation(tag));
            keys.put(c, new Key(s -> s.is(t), display, "#" + tag));
            return this;
        }

        public Builder keyTest(char c, Predicate<BlockState> test, Supplier<Block> display, String description) {
            keys.put(c, new Key(test, display, description));
            return this;
        }

        public MultiblockPattern build() {
            String[][] arr = layers.toArray(new String[0][]);
            int w = arr[0][0].length();
            for (String[] l : arr) {
                if (l.length != arr[0].length) throw new IllegalArgumentException(id + ": layers differ in depth");
                for (String r : l) if (r.length() != w) throw new IllegalArgumentException(id + ": rows differ in width");
            }
            return new MultiblockPattern(id, arr, keys, controller);
        }
    }

    public static Supplier<Block> vanilla(String id) {
        return () -> BuiltInRegistries.BLOCK.get(new ResourceLocation(id));
    }

    public static Supplier<Block> air() {
        return () -> Blocks.AIR;
    }
}
