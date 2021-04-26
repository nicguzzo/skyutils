package net.nicguzzo;
import net.nicguzzo.kiln.KilnScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;

public class SkyutilsClientMod implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		
		ScreenRegistry.register(SkyutilsMod.KILN_SCREEN_HANDLER, KilnScreen::new);
		
		/*ScreenProviderRegistry.INSTANCE.<KilnContainer>registerFactory(SkyutilsMod.KILN, 
		(container) -> new KilnScreen(container, MinecraftClient.getInstance().player.inventory, new TranslatableText(SkyutilsMod.KILN_BLOCK_TRANSLATION_KEY)));
*/
	}

}
