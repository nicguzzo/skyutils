package net.nicguzzo.mixin;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.util.registry.Registry;

import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.nicguzzo.SkyblockNetherChunkGenerator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(DimensionType.class)
public abstract class NetherMixin {

    @Inject(method = "createNetherGenerator", at = @At("HEAD"), cancellable = true)
    private static void createNetherGenerator(Registry<Biome> biomeRegistry, Registry<ChunkGeneratorSettings> chunkGeneratorSettingsRegistry, long seed,
    CallbackInfoReturnable<ChunkGenerator> cir) {
        

        BiomeSource bs=MultiNoiseBiomeSource.Preset.NETHER.getBiomeSource(biomeRegistry, seed);
        //BiomeSource bs = new VanillaLayeredBiomeSource(seed, false, false, biomeRegistry);
        SkyblockNetherChunkGenerator chunk_generator= new SkyblockNetherChunkGenerator(bs, seed, () -> {
            return (ChunkGeneratorSettings)chunkGeneratorSettingsRegistry.getOrThrow(ChunkGeneratorSettings.NETHER);
            });
        cir.setReturnValue((ChunkGenerator)chunk_generator);
            
    }
}
