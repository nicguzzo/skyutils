package net.nicguzzo.composter;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ComposterBlock;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldEvents;
import net.nicguzzo.mixin.ComposterMixin;
import org.jetbrains.annotations.Nullable;

public class Composter {
    static BlockState addToComposter(BlockState state, WorldAccess world, BlockPos pos, ItemStack item) {
        int i = (Integer)state.get(ComposterBlock.LEVEL);
        float f = 1.0f;//ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.getFloat(item.getItem());
        /*if ((i != 0 || !(f > 0.0F)) && !(world.getRandom().nextDouble() < (double)f)) {
            return state;
        } else {*/
            int j = i + 1;
            BlockState blockState = (BlockState)state.with(ComposterBlock.LEVEL, j);
            world.setBlockState(pos, blockState, Block.NOTIFY_ALL);
            if (j == 7) {
                world.getBlockTickScheduler().schedule(pos, state.getBlock(), 20);
            }

            return blockState;
        //}
    }
    public static boolean addToComposter(int level, BlockState state, WorldAccess world, BlockPos pos, ItemStack item) {

        System.out.println("composter level: " + level);
        int j = level + 1;
        world.setBlockState(pos, (BlockState) state.with(ComposterBlock.LEVEL, j), 3);
        if (j == 7) {
            world.getBlockTickScheduler().schedule(pos, state.getBlock(), 20);
        }
        return true;
    }
    static BlockState emptyComposter(BlockState state, WorldAccess world, BlockPos pos) {
        BlockState blockState = (BlockState)state.with(ComposterBlock.LEVEL, 0);
        world.setBlockState(pos, blockState, Block.NOTIFY_ALL);
        return blockState;
    }
    public static class FullComposterInventory extends SimpleInventory implements SidedInventory {
        private final BlockState state;
        private final WorldAccess world;
        private final BlockPos pos;
        private boolean dirty;

        public FullComposterInventory(BlockState state, WorldAccess world, BlockPos pos, ItemStack outputItem) {
            super(outputItem);
            this.state = state;
            this.world = world;
            this.pos = pos;
        }

        public int getMaxCountPerStack() {
            return 1;
        }

        public int[] getAvailableSlots(Direction side) {
            return side == Direction.DOWN ? new int[]{0} : new int[0];
        }

        public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
            return false;
        }

        public boolean canExtract(int slot, ItemStack stack, Direction dir) {
            return !this.dirty && dir == Direction.DOWN && stack.isOf(Items.GRASS_BLOCK);
        }

        public void markDirty() {
            Composter.emptyComposter(this.state, this.world, this.pos);
            this.dirty = true;
        }
    }

    public static class ComposterInventory extends SimpleInventory implements SidedInventory {
        private final BlockState state;
        private final WorldAccess world;
        private final BlockPos pos;
        private boolean dirty;

        public ComposterInventory(BlockState state, WorldAccess world, BlockPos pos) {
            super(1);
            this.state = state;
            this.world = world;
            this.pos = pos;
        }

        public int getMaxCountPerStack() {
            return 1;
        }

        public int[] getAvailableSlots(Direction side) {
            return side == Direction.UP ? new int[]{0} : new int[0];
        }

        public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
            return !this.dirty && dir == Direction.UP && ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.containsKey(stack.getItem());
        }

        public boolean canExtract(int slot, ItemStack stack, Direction dir) {
            return false;
        }

        public void markDirty() {
            ItemStack itemStack = this.getStack(0);
            if (!itemStack.isEmpty()) {
                this.dirty = true;
                BlockState blockState = Composter.addToComposter(this.state, this.world, this.pos, itemStack);
                this.world.syncWorldEvent(WorldEvents.COMPOSTER_USED, this.pos, blockState != this.state ? 1 : 0);
                this.removeStack(0);
            }
        }
    }

    public static class DummyInventory extends SimpleInventory implements SidedInventory {
        public DummyInventory() {
            super(0);
        }

        public int[] getAvailableSlots(Direction side) {
            return new int[0];
        }

        public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
            return false;
        }

        public boolean canExtract(int slot, ItemStack stack, Direction dir) {
            return false;
        }
    }
}
