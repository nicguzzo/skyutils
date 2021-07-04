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

import net.nicguzzo.SkyutilsMod;

@Mixin(GeneratorOptions.class)
class GeneratorOptionsMixin {

    @Inject(method = "fromProperties", at = @At("RETURN"), cancellable = true)
    private static void fromProperties(DynamicRegistryManager dynamicRegistryManager, Properties properties,
            CallbackInfoReturnable<GeneratorOptions> cir) {
        String string4 = (String) properties.get("level-type");

        if (string4.hashCode() == 1279229352) {// level-type=skyblock_island
            String string3 = (String) properties.get("generate-structures");
            boolean bl = string3 == null || Boolean.parseBoolean(string3);
            long l = (new Random()).nextLong();
            String string2 = (String) properties.get("level-seed");
            if (!string2.isEmpty()) {
                try {
                    long m = Long.parseLong(string2);
                    if (m != 0L) {
                        l = m;
                    }
                } catch (NumberFormatException var18) {
                    l = (long) string2.hashCode();
                }
            }
            Registry<DimensionType> registry = dynamicRegistryManager.get(Registry.DIMENSION_TYPE_KEY);
            Registry<Biome> registry2 = dynamicRegistryManager.get(Registry.BIOME_KEY);
            Registry<ChunkGeneratorSettings> registry3 = dynamicRegistryManager.get(Registry.CHUNK_GENERATOR_SETTINGS_KEY);
            SimpleRegistry<DimensionOptions> simpleRegistry = DimensionType.createDefaultDimensionOptions(registry,
                    registry2, registry3, l);

            BiomeSource bs = new VanillaLayeredBiomeSource(l, false, false, registry2);
            ChunkGenerator chunk_generator = new SkyblockChunkGenerator(bs, l,
                    () -> registry3.get(ChunkGeneratorSettings.FLOATING_ISLANDS));

            cir.setReturnValue(new GeneratorOptions(l, bl, true,
                    GeneratorOptions.getRegistryWithReplacedOverworldGenerator(registry, simpleRegistry, chunk_generator)));
        }else{
            SkyutilsMod.is_skyblock=false;
        }
    }

}