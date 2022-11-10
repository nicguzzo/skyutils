package net.nicguzzo;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.nicguzzo.kiln.KilnScreen;

public class SkyutilsClientMod implements ClientModInitializer {
	public static boolean skyblock = false;
	@Override
	public void onInitializeClient() {
		ScreenRegistry.register(SkyutilsMod.KILN_SCREEN_HANDLER, KilnScreen::new);
	}
}
