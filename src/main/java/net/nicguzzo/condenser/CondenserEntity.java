package net.nicguzzo.condenser;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.nicguzzo.SkyutilsMod;

public class CondenserEntity extends BlockEntity implements Tickable {

  // Store the current value of the number
  private int time = 0;
  private int level = 0;

  public CondenserEntity() {
    super(SkyutilsMod.CONDENSER_ENTITY);
  }

  public int getLevel(){
    return level;
  }
  public void empty(){
    level = 0;
    time = 0;
  }
  @Override
  public CompoundTag toTag(CompoundTag tag) {
    super.toTag(tag);

    // Save the current value of the number to the tag
    tag.putInt("number", time);
    tag.putInt("level", level);

    return tag;
  }

  @Override
  public void fromTag(CompoundTag tag) {
    super.fromTag(tag);
    time = tag.getInt("number");
    level = tag.getInt("level");
  }

 @Override
 public void tick() {
   if (!this.world.isClient) {
     int t = 24000;
     int d = t/8;
     if (time < t) {
       time++;
       Biome biome=this.world.getBiome(this.getPos());
       if (!(biome == Biomes.BADLANDS||
            biome == Biomes.BADLANDS_PLATEAU||
            biome == Biomes.DESERT||
            biome == Biomes.DESERT_HILLS||
           biome == Biomes.DESERT_LAKES)) {
         if (time % d == 0) {
           level ++;
           System.out.println("condenser level " + level);
         }
       }
       
       if (time == t) {
         /*BlockPos pos=this.getPos();
         BlockState state = this.world.getBlockState(pos);
         Block block = state.getBlock();
         if (block instanceof CondenserBlock) {           
           Direction dir = (Direction) state.get(HorizontalFacingBlock.FACING);
           BlockState state2 = this.world.getBlockState(pos.offset(dir, 1));
           if (state2.isAir()) {
             
           }
         }*/
       }
       markDirty();
     }/* else {
       time = 0;
     }*/
     
     //System.out.println("condenser tick " + time);
   }

 }
}