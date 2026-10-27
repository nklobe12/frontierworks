package com.lodestar.lens.client;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Items the player pinned (A over an item). Kept in config/lodestar-lens-bookmarks.txt, one item NBT per line. */
public final class Bookmarks {
    private static final Logger LOG = LogUtils.getLogger();
    private static List<ItemStack> list;

    private Bookmarks() {}

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("lodestar-lens-bookmarks.txt");
    }

    public static List<ItemStack> list() {
        if (list == null) {
            list = new ArrayList<>();
            try {
                if (Files.exists(file())) {
                    for (String line : Files.readAllLines(file(), StandardCharsets.UTF_8)) {
                        if (line.isBlank()) continue;
                        try {
                            ItemStack s = ItemStack.of(TagParser.parseTag(line));
                            if (!s.isEmpty()) list.add(s);
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (Exception e) {
                LOG.warn("Lens: could not read bookmarks", e);
            }
        }
        return list;
    }

    public static boolean has(ItemStack s) {
        for (ItemStack b : list()) if (ItemStack.isSameItemSameTags(b, s)) return true;
        return false;
    }

    public static void toggle(ItemStack s) {
        if (s.isEmpty()) return;
        List<ItemStack> l = list();
        boolean removed = l.removeIf(b -> ItemStack.isSameItemSameTags(b, s));
        if (!removed) l.add(s.copyWithCount(1));
        save();
    }

    private static void save() {
        List<String> lines = new ArrayList<>();
        for (ItemStack s : list()) lines.add(s.save(new CompoundTag()).toString());
        try {
            Files.write(file(), lines, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.warn("Lens: could not save bookmarks", e);
        }
    }
}
