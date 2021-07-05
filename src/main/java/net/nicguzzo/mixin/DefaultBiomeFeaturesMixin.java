package net.nicguzzo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.DefaultBiomeFeatures;
import net.nicguzzo.SkyutilsMod;

@Mixin(DefaultBiomeFeatures.class)
public class DefaultBiomeFeaturesMixin {
  @Inject(method = "addAmethystGeodes(Lnet/minecraft/world/biome/GenerationSettings$Builder;)V", at = @At("TAIL"))
  private static void addAmethystGeodes(GenerationSettings.Builder builder, CallbackInfo ci) {
    builder.feature(GenerationStep.Feature.UNDERGROUND_STRUCTURES, SkyutilsMod.GEODE2_CONFIGURED);
  }
}
