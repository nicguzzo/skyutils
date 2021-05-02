package net.nicguzzo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Supplier;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.Heightmap;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.*;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
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
        this.settings = settings;
        
        LOGGER.info("SkyblockNetherChunkGenerator");
    }

    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Environment(EnvType.CLIENT)
    public ChunkGenerator withSeed(long seed) {
        return new SkyblockNetherChunkGenerator(this.populationSource.withSeed(seed), seed, this.settings);        
    }

    public int getHeight(int x, int z, Heightmap.Type heightmapType) {

        return 62;
    }

    public BlockView getColumnSample(int x, int z) {
        BlockState[] blockStates = new BlockState[1];
        blockStates[0] = AIR;
        return new VerticalBlockSample(blockStates);
    }

    public void buildSurface(ChunkRegion region, Chunk chunk) {
               
    }

    public List<SpawnSettings.SpawnEntry> getEntitySpawnList(Biome biome, StructureAccessor accessor, SpawnGroup group, BlockPos pos) {
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
    

     public void populateNoise(WorldAccess world, StructureAccessor accessor, Chunk chunk) {      
      

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
            int i = region.getCenterChunkX();
            int j = region.getCenterChunkZ();
            Biome biome = region.getBiome((new ChunkPos(i, j)).getStartPos());
            ChunkRandom chunkRandom = new ChunkRandom();
            chunkRandom.setPopulationSeed(region.getSeed(), i << 4, j << 4);
            SpawnHelper.populateEntities(region, biome, i, j, chunkRandom);
        }
    }

    static {
        AIR = Blocks.AIR.getDefaultState();
        
    }
}
