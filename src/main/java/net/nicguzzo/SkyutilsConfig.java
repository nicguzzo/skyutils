package net.nicguzzo;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;



public class SkyutilsConfig {
	public static SkyutilsConfig INSTANCE;
	public int hammer_durability=0;
	public boolean overworld_bedrock=false;
	public boolean nether_bedrock=false;
	
	public SkyutilsConfig(int hd,boolean ov_b,boolean nth_b) {
		if(hd>=0)
			this.hammer_durability = hd;
		overworld_bedrock=ov_b;
		nether_bedrock=nth_b;
	}
	public SkyutilsConfig() {
		this(0,false,false);
	}
	public String toString() {
		return "hammer_durability: "+hammer_durability;
	}

	public PacketByteBuf writeConfig(PacketByteBuf buf) {
		return writeConfig(buf, this);
	}

	public static PacketByteBuf writeConfig(PacketByteBuf buf, SkyutilsConfig config) {
		buf.writeInt(config.hammer_durability);
		buf.writeBoolean(config.overworld_bedrock);
		buf.writeBoolean(config.nether_bedrock);
		return buf;
	}

	public static SkyutilsConfig readConfig(PacketByteBuf buf) {
		int hd = buf.readInt();
		boolean o_b = buf.readBoolean();
		boolean n_b = buf.readBoolean();
		return new SkyutilsConfig(hd,o_b,n_b);
	}

	public boolean equals(SkyutilsConfig config) {
		return (
				config.hammer_durability==hammer_durability
		);
	}
	public static void load_config() {
		File configFile = new File(FabricLoader.getInstance().getConfigDir().toString(), "skyutils.json");
		try (FileReader reader = new FileReader(configFile)) {
			INSTANCE = new Gson().fromJson(reader, SkyutilsConfig.class);
			try (FileWriter writer = new FileWriter(configFile)) {
				writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(INSTANCE));
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
