package net.nicguzzo;

import net.nicguzzo.kiln.KilnScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.minecraft.client.world.GeneratorType;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.util.registry.Registry;

import net.nicguzzo.mixin.GeneratorTypeAccessor;

public class SkyutilsClientMod implements ClientModInitializer {
	public static boolean skyblock = false;
	public static RegistryKey<ChunkGeneratorSettings> SKYBLOCK_FLOATING_ISLANDS;

	@Override
	public void onInitializeClient() {

		ScreenRegistry.register(SkyutilsMod.KILN_SCREEN_HANDLER, KilnScreen::new);

		GeneratorTypeAccessor.getValues().add(SKYBLOCK);

	}

	public static final GeneratorType SKYBLOCK = new GeneratorType("skyblock_island") {
		protected ChunkGenerator getChunkGenerator(Registry<Biome> biomeRegistry,
				Registry<ChunkGeneratorSettings> chunkGeneratorSettingsRegistry, long seed) {

			SkyutilsClientMod.skyblock = true;
			SkyutilsMod.is_skyblock=true;
			BiomeSource bs = new VanillaLayeredBiomeSource(seed, false, false, biomeRegistry);
			return new SkyblockChunkGenerator(bs, seed,
					() -> chunkGeneratorSettingsRegistry.get(ChunkGeneratorSettings.FLOATING_ISLANDS));

		}
	};
}
