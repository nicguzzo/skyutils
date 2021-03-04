package net.nicguzzo.mixin;

import net.minecraft.world.level.LevelGeneratorType;
import org.spongepowered.asm.mixin.Mixin;

import net.nicguzzo.SkyutilsMod;

@Mixin(LevelGeneratorType.class)
public class LevelTypeMixin {
    private LevelTypeMixin(int id, String name) {}

    static {
      SkyutilsMod.SKB_LEVEL_GENERATOR_TYPE = (LevelGeneratorType) (Object) new LevelTypeMixin(15, "skyblock");
    }
}
