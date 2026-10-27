package com.frontierworks.core.fluid;

import com.frontierworks.core.registry.FWRegistry;
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
public class FWFluid {
    private static final ResourceLocation STILL = new ResourceLocation("minecraft", "block/water_still");
    private static final ResourceLocation FLOW = new ResourceLocation("minecraft", "block/water_flow");

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

    public FWFluid(FWRegistry reg, String name, int tint, FluidType.Properties typeProps, boolean hasBlock, boolean hasBucket, @Nullable ResourceKey<CreativeModeTab> tab) {
        this.name = name;
        this.tint = tint;
        final int color = tint;
        this.type = reg.fluidTypes().register(name, () -> new FluidType(typeProps) {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {
                    @Override
                    public ResourceLocation getStillTexture() {
                        return STILL;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return FLOW;
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
        if (hasBlock) {
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
