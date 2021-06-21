package net.nicguzzo;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.Util;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.*;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.GenerationShapeConfig;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.feature.StructureFeature;

import net.minecraft.world.biome.SpawnSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public final class SkyblockNetherChunkGenerator extends ChunkGenerator {
    public static final Codec<SkyblockNetherChunkGenerator> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter((skyblockChunkGenerator) -> {
            return skyblockChunkGenerator.populationSource;
        }), Codec.LONG.fieldOf("seed").stable().forGetter((skyblockChunkGenerator) -> {
            return skyblockChunkGenerator.seed;
        }), ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings").forGetter((skyblockChunkGenerator) -> {
            return skyblockChunkGenerator.settings;
        })).apply(instance, instance.stable(SkyblockNetherChunkGenerator::new));
    });

    private static final BlockState AIR;
    private final int verticalNoiseResolution;

    @Nullable
    private long seed;
    protected final Supplier<ChunkGeneratorSettings> settings;

    private static final Logger LOGGER = LogManager.getLogger();

    public SkyblockNetherChunkGenerator(BiomeSource biomeSource, long seed, Supplier<ChunkGeneratorSettings> settings) {
        this(biomeSource, biomeSource, seed, settings);
    }

    private SkyblockNetherChunkGenerator(BiomeSource populationSource, BiomeSource biomeSource, long seed,
            Supplier<ChunkGeneratorSettings> settings) {
        super(populationSource, biomeSource, ((ChunkGeneratorSettings) settings.get()).getStructuresConfig(), seed);
        this.seed = seed;
        ChunkGeneratorSettings chunkGeneratorSettings = (ChunkGeneratorSettings) settings.get();
        this.settings = settings;
        GenerationShapeConfig generationShapeConfig = chunkGeneratorSettings.getGenerationShapeConfig();
        this.verticalNoiseResolution = BiomeCoords.toBlock(generationShapeConfig.getSizeVertical());

        LOGGER.info("SkyblockNetherChunkGenerator");
    }

    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Environment(EnvType.CLIENT)
    public ChunkGenerator withSeed(long seed) {
        return new SkyblockNetherChunkGenerator(this.populationSource.withSeed(seed), seed, this.settings);
    }

    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world) {

        return 62;
    }

    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world) {
        BlockState[] blockStates = new BlockState[1];
        blockStates[0] = AIR;
        return new VerticalBlockSample(0, blockStates);
    }

    public void buildSurface(ChunkRegion region, Chunk chunk) {

    }

    public Pool<SpawnSettings.SpawnEntry> getEntitySpawnList(Biome biome, StructureAccessor accessor, SpawnGroup group,
            BlockPos pos) {
        if (accessor.getStructureAt(pos, true, StructureFeature.SWAMP_HUT).hasChildren()) {
            if (group == SpawnGroup.MONSTER) {
                return StructureFeature.SWAMP_HUT.getMonsterSpawns();
            }

            if (group == SpawnGroup.CREATURE) {
                return StructureFeature.SWAMP_HUT.getCreatureSpawns();
            }
        }

        if (group == SpawnGroup.MONSTER) {
            if (accessor.getStructureAt(pos, false, StructureFeature.PILLAGER_OUTPOST).hasChildren()) {
                return StructureFeature.PILLAGER_OUTPOST.getMonsterSpawns();
            }

            if (accessor.getStructureAt(pos, false, StructureFeature.MONUMENT).hasChildren()) {
                return StructureFeature.MONUMENT.getMonsterSpawns();
            }

            if (accessor.getStructureAt(pos, true, StructureFeature.FORTRESS).hasChildren()) {
                return StructureFeature.FORTRESS.getMonsterSpawns();
            }
        }

        return super.getEntitySpawnList(biome, accessor, group, pos);
    }

    public void carve(long seed, BiomeAccess access, Chunk chunk, GenerationStep.Carver carver) {
    }

    public int getSeaLevel() {
        // return 64;
        return ((ChunkGeneratorSettings) this.settings.get()).getSeaLevel();
    }

    public void populateEntities(ChunkRegion region) {
        // if (!((ChunkGeneratorSettings)
        // this.settings.get()).isMobGenerationDisabled())
        {
            ChunkPos chunkPos = region.getCenterPos();
            Biome biome = region.getBiome(chunkPos.getStartPos());
            ChunkRandom chunkRandom = new ChunkRandom();
            chunkRandom.setPopulationSeed(region.getSeed(), chunkPos.getStartX(), chunkPos.getStartZ());
            SpawnHelper.populateEntities(region, biome, chunkPos, chunkRandom);
        }
    }

    static {
        AIR = Blocks.AIR.getDefaultState();

    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, StructureAccessor accessor, Chunk chunk) {

        GenerationShapeConfig generationShapeConfig = ((ChunkGeneratorSettings) this.settings.get())
                .getGenerationShapeConfig();
        int i = Math.max(generationShapeConfig.getMinimumY(), chunk.getBottomY());
        int j = Math.min(generationShapeConfig.getMinimumY() + generationShapeConfig.getHeight(), chunk.getTopY());
        int k = MathHelper.floorDiv(i, this.verticalNoiseResolution);
        int l = MathHelper.floorDiv(j - i, this.verticalNoiseResolution);
        if (l <= 0) {
            return CompletableFuture.completedFuture(chunk);
        } else {
            int m = chunk.getSectionIndex(l * this.verticalNoiseResolution - 1 + i);
            int n = chunk.getSectionIndex(i);
            return CompletableFuture.supplyAsync(() -> {
                HashSet set = Sets.newHashSet();
                boolean var15 = false;

                Chunk var17;
                try {
                    var15 = true;
                    int mx = m;

                    while (true) {
                        if (mx < n) {
                            var17 = this.populateNoise(accessor, chunk, k, l);
                            var15 = false;
                            break;
                        }

                        ChunkSection chunkSection = chunk.getSection(mx);
                        chunkSection.lock();
                        set.add(chunkSection);
                        --mx;
                    }
                } finally {
                    if (var15) {
                        Iterator var12 = set.iterator();

                        while (var12.hasNext()) {
                            ChunkSection chunkSection3 = (ChunkSection) var12.next();
                            chunkSection3.unlock();
                        }

                    }
                }

                Iterator var18 = set.iterator();

                while (var18.hasNext()) {
                    ChunkSection chunkSection2 = (ChunkSection) var18.next();
                    chunkSection2.unlock();
                }

                return var17;
            }, Util.getMainWorkerExecutor());
        }
    }

    public Chunk populateNoise(StructureAccessor accessor, Chunk chunk, int startY, int noiseSizeY) {
        return chunk;
    }
}
