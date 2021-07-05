package net.nicguzzo.mixin;

import com.google.common.collect.Maps;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.feature.StructureFeature;
import net.nicguzzo.SkyutilsConfig;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;
import java.util.Optional;

@Mixin(ChunkGeneratorSettings.class)
public class ChunkGeneratorSettingsMixin {

    @ModifyVariable(method = "createIslandSettings", at = @At("HEAD"), index = 0, ordinal = 0)
    static private StructuresConfig injected(StructuresConfig s) {
        Map<StructureFeature<?>, StructureConfig> DEFAULT_STRUCTURES = Maps.newHashMap();
        SkyutilsConfig config=SkyutilsConfig.get_instance();
        System.out.println("createIslandSettings");
        if(config.villages){
            DEFAULT_STRUCTURES.put(StructureFeature.VILLAGE, new StructureConfig(32, 8, 10387312));
        }
        DEFAULT_STRUCTURES.put(StructureFeature.DESERT_PYRAMID, new StructureConfig(32, 8, 14357617));
        DEFAULT_STRUCTURES.put(StructureFeature.IGLOO, new StructureConfig(32, 8, 14357618));
        DEFAULT_STRUCTURES.put(StructureFeature.JUNGLE_PYRAMID, new StructureConfig(32, 8, 14357619));
        DEFAULT_STRUCTURES.put(StructureFeature.SWAMP_HUT, new StructureConfig(32, 8, 14357620));
        DEFAULT_STRUCTURES.put(StructureFeature.PILLAGER_OUTPOST, new StructureConfig(32, 8, 165745296));
        DEFAULT_STRUCTURES.put(StructureFeature.STRONGHOLD, new StructureConfig(1, 0, 0));
        DEFAULT_STRUCTURES.put(StructureFeature.MONUMENT, new StructureConfig(32, 5, 10387313));
        DEFAULT_STRUCTURES.put(StructureFeature.END_CITY, new StructureConfig(20, 11, 10387313));
        DEFAULT_STRUCTURES.put(StructureFeature.MANSION, new StructureConfig(80, 20, 10387319));
        // DEFAULT_STRUCTURES.put(StructureFeature.BURIED_TREASURE, new
        // StructureConfig(1, 0, 0));
        // DEFAULT_STRUCTURES.put(StructureFeature.MINESHAFT, new StructureConfig(1, 0,
        // 0));
        DEFAULT_STRUCTURES.put(StructureFeature.RUINED_PORTAL, new StructureConfig(40, 15, 34222645));
        DEFAULT_STRUCTURES.put(StructureFeature.SHIPWRECK, new StructureConfig(24, 4, 165745295));
        DEFAULT_STRUCTURES.put(StructureFeature.OCEAN_RUIN, new StructureConfig(20, 8, 14357621));
        DEFAULT_STRUCTURES.put(StructureFeature.BASTION_REMNANT, new StructureConfig(27, 4, 30084232));
        DEFAULT_STRUCTURES.put(StructureFeature.FORTRESS, new StructureConfig(27, 4, 30084232));
        DEFAULT_STRUCTURES.put(StructureFeature.NETHER_FOSSIL, new StructureConfig(2, 1, 14357921));
        return new StructuresConfig(Optional.of(StructuresConfig.DEFAULT_STRONGHOLD), DEFAULT_STRUCTURES);

    }
}
