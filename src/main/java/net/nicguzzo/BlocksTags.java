package net.nicguzzo;


import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;


public class BlocksTags {
    public static final TagKey<Block> HAMMER_MINABLE = TagKey.of(RegistryKeys.BLOCK, new Identifier("skyutils", "hammer_minable"));
}