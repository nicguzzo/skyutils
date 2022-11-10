package net.nicguzzo;

import net.fabricmc.loader.api.FabricLoader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;



public class SkyutilsConfig {
	private static SkyutilsConfig INSTANCE=null;
	public boolean nether_bedrock=false;
	public float spawn_island_radius=1;
	public boolean villages=true;
	public int geode_rareness=200;
	public boolean generate_islands_below_structs=true;
	public boolean generate_small_islands=false;
	public int hammer_iron_nugget_chance=25;
	public int hammer_sand_redsand_ratio=70;
	public int hammer_redstone_from_sand_chance=10;
	public int hammer_redstone_from_redsand_chance=20;
	public int hammer_grass_extra_chance=20;
	public int hammer_podzol_extra_chance=20;
	public int hammer_sand_extra_chance=20;
	public int hammer_diamond_nugget_chance=5;
	public int hammer_nether_wart_chance=10;
	public int hammer_netherite_scarp_chance=1;
	public int hammer_netherrack_extra_chance=20;
	public int basalt_amethyst_chance=10;
	public int kiln_lava_cost=16;
	public int kiln_cobblestone_cook_time=900;
	public int kiln_crucible_cook_time=100;
	public int kiln_charcoal_burn_time=100;
	public int kiln_charcoal_block_burn_time=900;

	public static SkyutilsConfig get_instance(){
		if(INSTANCE==null){
			load_config();
		}
		return INSTANCE;
	}

	public static void load_config() {
		INSTANCE = new SkyutilsConfig();
		Gson gson=new Gson();
		File configFile = new File(FabricLoader.getInstance().getConfigDir().toString(), "skyutils.json");
		try (FileReader reader = new FileReader(configFile)) {
			INSTANCE = gson.fromJson(reader, SkyutilsConfig.class);
			System.out.println("Config: "+INSTANCE);
			try (FileWriter writer = new FileWriter(configFile)) {
				writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(INSTANCE));
				System.out.println("Config updated!");
			} catch (IOException e2) {
				System.out.println("Failed to update config file!");
			}
			System.out.println("Config loaded!");

		} catch (IOException e) {
			System.out.println("No config found, generating!");
			INSTANCE = new SkyutilsConfig();
			try (FileWriter writer = new FileWriter(configFile)) {
				writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(INSTANCE));
			} catch (IOException e2) {
				System.out.println("Failed to generate config file!");
			}
		}
	}
}
