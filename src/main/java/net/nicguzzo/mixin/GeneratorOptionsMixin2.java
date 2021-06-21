package net.nicguzzo.mixin;

import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.nicguzzo.SkyblockChunkGenerator;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.SimpleRegistry;

import java.util.Properties;
import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.nicguzzo.SkyutilsClientMod;

@Mixin(GeneratorOptions.class)
class GeneratorOptionsMixin2 {

    @ModifyVariable(method = "<init>", at = @At("HEAD"), index = 4, ordinal = 1/* , print = true */)
    static private boolean injected(boolean b) {
        if (SkyutilsClientMod.skyblock) {
            System.out.println("force bonus chest!!!!");
            return true;
        } else {
            return b;
        }
    }
}