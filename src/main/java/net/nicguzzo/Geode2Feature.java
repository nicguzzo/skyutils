package net.nicguzzo;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BuddingAmethystBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.GeodeCrackConfig;
import net.minecraft.world.gen.feature.GeodeFeatureConfig;
import net.minecraft.world.gen.feature.GeodeLayerConfig;
import net.minecraft.world.gen.feature.GeodeLayerThicknessConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class Geode2Feature extends Feature<GeodeFeatureConfig> {
    private static final Direction[] DIRECTIONS = Direction.values();
    public Geode2Feature(Codec<GeodeFeatureConfig> config) {
      super(config);
    }
   
    @Override
	public boolean generate(FeatureContext<GeodeFeatureConfig> context) {    
      if(!SkyutilsMod.is_skyblock){
         return false;
      }
        BlockPos blockPos=context.getOrigin().up(10);
        StructureWorldAccess structureWorldAccess=context.getWorld();
        GeodeFeatureConfig geodeFeatureConfig=(GeodeFeatureConfig)context.getConfig();
        Random random = structureWorldAccess.getRandom();
        int i = geodeFeatureConfig.minGenOffset;
        int j = geodeFeatureConfig.maxGenOffset;
        List<Pair<BlockPos, Integer>> list = Lists.newLinkedList();
        int k = geodeFeatureConfig.distributionPoints.get(random);
        ChunkRandom chunkRandom = new ChunkRandom(structureWorldAccess.getSeed());
        DoublePerlinNoiseSampler doublePerlinNoiseSampler = DoublePerlinNoiseSampler.create(chunkRandom, -4, 1.0D);
        List<BlockPos> list2 = Lists.newLinkedList();
        double d = (double)k / (double)geodeFeatureConfig.outerWallDistance.getMax();
        GeodeLayerThicknessConfig geodeLayerThicknessConfig = geodeFeatureConfig.layerThicknessConfig;
        GeodeLayerConfig geodeLayerConfig = geodeFeatureConfig.layerConfig;
        GeodeCrackConfig geodeCrackConfig = geodeFeatureConfig.crackConfig;
        double e = 1.0D / Math.sqrt(geodeLayerThicknessConfig.filling);
        double f = 1.0D / Math.sqrt(geodeLayerThicknessConfig.innerLayer + d);
        double g = 1.0D / Math.sqrt(geodeLayerThicknessConfig.middleLayer + d);
        double h = 1.0D / Math.sqrt(geodeLayerThicknessConfig.outerLayer + d);
        double l = 1.0D / Math.sqrt(geodeCrackConfig.baseCrackSize + random.nextDouble() / 2.0D + (k > 3 ? d : 0.0D));
        boolean bl = (double)random.nextFloat() < geodeCrackConfig.generateCrackChance;
        int m = 0;
  
        int r;
        int s;
        BlockPos blockPos6;
        BlockState blockState2;
        for(r = 0; r < k; ++r) {
           s = geodeFeatureConfig.outerWallDistance.get(random);
           int p = geodeFeatureConfig.outerWallDistance.get(random);
           int q = geodeFeatureConfig.outerWallDistance.get(random);
           blockPos6 = blockPos.add(s, p, q);
  
           list.add(Pair.of(blockPos6, geodeFeatureConfig.pointOffset.get(random)));
        }
  
        if (bl) {
           r = random.nextInt(4);
           s = k * 2 + 1;
           if (r == 0) {
              list2.add(blockPos.add(s, 7, 0));
              list2.add(blockPos.add(s, 5, 0));
              list2.add(blockPos.add(s, 1, 0));
           } else if (r == 1) {
              list2.add(blockPos.add(0, 7, s));
              list2.add(blockPos.add(0, 5, s));
              list2.add(blockPos.add(0, 1, s));
           } else if (r == 2) {
              list2.add(blockPos.add(s, 7, s));
              list2.add(blockPos.add(s, 5, s));
              list2.add(blockPos.add(s, 1, s));
           } else {
              list2.add(blockPos.add(0, 7, 0));
              list2.add(blockPos.add(0, 5, 0));
              list2.add(blockPos.add(0, 1, 0));
           }
        }
  
        List<BlockPos> list3 = Lists.newArrayList();
        Predicate<BlockState> predicate = notInBlockTagPredicate(geodeFeatureConfig.layerConfig.cannotReplace);
        Iterator var48 = BlockPos.iterate(blockPos.add(i, i, i), blockPos.add(j, j, j)).iterator();
  
        while(true) {
           while(true) {
              double u;
              double v;
              BlockPos blockPos3;
              do {
                 if (!var48.hasNext()) {
                    List<BlockState> list4 = geodeLayerConfig.innerBlocks;
                    Iterator var51 = list3.iterator();
  
                    while(true) {
                       while(var51.hasNext()) {
                          blockPos6 = (BlockPos)var51.next();
                          blockState2 = (BlockState)Util.getRandom(list4, random);
                          Direction[] var53 = DIRECTIONS;
                          int var37 = var53.length;
  
                          for(int var54 = 0; var54 < var37; ++var54) {
                             Direction direction2 = var53[var54];
                             if (blockState2.contains(Properties.FACING)) {
                                blockState2 = (BlockState)blockState2.with(Properties.FACING, direction2);
                             }
  
                             BlockPos blockPos7 = blockPos6.offset(direction2);
                             BlockState blockState3 = structureWorldAccess.getBlockState(blockPos7);
                             if (blockState2.contains(Properties.WATERLOGGED)) {
                                blockState2 = (BlockState)blockState2.with(Properties.WATERLOGGED, blockState3.getFluidState().isStill());
                             }
  
                             if (BuddingAmethystBlock.canGrowIn(blockState3)) {
                                this.setBlockStateIf(structureWorldAccess, blockPos7, blockState2, predicate);
                                break;
                             }
                          }
                       }
  
                       return true;
                    }
                 }
  
                 blockPos3 = (BlockPos)var48.next();
                 double t = doublePerlinNoiseSampler.sample((double)blockPos3.getX(), (double)blockPos3.getY(), (double)blockPos3.getZ()) * geodeFeatureConfig.noiseMultiplier;
                 u = 0.0D;
                 v = 0.0D;
  
                 Iterator var40;
                 Pair pair;
                 for(var40 = list.iterator(); var40.hasNext(); u += MathHelper.fastInverseSqrt(blockPos3.getSquaredDistance((Vec3i)pair.getFirst()) + (double)(Integer)pair.getSecond()) + t) {
                    pair = (Pair)var40.next();
                 }
  
                 BlockPos blockPos4;
                 for(var40 = list2.iterator(); var40.hasNext(); v += MathHelper.fastInverseSqrt(blockPos3.getSquaredDistance(blockPos4) + (double)geodeCrackConfig.crackPointOffset) + t) {
                    blockPos4 = (BlockPos)var40.next();
                 }
              } while(u < h);
  
              if (bl && v >= l && u < e) {
                 this.setBlockStateIf(structureWorldAccess, blockPos3, Blocks.AIR.getDefaultState(), predicate);
                 Direction[] var56 = DIRECTIONS;
                 int var59 = var56.length;
  
                 for(int var42 = 0; var42 < var59; ++var42) {
                    Direction direction = var56[var42];
                    BlockPos blockPos5 = blockPos3.offset(direction);
                    FluidState fluidState = structureWorldAccess.getFluidState(blockPos5);
                    if (!fluidState.isEmpty()) {
                       structureWorldAccess.getFluidTickScheduler().schedule(blockPos5, fluidState.getFluid(), 0);
                    }
                 }
              } else if (u >= e) {
                 this.setBlockStateIf(structureWorldAccess, blockPos3, geodeLayerConfig.fillingProvider.getBlockState(random, blockPos3), predicate);
              } else if (u >= f) {
                 boolean bl2 = (double)random.nextFloat() < geodeFeatureConfig.useAlternateLayer0Chance;
                 if (bl2) {
                    this.setBlockStateIf(structureWorldAccess, blockPos3, geodeLayerConfig.alternateInnerLayerProvider.getBlockState(random, blockPos3), predicate);
                 } else {
                    this.setBlockStateIf(structureWorldAccess, blockPos3, geodeLayerConfig.innerLayerProvider.getBlockState(random, blockPos3), predicate);
                 }
  
                 if ((!geodeFeatureConfig.placementsRequireLayer0Alternate || bl2) && (double)random.nextFloat() < geodeFeatureConfig.usePotentialPlacementsChance) {
                    list3.add(blockPos3.toImmutable());
                 }
              } else if (u >= g) {
                 this.setBlockStateIf(structureWorldAccess, blockPos3, geodeLayerConfig.middleLayerProvider.getBlockState(random, blockPos3), predicate);
              } else if (u >= h) {
                 this.setBlockStateIf(structureWorldAccess, blockPos3, geodeLayerConfig.outerLayerProvider.getBlockState(random, blockPos3), predicate);
              }
           }
        }
     }
  }