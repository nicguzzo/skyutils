package net.nicguzzo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
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
import net.minecraft.util.math.noise.OctavePerlinNoiseSampler;
import net.minecraft.util.math.noise.PerlinNoiseSampler;
import net.minecraft.world.biome.SpawnSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public final class SkyblockChunkGenerator extends ChunkGenerator {
    public static final Codec<SkyblockChunkGenerator> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter((skyblockChunkGenerator) -> {
            return skyblockChunkGenerator.populationSource;
        }), Codec.LONG.fieldOf("seed").stable().forGetter((skyblockChunkGenerator) -> {
            return skyblockChunkGenerator.seed;
        }), ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings").forGetter((skyblockChunkGenerator) -> {
            return skyblockChunkGenerator.settings;
        })).apply(instance, instance.stable(SkyblockChunkGenerator::new));
    });
    
    private final OctavePerlinNoiseSampler noise2;
    private static final BlockState AIR;
    private static BlockState glass;
    private static BlockState stone;
    private static BlockState water;
    private int radius=60;
    private int separation=20;
    private int height_variation=30;
    private int height_start=40;
    private int height_end=60;
    private int rad2=0;
    private int cx=0;
    private int cz=0;
    private boolean first=true;
    
    static final private int n_pbl=6;
    private static class Pbl{
        double a;
        int cx;
        int cy;
        int cz;    
        Pbl(int _cx,int _cy,int _cz,double _a){
            a=_a;
            cx=_cx;
            cy=_cy;
            cz=_cz;
        }
        public boolean inside(int x,int y,int z){
            x=x-cx;
            y=y-cy;
            z=z-cz;
            return (x*x+z*z)-(a*y)<=0;
        }
    }
    Pbl[] pbls=new Pbl[n_pbl];
    @Nullable
    private long seed;
    protected final Supplier<ChunkGeneratorSettings> settings;

    private static final Logger LOGGER = LogManager.getLogger();

    public SkyblockChunkGenerator(BiomeSource biomeSource, long seed, Supplier<ChunkGeneratorSettings> settings) {
        this(biomeSource, biomeSource, seed, settings);
    }

    private SkyblockChunkGenerator(BiomeSource populationSource, BiomeSource biomeSource, long seed,
            Supplier<ChunkGeneratorSettings> settings) {
        super(populationSource, biomeSource, ((ChunkGeneratorSettings) settings.get()).getStructuresConfig(), seed);
        this.seed = seed;
        
        this.settings = settings;
        rad2=radius*radius;
        ChunkRandom chunkRandom = new ChunkRandom(seed);
        this.noise2 = new OctavePerlinNoiseSampler(chunkRandom, IntStream.rangeClosed(-5, 0));        
        //chunkRandom.consume(17292);
        LOGGER.info("SkyblockChunkGenerator");
    }

    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Environment(EnvType.CLIENT)
    public ChunkGenerator withSeed(long seed) {
        return new SkyblockChunkGenerator(this.populationSource.withSeed(seed), seed, this.settings);        
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
        ChunkPos chunkPos = chunk.getPos();
        int i = chunkPos.x;
        int j = chunkPos.z;
        ChunkRandom chunkRandom = new ChunkRandom();
        chunkRandom.setTerrainSeed(i, j);
        ChunkPos chunkPos2 = chunk.getPos();
        int k = chunkPos2.getStartX();
        int l = chunkPos2.getStartZ();
        //double d = 0.0625D;
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for(int m = 0; m < 16; ++m) {
            for(int n = 0; n < 16; ++n) {
                int o = k + m;
                int p = l + n;
                int q = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, m, n) + 1;
                double e = 1.0;//this.surfaceDepthNoise.sample((double)o * 0.0625D, (double)p * 0.0625D, 0.0625D, (double)m * 0.0625D) * 15.0D;
                region.getBiome(mutable.set(k + m, q, l + n)).buildSurface(chunkRandom, chunk, o, p, q, e, stone, water, this.getSeaLevel(), region.getSeed());
            }
        }        
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
    
     public  BlockBox isStructureAt(StructureAccessor accessor,ChunkSectionPos pos, boolean matchChildren, StructureFeature<?> feature) {
        Optional<? extends StructureStart<?>> s=(accessor.getStructuresWithChildren(pos, feature).filter((structureStart) -> {
           return structureStart.getBoundingBox().intersectsXZ(pos.getMinX(), pos.getMinZ(), pos.getMaxX(), pos.getMaxZ());
        }).filter((structureStart) -> {
           return !matchChildren || structureStart.getChildren().stream().anyMatch((piece) -> {
              return piece.getBoundingBox().intersectsXZ(pos.getMinX(), pos.getMinZ(), pos.getMaxX(), pos.getMaxZ());
           });
        }).findFirst());
        if(s.isPresent())
            return s.get().getBoundingBox();
        return null;
     }

    private boolean point_in_island(int x,int y,int z){        
        for(int i=0;i<n_pbl;i++){            
            if(pbls[i].inside(x, y, z))
                return true;
        }
        return false;
    }
    private boolean inside_radius(int x,int z){
        x=x-cx;
        z=z-cz;
        return (x*x+z*z)<=rad2;
    }
     public void populateNoise(WorldAccess world, StructureAccessor accessor, Chunk chunk) {      
        ChunkPos chunkPos = chunk.getPos();
        
        if(first){
            first=false;
            cx=world.getLevelProperties().getSpawnX();
            cz=world.getLevelProperties().getSpawnZ();
            //LOGGER.info("cx: "+cx +" cz: "+cz);
            pbls[0]=new Pbl(cx,0,cz,1);
            int sep2=separation/2;
            int h2=height_variation/2;
            for(int i=1;i<n_pbl;i++){
                double r=0.1+world.getRandom().nextDouble()*1.5;
                int rx=-sep2+(int)(world.getRandom().nextDouble()*separation);
                int ry= h2+(int)(world.getRandom().nextDouble()*height_variation);
                int rz=-sep2+(int)(world.getRandom().nextDouble()*separation);
                pbls[i]=new Pbl(cx+rx,ry,cz+rz,r);
            }
        }
        
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        Heightmap heightmap = chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
        Heightmap heightmap2 = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
        
        BlockBox bb=isStructureAt(accessor,ChunkSectionPos.from(chunkPos, 0),true,StructureFeature.SWAMP_HUT);
        BlockBox bb2=isStructureAt(accessor,ChunkSectionPos.from(chunkPos, 0),true,StructureFeature.VILLAGE);
        //boolean is_spawn=(x>chunkPos.getStartX() && x< chunkPos.getEndX() && z>chunkPos.getStartZ() && z< chunkPos.getEndZ());
        int cshx=chunkPos.getStartX();
        int cshz=chunkPos.getStartZ();
        int cehx=chunkPos.getEndX();
        int cehz=chunkPos.getEndZ();
        //boolean in_island=inside_radius(cshx,cshz)||inside_radius(cshx+15,cshz)||inside_radius(cshx,cshz+15)||inside_radius(cshx+15,cshz+15);
        
        if(bb!=null || bb2!=null ){
            int i=60;
            for(int j = 0; j < 16; ++j) {
                for(int k = 0; k < 16; ++k) {
                    chunk.setBlockState(mutable.set(j, i, k), glass, false);
                    heightmap.trackUpdate(j, i, k, glass);
                    heightmap2.trackUpdate(j, i, k, glass);
                }
            }
        }else if(inside_radius(cshx,cshz)||inside_radius(cehx,cshz)||inside_radius(cshx,cehz)||inside_radius(cehx,cehz)){
          
            float m=(height_end-height_start);
            int yy=(int)(m*2.5);
            
            //int i=60;
            for(int i = height_start; i < height_end; ++i) 
            {
                for(int j = 0; j < 16; ++j) {
                    for(int k = 0; k < 16; ++k) {                        
                        //if(inside_radius(cshx+j, cshz+k))
                        {
                            if(point_in_island(cshx+j,i,cshz+k))
                            {
                                PerlinNoiseSampler perlinNoiseSampler = this.noise2.getOctave(1);
                                double n= yy+perlinNoiseSampler.sample((cshx+j)*0.1, 0,(cshz+k)*0.1,500,100)*m;
                                if(n>=height_end){
                                    n=n-1;
                                }
                                if(i > n)
                                {
                                    chunk.setBlockState(mutable.set(j, i, k), stone, false);
                                    heightmap.trackUpdate(j, i, k, stone);
                                    heightmap2.trackUpdate(j, i, k, stone);
                                }
                            }
                        }
                    }
                }
            }
            
        }

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
        glass = Blocks.GLASS.getDefaultState();
        stone = Blocks.STONE.getDefaultState();
        water = Blocks.WATER.getDefaultState();
    }
}
