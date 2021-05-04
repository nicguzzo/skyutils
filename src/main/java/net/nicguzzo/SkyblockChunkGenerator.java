package net.nicguzzo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.*;
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
    private int spawn_radius = 60;
    private int separation = 20;
    private int height_variation = 30;
    private int height_end = 120;
    private int height_start = height_end-20;

    private int rad2 = 0;
    private int spawn_cx = 0;
    private int spawn_cz = 0;
    private boolean first = true;

    static final private int n_pbl = 6;
    private static class Vec2i{
        public int x;
        public int z;
        public Vec2i(int x,int z){
            this.x=x;
            this.z=z;
        }
    }
    private static class Circle{
        public int cx;
        public int cz;
        public int rad;// rad^2
        public Circle(int cx,int cz,int rad){
            this.cx=cx;
            this.cz=cz;
            this.rad=rad;
        }
        private boolean inside(int x, int z) {
            x = x - cx;
            z = z - cz;
            return (x * x + z * z) <= rad;
        }
    }
    static private Circle[] circles=new Circle[4];
    private static class Pbl {
        double a;
        int cx;
        int cy;
        int cz;

        Pbl(int _cx, int _cy, int _cz, double _a) {
            a = _a;
            cx = _cx;
            cy = _cy;
            cz = _cz;
        }

        public boolean inside(int x, int y, int z) {
            x = x - cx;
            y = y - cy;
            z = z - cz;
            return (x * x + z * z) - (a * y) <= 0;
        }
    }

    Pbl[] pbls = new Pbl[n_pbl];
    @Nullable
    private long seed;
    protected final Supplier<ChunkGeneratorSettings> settings;

    StructureFeature<?>[] features={StructureFeature.SWAMP_HUT,StructureFeature.VILLAGE,StructureFeature.DESERT_PYRAMID,StructureFeature.IGLOO,StructureFeature.JUNGLE_PYRAMID,StructureFeature.PILLAGER_OUTPOST};
    StructureFeature<?>[] features2={StructureFeature.VILLAGE,StructureFeature.DESERT_PYRAMID,StructureFeature.PILLAGER_OUTPOST};

    private static final Logger LOGGER = LogManager.getLogger();

    public SkyblockChunkGenerator(BiomeSource biomeSource, long seed, Supplier<ChunkGeneratorSettings> settings) {
        this(biomeSource, biomeSource, seed, settings);
    }

    private SkyblockChunkGenerator(BiomeSource populationSource, BiomeSource biomeSource, long seed,
                                   Supplier<ChunkGeneratorSettings> settings) {
        super(populationSource, biomeSource, ((ChunkGeneratorSettings) settings.get()).getStructuresConfig(), seed);

        this.seed = seed;

        this.settings = settings;
        rad2 = spawn_radius * spawn_radius;
        ChunkRandom chunkRandom = new ChunkRandom(seed);
        this.noise2 = new OctavePerlinNoiseSampler(chunkRandom, IntStream.rangeClosed(-5, 0));
//chunkRandom.consume(17292);
        LOGGER.info("SkyblockChunkGenerator");
        LOGGER.info("sealevel " + getSeaLevel());
                
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

        for (int m = 0; m < 16; ++m) {
            for (int n = 0; n < 16; ++n) {
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

    public BlockBox isStructureAt(StructureAccessor accessor, ChunkSectionPos pos, boolean matchChildren, StructureFeature<?> feature) {
        Optional<? extends StructureStart<?>> s = (accessor.getStructuresWithChildren(pos, feature).filter((structureStart) -> {
            return structureStart.getBoundingBox().intersectsXZ(pos.getMinX(), pos.getMinZ(), pos.getMaxX(), pos.getMaxZ());
        }).filter((structureStart) -> {
            return !matchChildren || structureStart.getChildren().stream().anyMatch((piece) -> {
                return piece.getBoundingBox().intersectsXZ(pos.getMinX(), pos.getMinZ(), pos.getMaxX(), pos.getMaxZ());
            });
        }).findFirst());
        if (s.isPresent())
            return s.get().getBoundingBox();
        return null;
    }

    public Stream<? extends StructureStart<?>> getStructuresWithChildren2(WorldAccess world,StructureAccessor accessor,Chunk chunk, StructureFeature<?> feature) {
        return chunk.getStructureReferences(feature).stream().map((posx) -> {
            return ChunkSectionPos.from(new ChunkPos(posx), 0);
        }).map((posx) -> {
            StructureStart<?> s=null;
            if(world.isChunkLoaded(posx.getSectionX(), posx.getSectionZ())){
                s=accessor.getStructureStart(posx, feature, world.getChunk(posx.getSectionX(), posx.getSectionZ(), ChunkStatus.STRUCTURE_STARTS));
            }
            return s;
        }).filter((structureStart) -> {
            return structureStart != null && structureStart.hasChildren();
        });
    }

    private boolean point_in_island(int x, int y, int z) {
        for (int i = 0; i < n_pbl; i++) {
            if (pbls[i].inside(x, y, z))
                return true;
        }
        return false;
    }

    private boolean inside_radius(int cx,int cz,int x, int z,int r2) {
        x = x - cx;
        z = z - cz;
        return (x * x + z * z) <= r2;
    }

    public void populateNoise(WorldAccess world, StructureAccessor accessor, Chunk chunk) {
        ChunkPos chunkPos = chunk.getPos();

        boolean on_test= chunkPos.x==4 && chunkPos.z == 11;

        if (first) {
            first = false;
            spawn_cx = world.getLevelProperties().getSpawnX();
            spawn_cz = world.getLevelProperties().getSpawnZ();
//LOGGER.info("cx: "+cx +" cz: "+cz);
            pbls[0] = new Pbl(spawn_cx, 0, spawn_cz, 1);
            int sep2 = separation / 2;
            int h2 = height_variation / 2;
            for (int i = 1; i < n_pbl; i++) {
                double r = 0.1 + world.getRandom().nextDouble() * 1.5;
                int rx = -sep2 + (int) (world.getRandom().nextDouble() * separation);
                int ry = h2 + (int) (world.getRandom().nextDouble() * height_variation);
                int rz = -sep2 + (int) (world.getRandom().nextDouble() * separation);
                pbls[i] = new Pbl(spawn_cx + rx, ry, spawn_cz + rz, r);
            }
        }
        int chsx = chunkPos.getStartX();
        int chsz = chunkPos.getStartZ();
        int chex = chunkPos.getEndX();
        int chez = chunkPos.getEndZ();

        BlockPos.Mutable mutable = new BlockPos.Mutable();
        Heightmap heightmap = chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
        Heightmap heightmap2 = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
        boolean has_struct=false;
        /*
        BlockBox swamp_hut = isStructureAt(accessor, ChunkSectionPos.from(chunkPos, 0), true, StructureFeature.SWAMP_HUT);
        BlockBox jungle_pyramid = isStructureAt(accessor, ChunkSectionPos.from(chunkPos, 0), true, StructureFeature.JUNGLE_PYRAMID);
        BlockBox village = isStructureAt(accessor, ChunkSectionPos.from(chunkPos, 0), true, StructureFeature.VILLAGE);
        BlockBox desert_pyramid = isStructureAt(accessor, ChunkSectionPos.from(chunkPos, 0), true, StructureFeature.DESERT_PYRAMID);
        BlockBox igloo = isStructureAt(accessor, ChunkSectionPos.from(chunkPos, 0), true, StructureFeature.IGLOO);
*/

        //boolean has_struct = has_village || has_swamp_hut|| has_jungle_pyramid || has_desert_pyramid ||has_igloo;
        for (int j = 0; j < features.length; j++) {
            BlockBox bb = isStructureAt(accessor, ChunkSectionPos.from(chunkPos, 0), true, features[j]);
            has_struct= (bb!=null)|| has_struct;
        }

        boolean around=false;
        //boolean has_swamp_hut = swamp_hut != null;
        //boolean has_village = village != null;
        //boolean has_jungle_pyramid= jungle_pyramid!=null;
        //boolean has_desert_pyramid= desert_pyramid!=null;
        //boolean has_igloo= igloo!=null;
        circles[0].rad=0;
        circles[1].rad=0;
        circles[2].rad=0;
        circles[3].rad=0;
        //int struct_center_x=0;
        //int struct_center_z=0;
        //int rad3=400;
        if(!has_struct) {
            Vec2i[] chnks = new Vec2i[8];
            chnks[0] = new Vec2i(chunkPos.x + 1, chunkPos.z);
            chnks[1] = new Vec2i(chunkPos.x - 1, chunkPos.z);
            chnks[2] = new Vec2i(chunkPos.x, chunkPos.z + 1);
            chnks[3] = new Vec2i(chunkPos.x, chunkPos.z - 1);
            chnks[4] = new Vec2i(chunkPos.x + 1, chunkPos.z + 1);
            chnks[5] = new Vec2i(chunkPos.x + 1, chunkPos.z - 1);
            chnks[6] = new Vec2i(chunkPos.x - 1, chunkPos.z + 1);
            chnks[7] = new Vec2i(chunkPos.x - 1, chunkPos.z - 1);
            
            for (int i = 0; i < chnks.length; i++) {
                //Chunk ch=world.getExistingChunk(chnks[i].x, chnks[i].z);
                Chunk chnk = world.getChunk(chnks[i].x, chnks[i].z, ChunkStatus.STRUCTURE_STARTS);
                ChunkPos chunkPos2 =chnk.getPos();
                for (int j = 0; j < features.length; j++) {
                    StructureStart<?> fe = accessor.getStructureStart(ChunkSectionPos.from(chnk.getPos(), 0), features[j],chnk);
                    if (fe != null && fe.hasChildren()) {
                        around = true;
                        circles[0].cx = (int)chunkPos2.getStartX() + (chunkPos2.getEndX() - chunkPos2.getStartX() + 1) / 2;
                        circles[0].cz = (int)chunkPos2.getStartZ() + (chunkPos2.getEndZ() - chunkPos2.getStartZ() + 1) / 2;
                        circles[0].rad=400;
                        break;
                    }
                }
                if(around){
                    break;
                }
                /*StructureStart<?> swamp_hut1 = accessor.getStructureStart(ChunkSectionPos.from(chnk.getPos(), 0), StructureFeature.SWAMP_HUT,chnk);
                if (swamp_hut1 != null && swamp_hut1.hasChildren()) {
                    around = true;
                    circles[0].cx = (int)chunkPos2.getStartX() + (chunkPos2.getEndX() - chunkPos2.getStartX() + 1) / 2;
                    circles[0].cz = (int)chunkPos2.getStartZ() + (chunkPos2.getEndZ() - chunkPos2.getStartZ() + 1) / 2;
                    circles[0].rad=400;
                    break;
                }
                StructureStart<?> jungle_pyramid1 = accessor.getStructureStart(ChunkSectionPos.from(chnk.getPos(), 0), StructureFeature.JUNGLE_PYRAMID,chnk);
                if (jungle_pyramid1 != null && jungle_pyramid1.hasChildren()) {
                    around = true;
                    circles[0].cx = (int)chunkPos2.getStartX() + (chunkPos2.getEndX() - chunkPos2.getStartX() + 1) / 2;
                    circles[0].cz = (int)chunkPos2.getStartZ() + (chunkPos2.getEndZ() - chunkPos2.getStartZ() + 1) / 2;
                    circles[0].rad=400;
                    break;
                }
                StructureStart<?> desert_pyramid1 = accessor.getStructureStart(ChunkSectionPos.from(chnk.getPos(), 0), StructureFeature.DESERT_PYRAMID,chnk);
                if (desert_pyramid1 != null && desert_pyramid1.hasChildren()) {
                    around = true;
                    circles[0].cx = (int)chunkPos2.getStartX() + (chunkPos2.getEndX() - chunkPos2.getStartX() + 1) / 2;
                    circles[0].cz = (int)chunkPos2.getStartZ() + (chunkPos2.getEndZ() - chunkPos2.getStartZ() + 1) / 2;
                    circles[0].rad=400;
                    break;
                }
                StructureStart<?> igloo1 = accessor.getStructureStart(ChunkSectionPos.from(chnk.getPos(), 0), StructureFeature.IGLOO,chnk);
                if (igloo1 != null && igloo1.hasChildren()) {
                    around = true;
                    circles[0].cx = (int)chunkPos2.getStartX() + (chunkPos2.getEndX() - chunkPos2.getStartX() + 1) / 2;
                    circles[0].cz = (int)chunkPos2.getStartZ() + (chunkPos2.getEndZ() - chunkPos2.getStartZ() + 1) / 2;
                    circles[0].rad=400;
                    break;
                }*/

                
                
                //if(world.isChunkLoaded(chnks[i].x, chnks[i].z))
                if(i<4)
                {
                    ChunkSectionPos pos=ChunkSectionPos.from(chnks[i].x, 0,chnks[i].z);
                    for (int j = 0; j < features2.length; j++) {
                        try {
                            Optional<? extends StructureStart<?>> vv =getStructuresWithChildren2(world,accessor,chnk,features2[j]).filter((structureStart) -> {
                                return structureStart.getChildren().stream().anyMatch((piece) -> {
                                    return piece.getBoundingBox().intersectsXZ(pos.getMinX(), pos.getMinZ(), pos.getMaxX(), pos.getMaxZ());
                                });
                            }).findFirst();
                            if (vv!=null && vv.isPresent()) {                            
                                around = true;                            
                                circles[i].cx = (int) chunkPos2.getStartX() + (chunkPos2.getEndX() - chunkPos2.getStartX() + 1) / 2;
                                circles[i].cz = (int) chunkPos2.getStartZ() + (chunkPos2.getEndZ() - chunkPos2.getStartZ() + 1) / 2;
                                circles[i].rad = 250;                                
                            }
                        }catch(RuntimeException e){

                        }
                    }
                    /*{   
                        Optional<? extends StructureStart<?>> vv =getStructuresWithChildren2(world,accessor,chnk, StructureFeature.DESERT_PYRAMID).filter((structureStart) -> {
                            return structureStart.getChildren().stream().anyMatch((piece) -> {
                                return piece.getBoundingBox().intersectsXZ(pos.getMinX(), pos.getMinZ(), pos.getMaxX(), pos.getMaxZ());
                            });
                        }).findFirst();
                        if (vv!=null && vv.isPresent()) {                            
                            around = true;                            
                            circles[i].cx = (int) chunkPos2.getStartX() + (chunkPos2.getEndX() - chunkPos2.getStartX() + 1) / 2;
                            circles[i].cz = (int) chunkPos2.getStartZ() + (chunkPos2.getEndZ() - chunkPos2.getStartZ() + 1) / 2;
                            circles[i].rad = 250;
                        }
                    }*/
                }
            }
        }

        if(on_test){
            System.out.println("circle 0 r" + circles[0].rad + " cx "+circles[0].cx+ " cz "+circles[0].cz);
            System.out.println("circle 1 r" + circles[1].rad + " cx "+circles[1].cx+ " cz "+circles[1].cz);
            System.out.println("circle 2 r" + circles[2].rad + " cx "+circles[2].cx+ " cz "+circles[2].cz);
            System.out.println("circle 3 r" + circles[3].rad + " cx "+circles[3].cx+ " cz "+circles[3].cz);
        }

        

        if (has_struct || around||
                inside_radius(spawn_cx,spawn_cz,chsx, chsz,rad2) ||
                inside_radius(spawn_cx,spawn_cz,chex, chsz,rad2) ||
                inside_radius(spawn_cx,spawn_cz,chsx, chez,rad2) ||
                inside_radius(spawn_cx,spawn_cz,chex, chez,rad2)) {
            if (has_struct ||around) {
                height_end = 63;
                height_start = height_end-20;
            }
            float m = (height_end - height_start);
            int yy =height_start+(int)(m/2)+1;
            m=m*2.5f;
            //boolean around=has_struct && swamp_hut==null && village==null;
            if (around) {
                yy+=6;
            }
//int i=60;
            int he=height_end+20;
            for (int i = height_start; i < he; ++i) {
                for (int j = 0; j < 16; ++j) {
                    for (int k = 0; k < 16; ++k) {
//if(inside_radius(cshx+j, cshz+k))
                        {
                            if (
                                    has_struct
                                    || (circles[0].rad!=0 && circles[0].inside(chsx + j, chsz + k))
                                    || (circles[1].rad!=0 && circles[1].inside(chsx + j, chsz + k))
                                    || (circles[2].rad!=0 && circles[2].inside(chsx + j, chsz + k))
                                    || (circles[3].rad!=0 && circles[3].inside(chsx + j, chsz + k))
                                    || point_in_island(chsx + j, i, chsz + k)
                            ) {
                                PerlinNoiseSampler perlinNoiseSampler = this.noise2.getOctave(1);
                                double n = yy + perlinNoiseSampler.sample((chsx + j) * 0.1, 0, (chsz + k) * 0.1, 500, 100) * m;
                                double n2 = height_end+ perlinNoiseSampler.sample((chsx + j) * 0.01, 0, (chsz + k) * 0.01, 500, 100) * 30;
                                if(n2<height_end){
                                    n2=height_end;
                                }
                                if (n >= height_end) {
                                    n=height_end-1;
                                }
                                //if (!around && n >= height_end) {
                                //    n = n + 1;
                                //}
                                if (i > n  && i< n2) {
                                //if (i > n  || (!around && i>(height_end-3))) {
                                //if (i > n  || (i>(height_end-3))) {
                                    chunk.setBlockState(mutable.set(j, i, k), stone, false);
                                    heightmap.trackUpdate(j, i, k, stone);
                                    heightmap2.trackUpdate(j, i, k, stone);
                                }
                            }/*else if(around){
                                chunk.setBlockState(mutable.set(j, i, k), glass, false);
                                heightmap.trackUpdate(j, i, k, glass);
                                heightmap2.trackUpdate(j, i, k, glass);
                            }*/
                        }
                    }
                }
            }
        }

    }

    public void carve(long seed, BiomeAccess access, Chunk chunk, GenerationStep.Carver carver) {
    }

    public int getSeaLevel() {
        return 62;
//return ((ChunkGeneratorSettings) this.settings.get()).getSeaLevel();
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
        circles[0]=new Circle(0,0,0);
        circles[1]=new Circle(0,0,0);
        circles[2]=new Circle(0,0,0);
        circles[3]=new Circle(0,0,0);
    }
}
