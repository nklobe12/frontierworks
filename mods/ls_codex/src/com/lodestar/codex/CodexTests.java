package com.lodestar.codex;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.lodestar.core.data.PlayerData;
import com.lodestar.core.multiblock.MultiblockPattern;
import com.lodestar.core.test.SelfTest;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/** Server self-test: the first-join grant and the integrity of every mod's Codex chapter. */
final class CodexTests {
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private static final Set<String> TABS = Set.of("CORE", "MATERIALS", "INDUSTRY", "POWER", "LOGISTICS", "AUTOMATION", "TOOLS", "GEAR",
            "ALCHEMY", "EXPLORATION", "NATURE", "CREATURES", "DIMENSIONS", "SPACE", "FARMING", "BUILDING", "TRANSPORT");

    private CodexTests() {}

    static void register() {
        SelfTest.scenario("codex:first_join", 20, CodexTests::firstJoin);
        SelfTest.scenario("codex:chapters", 1, CodexTests::chapters);
    }

    private static int countCodex(FakePlayer p) {
        return p.getInventory().countItem(Content.FIELD_CODEX.get());
    }

    private static Supplier<String> firstJoin(ServerLevel level, BlockPos origin) {
        FakePlayer p = FakePlayerFactory.get(level, new GameProfile(UUID.fromString("6c6f6465-7374-6172-636f-646578746573"), "[LodestarCodexTest]"));
        p.getInventory().clearContent();
        PlayerData.get(p, CodexSetup.KEY).remove("Granted");
        boolean first = CodexSetup.grantOnce(p);
        int afterFirst = countCodex(p);
        boolean second = CodexSetup.grantOnce(p);
        int afterSecond = countCodex(p);
        boolean flag = PlayerData.get(p, CodexSetup.KEY).getBoolean("Granted");
        Optional<? extends Recipe<?>> recipe = level.getServer().getRecipeManager().byKey(new ResourceLocation("ls_codex", "field_codex"));
        boolean recipeOk = recipe.isPresent() && recipe.get().getResultItem(level.registryAccess()).is(Content.FIELD_CODEX.get());
        return () -> {
            LOG.info("codex:first_join first grant={} (codex count {}), second grant={} (count {}), flag={}, recipe={}",
                    first, afterFirst, second, afterSecond, flag, recipeOk);
            p.getInventory().clearContent();
            if (!first || afterFirst != 1) return "first join gave " + afterFirst + " codices";
            if (second || afterSecond != 1) return "second join gave another codex (" + afterSecond + ")";
            if (!flag) return "grant flag not stored in player data";
            if (!recipeOk) return "book + compass recipe missing";
            return null;
        };
    }

    /** Reads every Lodestar mod's chapter.json from its mod file and checks items, multiblocks and text. */
    private static Supplier<String> chapters(ServerLevel level, BlockPos origin) {
        List<String> problems = new ArrayList<>();
        int chapters = 0, pages = 0, links = 0, structures = 0;
        for (IModInfo mod : ModList.get().getMods()) {
            String id = mod.getModId();
            if (!id.startsWith("ls_")) continue;
            Path path = mod.getOwningFile().getFile().findResource("assets", id, "codex", "chapter.json");
            if (!Files.exists(path)) {
                problems.add(id + ": no codex chapter");
                continue;
            }
            CodexChapter ch;
            try (Reader r = Files.newBufferedReader(path)) {
                JsonElement el = JsonParser.parseReader(r);
                ch = CodexChapter.parse(el.getAsJsonObject(), id);
            } catch (Exception e) {
                problems.add(id + ": unreadable chapter (" + e.getMessage() + ")");
                continue;
            }
            chapters++;
            if (!ch.mod.equals(id)) problems.add(id + ": chapter names mod " + ch.mod);
            if (!TABS.contains(ch.tab)) problems.add(id + ": unknown tab " + ch.tab);
            if (ch.intro.isBlank()) problems.add(id + ": empty intro");
            for (CodexChapter.Page p : ch.pages) {
                pages++;
                if (p.text().isBlank()) problems.add(id + "/" + p.title() + ": empty text");
                for (String item : p.items()) {
                    links++;
                    if (item.startsWith("#")) {
                        TagKey<net.minecraft.world.item.Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation(item.substring(1)));
                        if (BuiltInRegistries.ITEM.getTag(tag).map(t -> t.size() == 0).orElse(true)) problems.add(id + "/" + p.title() + ": empty tag " + item);
                    } else if (!ResourceLocation.isValidResourceLocation(item) || !ForgeRegistries.ITEMS.containsKey(new ResourceLocation(item))) {
                        problems.add(id + "/" + p.title() + ": unknown item " + item);
                    }
                }
                if (p.multiblock() != null) {
                    structures++;
                    MultiblockPattern.Preview pv = MultiblockPattern.preview(p.multiblock());
                    if (pv == null) {
                        problems.add(id + "/" + p.title() + ": no preview registered for " + p.multiblock());
                    } else {
                        try {
                            MultiblockPattern pat = pv.pattern().get();
                            List<ItemStack> mats = pat.materialStacks(pv.controller().get());
                            LOG.info("codex:chapters structure {} is {}x{}x{} with {} kinds of block", p.multiblock(), pat.width, pat.height, pat.depth, mats.size());
                            if (mats.isEmpty()) problems.add(p.multiblock() + ": empty bill of materials");
                        } catch (Exception e) {
                            problems.add(p.multiblock() + ": preview failed (" + e + ")");
                        }
                    }
                }
            }
        }
        final int c = chapters, pg = pages, l = links, s = structures;
        return () -> {
            LOG.info("codex:chapters read {} chapters, {} pages, {} item links, {} structures, {} problems", c, pg, l, s, problems.size());
            for (String p : problems) LOG.warn("codex:chapters problem: {}", p);
            if (c == 0) return "no chapters found";
            return problems.isEmpty() ? null : problems.size() + " problems, first: " + problems.get(0);
        };
    }
}
