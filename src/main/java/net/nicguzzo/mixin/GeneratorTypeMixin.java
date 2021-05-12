package net.nicguzzo.mixin;

import net.minecraft.client.world.GeneratorType;
import net.minecraft.text.Text;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.gen.GeneratorOptions;
import net.nicguzzo.SkyutilsClientMod;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GeneratorType.class)
public abstract class GeneratorTypeMixin {

    @Shadow
    public abstract Text getTranslationKey();

    // @ModifyVariable(method =
    // "createDefaultOptions(Lnet/minecraft/util/registry/DynamicRegistryManager$Impl;JZZ)Lnet/minecraft/world/gen/GeneratorOptions;",
    // at = @At("HEAD"), ordinal = 3)
    @Inject(method = "createDefaultOptions", at = @At("HEAD"))
    private void injected(DynamicRegistryManager.Impl registryManager, long seed, boolean generateStructures,
            boolean bonusChest, CallbackInfoReturnable<GeneratorOptions> info) {

        SkyutilsClientMod.skyblock = this.getTranslationKey().asString().equalsIgnoreCase("skyblock_island");
        System.out.println("skyblock: " + SkyutilsClientMod.skyblock);
    }

}