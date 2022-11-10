package net.nicguzzo;


import net.minecraft.block.Block;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class BlocksTags {
    public static final TagKey<Block> HAMMER_MINABLE = TagKey.of(Registry.BLOCK_KEY, new Identifier("skyutils", "hammer_minable"));
}