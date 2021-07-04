package net.nicguzzo.kiln;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.nicguzzo.SkyutilsMod;

public class KilnBlock extends BlockWithEntity {

    public static final DirectionProperty FACING;

    // public static final BooleanProperty LIT;
    public KilnBlock(Settings settings) {
        super(settings);
        this.setDefaultState((BlockState) (this.stateManager.getDefaultState().with(FACING, Direction.NORTH)));
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new KilnBlockEntity(pos, state);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        if (itemStack.hasCustomName()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof KilnBlockEntity) {
                ((KilnBlockEntity) blockEntity).setCustomName(itemStack.getName());
            }
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
            BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof KilnBlockEntity) {
                System.out.println("open!");

                NamedScreenHandlerFactory screenHandlerFactory = state.createScreenHandlerFactory(world, pos);

                if (screenHandlerFactory != null) {
                    // With this call the server will request the client to open the appropriate
                    // Screenhandler
                    player.openHandledScreen(screenHandlerFactory);
                }
                /*
                 * ContainerProviderRegistry.INSTANCE.openContainer(SkyutilsMod.KILN, player,
                 * buf -> buf.writeBlockPos(pos));
                 */
            }
        }
        return ActionResult.SUCCESS;
    }

    // Scatter the items in the chest when it is removed.
    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        // if (state.getBlock() != newState.getBlock())
        {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof KilnBlockEntity) {
                ItemScatterer.spawn(world, (BlockPos) pos, (Inventory) ((KilnBlockEntity) blockEntity));
                // world.updateHorizontalAdjacent(pos, this);
                world.updateNeighbors(pos, this);
            }
            super.onBreak(world, pos, state, player);
        }
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return ScreenHandler.calculateComparatorOutput(world.getBlockEntity(pos));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return (BlockState) this.getDefaultState().with(FACING, ctx.getPlayerFacing().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return (BlockState) state.with(FACING, rotation.rotate((Direction) state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation((Direction) state.get(FACING)));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
            BlockEntityType<T> type) {
        return !world.isClient ? checkType(type, SkyutilsMod.KILN_ENTITY, KilnBlockEntity::tick) : null;
    }

    static {
        FACING = HorizontalFacingBlock.FACING;
        // LIT = RedstoneTorchBlock.LIT;
    }

}