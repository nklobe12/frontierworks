package com.frontierworks.core.registry;

import com.frontierworks.core.recipe.MachineRecipe;
import com.frontierworks.core.recipe.MachineRecipeKind;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Per-mod registration helper. Deferred registers are created lazily on first use and
 * bound to the mod event bus as soon as {@link #register(IEventBus)} has been called.
 */
public class FWRegistry {
    public final String modid;
    private IEventBus bus;
    private final List<DeferredRegister<?>> created = new ArrayList<>();
    private final Map<ResourceKey<CreativeModeTab>, List<Supplier<? extends ItemLike>>> tabContents = new LinkedHashMap<>();

    private DeferredRegister<Block> blocks;
    private DeferredRegister<Item> items;
    private DeferredRegister<BlockEntityType<?>> blockEntities;
    private DeferredRegister<MenuType<?>> menus;
    private DeferredRegister<EntityType<?>> entities;
    private DeferredRegister<RecipeType<?>> recipeTypes;
    private DeferredRegister<RecipeSerializer<?>> recipeSerializers;
    private DeferredRegister<FluidType> fluidTypes;
    private DeferredRegister<Fluid> fluids;
    private DeferredRegister<MobEffect> effects;
    private DeferredRegister<Potion> potions;
    private DeferredRegister<Enchantment> enchantments;
    private DeferredRegister<SoundEvent> sounds;
    private DeferredRegister<Feature<?>> features;
    private DeferredRegister<Codec<? extends IGlobalLootModifier>> lootModifiers;
    private DeferredRegister<PoiType> poiTypes;
    private DeferredRegister<VillagerProfession> professions;
    private DeferredRegister<CreativeModeTab> tabs;

    public FWRegistry(String modid) {
        this.modid = modid;
    }

    private <T> DeferredRegister<T> make(DeferredRegister<T> reg) {
        created.add(reg);
        if (bus != null) {
            reg.register(bus);
        }
        return reg;
    }

    public void register(IEventBus bus) {
        this.bus = bus;
        for (DeferredRegister<?> reg : created) {
            reg.register(bus);
        }
        bus.addListener(this::onBuildTabs);
    }

    private void onBuildTabs(BuildCreativeModeTabContentsEvent event) {
        List<Supplier<? extends ItemLike>> list = tabContents.get(event.getTabKey());
        if (list == null) {
            return;
        }
        for (Supplier<? extends ItemLike> s : list) {
            ItemLike like = s.get();
            if (like != null && like.asItem() != net.minecraft.world.item.Items.AIR) {
                event.accept(like);
            }
        }
    }

    public ResourceLocation id(String path) {
        return new ResourceLocation(modid, path);
    }

    // ------------------------------------------------------------------ raw registers

    public DeferredRegister<Block> blocks() {
        if (blocks == null) blocks = make(DeferredRegister.create(ForgeRegistries.BLOCKS, modid));
        return blocks;
    }

    public DeferredRegister<Item> items() {
        if (items == null) items = make(DeferredRegister.create(ForgeRegistries.ITEMS, modid));
        return items;
    }

    public DeferredRegister<BlockEntityType<?>> blockEntities() {
        if (blockEntities == null) blockEntities = make(DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, modid));
        return blockEntities;
    }

    public DeferredRegister<MenuType<?>> menus() {
        if (menus == null) menus = make(DeferredRegister.create(ForgeRegistries.MENU_TYPES, modid));
        return menus;
    }

    public DeferredRegister<EntityType<?>> entities() {
        if (entities == null) entities = make(DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, modid));
        return entities;
    }

    public DeferredRegister<RecipeType<?>> recipeTypes() {
        if (recipeTypes == null) recipeTypes = make(DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, modid));
        return recipeTypes;
    }

    public DeferredRegister<RecipeSerializer<?>> recipeSerializers() {
        if (recipeSerializers == null) recipeSerializers = make(DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, modid));
        return recipeSerializers;
    }

    public DeferredRegister<FluidType> fluidTypes() {
        if (fluidTypes == null) fluidTypes = make(DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, modid));
        return fluidTypes;
    }

    public DeferredRegister<Fluid> fluids() {
        if (fluids == null) fluids = make(DeferredRegister.create(ForgeRegistries.FLUIDS, modid));
        return fluids;
    }

    public DeferredRegister<MobEffect> effects() {
        if (effects == null) effects = make(DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, modid));
        return effects;
    }

    public DeferredRegister<Potion> potions() {
        if (potions == null) potions = make(DeferredRegister.create(ForgeRegistries.POTIONS, modid));
        return potions;
    }

    public DeferredRegister<Enchantment> enchantments() {
        if (enchantments == null) enchantments = make(DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, modid));
        return enchantments;
    }

    public DeferredRegister<SoundEvent> sounds() {
        if (sounds == null) sounds = make(DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, modid));
        return sounds;
    }

    public DeferredRegister<Feature<?>> features() {
        if (features == null) features = make(DeferredRegister.create(ForgeRegistries.FEATURES, modid));
        return features;
    }

    public DeferredRegister<Codec<? extends IGlobalLootModifier>> lootModifiers() {
        if (lootModifiers == null) lootModifiers = make(DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, modid));
        return lootModifiers;
    }

    public DeferredRegister<PoiType> poiTypes() {
        if (poiTypes == null) poiTypes = make(DeferredRegister.create(ForgeRegistries.POI_TYPES, modid));
        return poiTypes;
    }

    public DeferredRegister<VillagerProfession> professions() {
        if (professions == null) professions = make(DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, modid));
        return professions;
    }

    public DeferredRegister<CreativeModeTab> tabs() {
        if (tabs == null) tabs = make(DeferredRegister.create(Registries.CREATIVE_MODE_TAB, modid));
        return tabs;
    }

    // ------------------------------------------------------------------ convenience

    public void addToTab(ResourceKey<CreativeModeTab> tab, Supplier<? extends ItemLike> item) {
        if (tab == null) {
            return;
        }
        tabContents.computeIfAbsent(tab, k -> new ArrayList<>()).add(item);
    }

    public <I extends Item> RegistryObject<I> item(String name, ResourceKey<CreativeModeTab> tab, Supplier<I> sup) {
        RegistryObject<I> ro = items().register(name, sup);
        addToTab(tab, ro);
        return ro;
    }

    public RegistryObject<Item> item(String name, ResourceKey<CreativeModeTab> tab) {
        return item(name, tab, () -> new Item(new Item.Properties()));
    }

    public <B extends Block> RegistryObject<B> blockOnly(String name, Supplier<B> sup) {
        return blocks().register(name, sup);
    }

    public <B extends Block> RegistryObject<B> block(String name, ResourceKey<CreativeModeTab> tab, Supplier<B> sup) {
        return block(name, tab, sup, b -> new BlockItem(b, new Item.Properties()));
    }

    public <B extends Block> RegistryObject<B> block(String name, ResourceKey<CreativeModeTab> tab, Supplier<B> sup, Function<B, ? extends Item> itemFactory) {
        RegistryObject<B> ro = blocks().register(name, sup);
        RegistryObject<Item> item = items().register(name, () -> itemFactory.apply(ro.get()));
        addToTab(tab, item);
        return ro;
    }

    @SafeVarargs
    public final <T extends BlockEntity> RegistryObject<BlockEntityType<T>> blockEntity(String name, BlockEntityType.BlockEntitySupplier<T> factory, Supplier<? extends Block>... validBlocks) {
        return blockEntities().register(name, () -> {
            Block[] arr = new Block[validBlocks.length];
            for (int i = 0; i < validBlocks.length; i++) {
                arr[i] = validBlocks[i].get();
            }
            return BlockEntityType.Builder.of(factory, arr).build(null);
        });
    }

    public <T extends Entity> RegistryObject<EntityType<T>> entity(String name, Supplier<EntityType.Builder<T>> builder) {
        return entities().register(name, () -> builder.get().build(modid + ":" + name));
    }

    public <M extends AbstractContainerMenu> RegistryObject<MenuType<M>> menu(String name, IContainerFactory<M> factory) {
        return menus().register(name, () -> IForgeMenuType.create(factory));
    }

    public MachineRecipeKind machineRecipe(String name) {
        final ResourceLocation rl = id(name);
        RegistryObject<RecipeType<MachineRecipe>> type = recipeTypes().register(name, () -> RecipeType.<MachineRecipe>simple(rl));
        MachineRecipeKind kind = new MachineRecipeKind(rl, type);
        RegistryObject<RecipeSerializer<MachineRecipe>> ser = recipeSerializers().register(name, () -> new MachineRecipe.Serializer(kind));
        kind.setSerializer(ser);
        return kind;
    }

    public RegistryObject<SoundEvent> sound(String name) {
        final ResourceLocation rl = id(name);
        return sounds().register(name, () -> SoundEvent.createVariableRangeEvent(rl));
    }
}
