package net.nicguzzo.condenser;

import net.fabricmc.fabric.api.block.FabricBlockSettings;
import net.fabricmc.fabric.api.tools.FabricToolTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidDrainable;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.IWorld;

public class CondenserBlock extends HorizontalFacingBlock implements BlockEntityProvider, FluidDrainable {
  
  public CondenserBlock() {
    super(FabricBlockSettings.of(Material.CARPET).breakByHand(true).breakByTool(FabricToolTags.AXES).nonOpaque()
        .sounds(BlockSoundGroup.WOOD).strength(2.0f, 1.0f).build());
    setDefaultState(this.stateManager.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> stateManager) {
    stateManager.add(Properties.HORIZONTAL_FACING);
  }

  @Override
  public BlockState getPlacementState(ItemPlacementContext ctx) {
    return (BlockState) this.getDefaultState().with(FACING, ctx.getPlayerFacing());
  }

  @Override
  public BlockEntity createBlockEntity(BlockView blockView) {
    return new CondenserEntity();
  }

  @Override
  public Fluid tryDrainFluid(IWorld world, BlockPos pos, BlockState state) {
    if (!world.isClient()) {
      BlockEntity entity=world.getBlockEntity(pos);
      if ( entity instanceof CondenserEntity) {
        CondenserEntity c = (CondenserEntity) entity;
        if (c.getLevel() == 8) {
          c.empty();
          world.playSound((PlayerEntity)null,pos,SoundEvents.ITEM_BUCKET_FILL,SoundCategory.BLOCKS, 1.0F, 1.0F);
          return Fluids.WATER;    
        }
      }
      
    }
    return Fluids.EMPTY;
    //return Fluids.WATER;
  }

}
