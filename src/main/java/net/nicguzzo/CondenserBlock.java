package net.nicguzzo;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidDrainable;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;

import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;

import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class CondenserBlock extends HorizontalFacingBlock implements BlockEntityProvider, FluidDrainable {
    public static final IntProperty LEVEL = IntProperty.of("level", 0, 7);

    public CondenserBlock() {
        super(Settings.copy(Blocks.OAK_LOG).nonOpaque());
        setDefaultState(
                this.stateManager.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH).with(LEVEL, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> stateManager) {
        stateManager.add(Properties.HORIZONTAL_FACING);
        stateManager.add(LEVEL);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return (BlockState) this.getDefaultState().with(FACING, ctx.getPlayerFacing());
    }

    @Override
    public BlockEntity createBlockEntity(BlockView blockView) {
        return new CondenserEntity();
    }

    public void setLevel(World world, BlockPos pos, BlockState state, int level) {

        world.setBlockState(pos, (BlockState) state.with(LEVEL, MathHelper.clamp(level, 0, 7)), 2);
        // world.updateHorizontalAdjacent(pos, this);
        world.updateNeighbors(pos, this);
        // world.markDirty(pos, world.getBlockEntity(pos));
    }

    public void incLevel(World world, BlockPos pos, BlockState state) {
        int level = ((Integer) state.get(LEVEL)) + 1;
        world.setBlockState(pos, (BlockState) state.with(LEVEL, MathHelper.clamp(level, 0, 7)), 2);
        // world.updateHorizontalAdjacent(pos, this);
        world.updateNeighbors(pos, this);
    }

    public int getLevel(BlockState state) {
        return (Integer) state.get(LEVEL);
    }

    @Override
    public void rainTick(World world, BlockPos pos) {
        /*
         * System.out.println("condenser rainTick "); if (!world.isClient()) { //if
         * (world.random.nextInt(10) <= 3) { BlockState state =
         * world.getBlockState(pos); if ((Integer)state.get(LEVEL) < 7) {
         * world.setBlockState(pos, (BlockState) state.cycle(LEVEL), 2);
         * System.out.println("condenser level rain " + (Integer) state.get(LEVEL)); } }
         * }
         */
    }

    @Override
    public Fluid tryDrainFluid(WorldAccess world, BlockPos pos, BlockState state) {
        if (!world.isClient()) {
            int i = (Integer) state.get(LEVEL);
            System.out.println(" tryDrainFluid condenser level " + i);
            if (i == 7) {
                this.setLevel((World) world, pos, state, 0);
                CondenserEntity e = (CondenserEntity) world.getBlockEntity(pos);
                if (e != null) {
                    e.empty();
                }
                // ((World)world).markDirty(pos, world.getBlockEntity(pos));
                world.playSound((PlayerEntity) null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1.0F,
                        1.0F);
                return Fluids.WATER;
            }
        }
        return Fluids.EMPTY;
    }

}
