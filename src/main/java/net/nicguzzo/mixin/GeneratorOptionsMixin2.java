package net.nicguzzo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.world.gen.GeneratorOptions;
import net.nicguzzo.SkyutilsClientMod;
import net.nicguzzo.SkyutilsMod;

@Mixin(GeneratorOptions.class)
class GeneratorOptionsMixin2 {

    @ModifyVariable(method = "<init>", at = @At("HEAD"), index = 4, ordinal = 1/* , print = true */)
    static private boolean injected(boolean b) {
        if (SkyutilsClientMod.skyblock) {
            System.out.println("force bonus chest!!!!");
            return true;
        } else {            
            SkyutilsMod.is_skyblock=false;
            return b;
        }
    }
}