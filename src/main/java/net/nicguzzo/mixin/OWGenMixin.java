package net.nicguzzo.mixin;

import net.minecraft.world.World;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorConfig;

import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.OverworldDimension;

import net.minecraft.world.level.LevelGeneratorType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.nicguzzo.SkyutilsMod;

@Mixin(OverworldDimension.class)
public abstract class OWGenMixin extends Dimension {
  public OWGenMixin(World world_1, DimensionType dimensionType_1, float f) {
    super(world_1, dimensionType_1,f);
  }     
    @Inject(method = "createChunkGenerator()Lnet/minecraft/world/gen/chunk/ChunkGenerator;", at = @At("HEAD"), cancellable = true)
    private void createChunkGenerator(CallbackInfoReturnable<ChunkGenerator<? extends ChunkGeneratorConfig>> cir) {
        LevelGeneratorType type = this.world.getLevelProperties().getGeneratorType();
        if (type == SkyutilsMod.SKB_LEVEL_GENERATOR_TYPE) {
          
          cir.setReturnValue(SkyutilsMod.createOWGen(this.world));
          cir.cancel();
        }
    }
}

