package net.nicguzzo;
import net.nicguzzo.kiln.KilnContainer;
import net.nicguzzo.kiln.KilnScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.ScreenProviderRegistry;
import net.minecraft.text.TranslatableText;
import net.minecraft.client.MinecraftClient;

public class SkyutilsClientMod implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ScreenProviderRegistry.INSTANCE.<KilnContainer>registerFactory(SkyutilsMod.KILN, 
		(container) -> new KilnScreen(container, MinecraftClient.getInstance().player.inventory, new TranslatableText(SkyutilsMod.KILN_BLOCK_TRANSLATION_KEY)));

	}

}
