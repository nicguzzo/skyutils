package net.nicguzzo;

import net.minecraft.network.PacketByteBuf;


public class SkyutilsConfig {
	public int hammer_durability=0;
	
	public SkyutilsConfig(int hd) {
		if(hd>=0)
			this.hammer_durability = hd;		
	}
	public SkyutilsConfig() {
		this(0);
	}
	public String toString() {
		return "hammer_durability: "+hammer_durability;
	}

	public PacketByteBuf writeConfig(PacketByteBuf buf) {
		return writeConfig(buf, this);
	}

	public static PacketByteBuf writeConfig(PacketByteBuf buf, SkyutilsConfig config) {
		buf.writeFloat(config.hammer_durability);		
		return buf;
	}

	public static SkyutilsConfig readConfig(PacketByteBuf buf) {
		int hd = buf.readInt();		
		return new SkyutilsConfig(hd);
	}

	public boolean equals(SkyutilsConfig config) {
		return (
				config.hammer_durability==hammer_durability
		);
	}
}
