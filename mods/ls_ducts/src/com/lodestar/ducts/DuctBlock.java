package com.lodestar.ducts;

import com.lodestar.core.item.WrenchItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A duct: connects to ducts of the same kind and to blocks that hold its kind of stuff. Each side shows NONE,
 * DUCT (another duct) or END (an inventory, tank or energy block). Wrench a side to cut or restore it; use an
 * attachment item on an END side to fit a servo, filter or retriever; use an attachment with an empty hand to
 * configure it.
 */
public class DuctBlock extends BaseEntityBlock {
    public enum Conn implements StringRepresentable {
        NONE, DUCT, END;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final Map<Direction, EnumProperty<Conn>> PROPS = new HashMap<>();

    static {
        for (Direction d : Direction.values()) PROPS.put(d, EnumProperty.create(d.getName(), Conn.class));
    }

    public final DuctKind kind;
    public final int tier;
    private final Supplier<? extends BlockEntityType<?>> type;
    private final float radius;
    private final Map<BlockState, VoxelShape> shapes = new java.util.concurrent.ConcurrentHashMap<>();

    public DuctBlock(Properties props, DuctKind kind, int tier, float radius, Supplier<? extends BlockEntityType<?>> type) {
        super(props);
        this.kind = kind;
        this.tier = tier;
        this.radius = radius;
        this.type = type;
        BlockState s = stateDefinition.any();
        for (EnumProperty<Conn> p : PROPS.values()) s = s.setValue(p, Conn.NONE);
        registerDefaultState(s);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        for (Direction d : Direction.values()) b.add(PROPS.get(d));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapes.computeIfAbsent(state, s -> {
            double a = 8 - radius * 16, b = 8 + radius * 16;
            VoxelShape v = Block.box(a, a, a, b, b, b);
            for (Direction d : Direction.values()) {
                if (s.getValue(PROPS.get(d)) == Conn.NONE) continue;
                VoxelShape arm = switch (d) {
                    case NORTH -> Block.box(a, a, 0, b, b, a);
                    case SOUTH -> Block.box(a, a, b, b, b, 16);
                    case WEST -> Block.box(0, a, a, a, b, b);
                    case EAST -> Block.box(b, a, a, 16, b, b);
                    case DOWN -> Block.box(a, 0, a, b, a, b);
                    case UP -> Block.box(a, b, a, b, 16, b);
                };
                v = Shapes.or(v, arm);
            }
            return v;
        });
    }

    /** Connection for a side, given the block entity's cut sides. */
    public Conn connection(LevelAccessor level, BlockPos pos, Direction d) {
        if (level.getBlockEntity(pos) instanceof DuctBE self && self.isCut(d)) return Conn.NONE;
        BlockPos np = pos.relative(d);
        BlockState ns = level.getBlockState(np);
        BlockEntity nbe = level.getBlockEntity(np);
        if (ns.getBlock() instanceof DuctBlock other && other.kind == kind) {
            if (nbe instanceof DuctBE od && od.isCut(d.getOpposite())) return Conn.NONE;
            return Conn.DUCT;
        }
        if (kind.accepts(nbe, d.getOpposite())) return Conn.END;
        return Conn.NONE;
    }

    public BlockState computeState(LevelAccessor level, BlockPos pos, BlockState state) {
        for (Direction d : Direction.values()) state = state.setValue(PROPS.get(d), connection(level, pos, d));
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return computeState(ctx.getLevel(), ctx.getClickedPos(), defaultBlockState());
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction dir, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        BlockState ns = state.setValue(PROPS.get(dir), connection(level, pos, dir));
        if (ns != state && level.getBlockEntity(pos) instanceof DuctBE be) be.connectionsChanged();
        return ns;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        BlockState ns = computeState(level, pos, state);
        if (ns != state) level.setBlock(pos, ns, 3);
        if (level.getBlockEntity(pos) instanceof DuctBE be) be.connectionsChanged();
    }

    /** Which side of the duct a hit landed on (the arm pointing that way, or the clicked face of the core). */
    public static Direction sideOf(BlockHitResult hit, BlockPos pos) {
        Vec3 v = hit.getLocation().subtract(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        double ax = Math.abs(v.x), ay = Math.abs(v.y), az = Math.abs(v.z);
        if (Math.max(ax, Math.max(ay, az)) < 0.2) return hit.getDirection();
        if (ax >= ay && ax >= az) return v.x > 0 ? Direction.EAST : Direction.WEST;
        if (ay >= az) return v.y > 0 ? Direction.UP : Direction.DOWN;
        return v.z > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DuctBE be)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        Direction side = sideOf(hit, pos);
        if (held.getItem() instanceof WrenchItem) {
            if (!level.isClientSide) {
                be.toggleCut(side);
                level.setBlock(pos, computeState(level, pos, state), 3);
                BlockPos np = pos.relative(side);
                BlockState nst = level.getBlockState(np);
                if (nst.getBlock() instanceof DuctBlock nd) level.setBlock(np, nd.computeState(level, np, nst), 3);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (held.getItem() instanceof AttachmentItem ai && state.getValue(PROPS.get(side)) == Conn.END && kind != DuctKind.ENERGY) {
            if (be.attachment(side) == null && ai.fits(kind)) {
                if (!level.isClientSide) {
                    be.attach(side, ai.type);
                    if (!player.isCreative()) held.shrink(1);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        if (held.isEmpty() && be.attachment(side) != null) {
            if (player.isShiftKeyDown()) {
                if (!level.isClientSide) {
                    ItemStack back = be.detach(side);
                    if (!player.addItem(back)) player.drop(back, false);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (player instanceof ServerPlayer sp) {
                NetworkHooks.openScreen(sp, be.attachmentMenu(side), buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeVarInt(side.get3DDataValue());
                });
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DuctBE be) be.dropAll();
        super.onRemove(state, level, pos, newState, moving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> t) {
        if (level.isClientSide) return (l, p, s, be) -> {
            if (be instanceof DuctBE d) d.tickClient();
        };
        return (l, p, s, be) -> {
            if (be instanceof DuctBE d) d.baseServerTick();
        };
    }
}
