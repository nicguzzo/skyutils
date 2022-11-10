package net.nicguzzo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Blocks;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

import java.util.List;

public record SkbGeodeConfig(int number, Identifier blockID) implements FeatureConfig {
    public SkbGeodeConfig(int number, Identifier blockID) {
        this.blockID = blockID;
        this.number = number;

        /*new GeodeFeatureConfig(
                new GeodeLayerConfig(
                        BlockStateProvider.of(Blocks.AIR),
                        BlockStateProvider.of(Blocks.AMETHYST_BLOCK),
                        BlockStateProvider.of(Blocks.BUDDING_AMETHYST),
                        BlockStateProvider.of(Blocks.CALCITE),
                        BlockStateProvider.of(Blocks.SMOOTH_BASALT),
                        List.of(Blocks.SMALL_AMETHYST_BUD.getDefaultState(),
                                Blocks.MEDIUM_AMETHYST_BUD.getDefaultState(),
                                Blocks.LARGE_AMETHYST_BUD.getDefaultState(),
                                Blocks.AMETHYST_CLUSTER.getDefaultState()),
                        BlockTags.FEATURES_CANNOT_REPLACE, BlockTags.GEODE_INVALID_BLOCKS),
                new GeodeLayerThicknessConfig(1.7, 2.2, 3.2, 4.2),
                new GeodeCrackConfig(0.95, 2.0, 2), 0.35,
                0.083, true,
                UniformIntProvider.create(4, 6), UniformIntProvider.create(3, 4), UniformIntProvider
                .create(1, 2), -16, 16, 0.05, 1);*/
    }

    public static Codec<SkbGeodeConfig> CODEC = RecordCodecBuilder.create(
            instance ->
                    instance.group(
                                    // you can add as many of these as you want, one for each parameter
                                    Codecs.POSITIVE_INT.fieldOf("number").forGetter(SkbGeodeConfig::number),
                                    Identifier.CODEC.fieldOf("blockID").forGetter(SkbGeodeConfig::blockID))
                            .apply(instance, SkbGeodeConfig::new));

    public int number() {
        return number;
    }

    public Identifier blockID() {
        return blockID;
    }
}