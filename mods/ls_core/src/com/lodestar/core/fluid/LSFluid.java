package com.lodestar.core.fluid;

import com.lodestar.core.registry.LSRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/** Registers a complete tinted fluid: type, source, flowing, optional bucket and in-world block. */
public class LSFluid {
    private static final ResourceLocation STILL = new ResourceLocation("minecraft", "block/water_still");
    private static final ResourceLocation FLOW = new ResourceLocation("minecraft", "block/water_flow");
    private static final ResourceLocation LAVA_STILL = new ResourceLocation("minecraft", "block/lava_still");
    private static final ResourceLocation LAVA_FLOW = new ResourceLocation("minecraft", "block/lava_flow");

    public final String name;
    public final int tint;
    public final RegistryObject<FluidType> type;
    public final RegistryObject<ForgeFlowingFluid.Source> source;
    public final RegistryObject<ForgeFlowingFluid.Flowing> flowing;
    @Nullable
    public final RegistryObject<LiquidBlock> block;
    @Nullable
    public final RegistryObject<Item> bucket;
    private ForgeFlowingFluid.Properties props;

    public LSFluid(LSRegistry reg, String name, int tint, FluidType.Properties typeProps, boolean hasBlock, boolean hasBucket, @Nullable ResourceKey<CreativeModeTab> tab) {
        this(reg, name, tint, typeProps, hasBlock, hasBucket, tab, false);
    }

    /** Molten fluids glow, use the lava texture tinted to the metal's colour and set things on fire. */
    public static LSFluid molten(LSRegistry reg, String name, int tint, int temperature, @Nullable ResourceKey<CreativeModeTab> tab) {
        FluidType.Properties p = FluidType.Properties.create().density(3000).viscosity(6000).temperature(temperature).lightLevel(12)
                .canSwim(false).canDrown(false).canExtinguish(false).supportsBoating(false).canConvertToSource(false)
                .sound(net.minecraftforge.common.SoundActions.BUCKET_FILL, net.minecraft.sounds.SoundEvents.BUCKET_FILL_LAVA)
                .sound(net.minecraftforge.common.SoundActions.BUCKET_EMPTY, net.minecraft.sounds.SoundEvents.BUCKET_EMPTY_LAVA);
        return new LSFluid(reg, name, tint, p, true, true, tab, true);
    }

    public LSFluid(LSRegistry reg, String name, int tint, FluidType.Properties typeProps, boolean hasBlock, boolean hasBucket,
                   @Nullable ResourceKey<CreativeModeTab> tab, boolean molten) {
        this.name = name;
        this.tint = tint;
        final int color = tint;
        final ResourceLocation still = molten ? LAVA_STILL : STILL, flow = molten ? LAVA_FLOW : FLOW;
        this.type = reg.fluidTypes().register(name, () -> new FluidType(typeProps) {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {
                    @Override
                    public ResourceLocation getStillTexture() {
                        return still;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return flow;
                    }

                    @Override
                    public int getTintColor() {
                        return color;
                    }
                });
            }
        });
        this.source = reg.fluids().register(name, () -> new ForgeFlowingFluid.Source(props));
        this.flowing = reg.fluids().register("flowing_" + name, () -> new ForgeFlowingFluid.Flowing(props));
        if (hasBucket) {
            this.bucket = reg.item(name + "_bucket", tab, () -> new BucketItem(source, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
        } else {
            this.bucket = null;
        }
        if (hasBlock && molten) {
            this.block = reg.blockOnly(name, () -> new LiquidBlock(source, BlockBehaviour.Properties.copy(Blocks.LAVA).noLootTable()) {
                @Override
                @SuppressWarnings("deprecation")
                public void entityInside(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level,
                                         net.minecraft.core.BlockPos pos, net.minecraft.world.entity.Entity entity) {
                    if (!entity.fireImmune()) {
                        entity.setSecondsOnFire(8);
                        entity.hurt(level.damageSources().lava(), 4.0f);
                    }
                }
            });
        } else if (hasBlock) {
            this.block = reg.blockOnly(name, () -> new LiquidBlock(source, BlockBehaviour.Properties.copy(Blocks.WATER).noLootTable()));
        } else {
            this.block = null;
        }
        this.props = new ForgeFlowingFluid.Properties(type, source, flowing).slopeFindDistance(3).levelDecreasePerBlock(1).tickRate(8);
        if (bucket != null) props.bucket(bucket);
        if (block != null) props.block(block);
    }

    public static FluidType.Properties liquid(int density, int viscosity, int temperature) {
        return FluidType.Properties.create().density(density).viscosity(viscosity).temperature(temperature)
                .canSwim(true).canDrown(true).canExtinguish(false).supportsBoating(false);
    }

    public static FluidType.Properties gas(int temperature) {
        return FluidType.Properties.create().density(-500).viscosity(200).temperature(temperature)
                .canSwim(false).canDrown(false).canPushEntity(false);
    }

    public ForgeFlowingFluid.Source get() {
        return source.get();
    }
}
