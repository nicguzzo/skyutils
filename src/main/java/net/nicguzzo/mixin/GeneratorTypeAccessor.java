package net.nicguzzo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import net.minecraft.world.gen.WorldPresets;

@Mixin(WorldPresets.class)
public interface GeneratorTypeAccessor {
  /*@Accessor("VALUES")
  public static List<WorldPresets> getValues() {
    throw new AssertionError();
  }*/
}
