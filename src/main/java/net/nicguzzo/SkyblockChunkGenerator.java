package net.nicguzzo;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.noise.OctavePerlinNoiseSampler;
import net.minecraft.util.math.noise.PerlinNoiseSampler;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.GenerationShapeConfig;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.feature.StructureFeature;

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
    private final PerlinNoiseSampler perlinNoiseSampler;
    private final PerlinNoiseSampler perlinNoiseSampler2;
    private final SimplexNoiseSampler simplexSampler;
    private final int verticalNoiseResolution;
    private static final BlockState AIR;
    private static BlockState test_block;
    private static BlockState stone;
    private static BlockState water;
    
    SkyutilsConfig config;
    private int spawn_radius =60;
    private int separation = 20;
    private int height_variation = 30;
    private int height_end = 120;
    private int height_end2 = height_end + height_variation;
    private int height_start = height_end - 20;
    private int bx = 0;
    private int bz = 0;
    private int rad2 = 0;
    private int spawn_cx = 0;
    private int spawn_cz = 0;
    private boolean first = true;
    private int local_max=-100;
    private int last_local_max=0;
    private int last_level=50;
    
    static final private int n_pbl = 6;

    private static class Vec2i {
        public int x;
        public int z;

        public Vec2i(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }

    private static class Circle {
        public int cx;
        public int cz;
        public int rad;// rad^2

        public Circle(int cx, int cz, int rad) {
            this.cx = cx;
            this.cz = cz;
            this.rad = rad;
        }

        private boolean inside(int x, int z) {
            x = x - cx;
            z = z - cz;
            return (x * x + z * z) <= rad;
        }
    }

    static private Circle[] circles = new Circle[4];
    static private Vec2i[] chnks = new Vec2i[8];

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

    StructureFeature<?>[] features = { StructureFeature.SWAMP_HUT, StructureFeature.VILLAGE,
            StructureFeature.DESERT_PYRAMID, StructureFeature.IGLOO, StructureFeature.JUNGLE_PYRAMID,
            StructureFeature.PILLAGER_OUTPOST, StructureFeature.MANSION };
    StructureFeature<?>[] features2 = { StructureFeature.VILLAGE, StructureFeature.DESERT_PYRAMID,
            StructureFeature.PILLAGER_OUTPOST, StructureFeature.MANSION };

    private static final Logger LOGGER = LogManager.getLogger();

    public SkyblockChunkGenerator(BiomeSource biomeSource, long seed, Supplier<ChunkGeneratorSettings> settings) {
        this(biomeSource, biomeSource, seed, settings);
    }

    private SkyblockChunkGenerator(BiomeSource populationSource, BiomeSource biomeSource, long seed,
            Supplier<ChunkGeneratorSettings> settings) {
        super(populationSource, biomeSource, ((ChunkGeneratorSettings) settings.get()).getStructuresConfig(), seed);

        this.seed = seed;
        ChunkGeneratorSettings chunkGeneratorSettings = (ChunkGeneratorSettings) settings.get();
        this.settings = settings;
        GenerationShapeConfig generationShapeConfig = chunkGeneratorSettings.getGenerationShapeConfig();

        this.verticalNoiseResolution = BiomeCoords.toBlock(generationShapeConfig.getSizeVertical());
        config=SkyutilsConfig.get_instance();
        //spawn_radius=config.spawn_island_radius;
        rad2 = spawn_radius * spawn_radius;
        ChunkRandom chunkRandom = new ChunkRandom(seed);
        this.noise2 = new OctavePerlinNoiseSampler(chunkRandom, IntStream.rangeClosed(-5, 0));
        this.perlinNoiseSampler = this.noise2.getOctave(1);
        this.perlinNoiseSampler2 = this.noise2.getOctave(2);
        this.simplexSampler=new SimplexNoiseSampler(chunkRandom);
        // chunkRandom.consume(17292);
        LOGGER.info("SkyblockChunkGenerator");
        // LOGGER.info("sealevel " + getSeaLevel());
        SkyutilsMod.is_skyblock=true;
    }

    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Environment(EnvType.CLIENT)
    public ChunkGenerator withSeed(long seed) {
        return new SkyblockChunkGenerator(this.populationSource.withSeed(seed), seed, this.settings);
    }

    // @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world) {
        BlockState[] blockStates = new BlockState[1];
        blockStates[0] = AIR;
        return new VerticalBlockSample(0, blockStates);
    }

    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world) {
        // public int getHeight(int x, int z, Heightmap.Type heightmapType) {
        if (!inside_radius(spawn_cx, spawn_cz, x, z, rad2)) {
            height_end = 62;
            height_start = height_end - 20;
            height_end2 = height_end + height_variation;
        }
        double s = perlinNoiseSampler.sample(x * 0.01, 0, z * 0.01, 500, 100);
        int n2 = (int) (height_end + s * height_variation);
        if (n2 < height_end) {
            n2 = height_end + 1;
        }
        return n2;
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
        // double d = 0.0625D;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        
        for (int m = 0; m < 16; ++m) {
            for (int n = 0; n < 16; ++n) {
                int o = k + m;
                int p = l + n;
                int q = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, m, n) + 1;
                double e = 1.0;// this.surfaceDepthNoise.sample((double)o * 0.0625D, (double)p * 0.0625D,
                               // 0.0625D, (double)m * 0.0625D) * 15.0D;
                int r = ((ChunkGeneratorSettings) this.settings.get()).getMinSurfaceLevel();
                region.getBiome(mutable.set(k + m, q, l + n)).buildSurface(chunkRandom, chunk, o, p, q, e, stone, water,
                        this.getSeaLevel(), r, region.getSeed());
            }
        }
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

    private boolean struct_contains(StructureStart<?> structure, BlockPos pos) {
        Iterator<?> var2 = structure.getChildren().iterator();
        StructurePiece structurePiece;
        do {
            if (!var2.hasNext()) {
                return false;
            }

            structurePiece = (StructurePiece) var2.next();
        } while (!structurePiece.getBoundingBox().contains(pos));
        return true;
    }

    public boolean isStructureAt(WorldAccess world, Chunk chunk, StructureAccessor accessor, ChunkSectionPos pos,
            boolean matchChildren, StructureFeature<?> feature) {
        Optional<? extends StructureStart<?>> s = getStructuresWithChildren2(world, accessor, chunk, feature)
                .filter((structureStart) -> {
                    return structureStart.getChildren().stream().anyMatch((piece) -> {
                        return piece.getBoundingBox().intersectsXZ(pos.getMinX(), pos.getMinZ(), pos.getMaxX(),
                                pos.getMaxZ());
                    });
                }).findFirst();

        /*
         * Optional<? extends StructureStart<?>> s =
         * (accessor.getStructuresWithChildren(pos, feature) .filter((structureStart) ->
         * { return struct_contains(structureStart, pos.getMinPos());
         * }).filter((structureStart) -> { return !matchChildren ||
         * structureStart.getChildren().stream().anyMatch((piece) -> { return
         * piece.getBoundingBox().intersectsXZ(pos.getMinX(), pos.getMinZ(),
         * pos.getMaxX(), pos.getMaxZ()); }); }).findFirst());
         */
        if (s.isPresent())
            return true;
        return false;
    }

    public Stream<? extends StructureStart<?>> getStructuresWithChildren2(WorldAccess world, StructureAccessor accessor,
            Chunk chunk, StructureFeature<?> feature) {
        return chunk.getStructureReferences(feature).stream().map((posx) -> {
            return ChunkSectionPos.from(new ChunkPos(posx), 0);
        }).map((posx) -> {
            StructureStart<?> s = null;
            if (world.isChunkLoaded(posx.getSectionX(), posx.getSectionZ())) {
                s = accessor.getStructureStart(posx, feature,
                        world.getChunk(posx.getSectionX(), posx.getSectionZ(), ChunkStatus.STRUCTURE_STARTS));
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

    private boolean inside_radius(int cx, int cz, int x, int z, int r2) {
        x = x - cx;
        z = z - cz;
        return (x * x + z * z) <= r2;
    }

    public CompletableFuture<Chunk> populateNoise(Executor executor, StructureAccessor accessor, Chunk chunk) {

        ChunkPos chunkPos = chunk.getPos();
        WorldAccess world = accessor.world;
        // boolean on_test = chunkPos.x == 70 && chunkPos.z == 211;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        Heightmap heightmap = chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
        Heightmap heightmap2 = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
        int chsx = chunkPos.getStartX();
        int chsz = chunkPos.getStartZ();
        int chex = chunkPos.getEndX();
        int chez = chunkPos.getEndZ();
        if (first) {
            first = false;
            spawn_cx = world.getLevelProperties().getSpawnX();
            spawn_cz = world.getLevelProperties().getSpawnZ();
            System.out.println("spawn_island_radius: "+config.spawn_island_radius);
            pbls[0] = new Pbl(spawn_cx, 0, spawn_cz, config.spawn_island_radius);
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
        boolean has_struct = false;
        boolean around = false;
        // StructureWeightSampler structureWeightSampler = new
        // StructureWeightSampler(accessor, chunk);
        if(config.generate_islands_below_structs){
            for (int j = 0; j < features.length; j++) {
                if(features[j]== StructureFeature.VILLAGE && !config.villages)
                    continue;
                boolean bb = isStructureAt(world, chunk, accessor, ChunkSectionPos.from(chunkPos, 0), true, features[j]);
                has_struct = bb || has_struct;
            }
            circles[0].rad = 0;
            circles[1].rad = 0;
            circles[2].rad = 0;
            circles[3].rad = 0;

            if (!has_struct) {

                chnks[0].x = chunkPos.x + 1;
                chnks[0].z = chunkPos.z;
                chnks[1].x = chunkPos.x - 1;
                chnks[1].z = chunkPos.z;
                chnks[2].x = chunkPos.x;
                chnks[2].z = chunkPos.z + 1;
                chnks[3].x = chunkPos.x;
                chnks[3].z = chunkPos.z - 1;
                chnks[4].x = chunkPos.x + 1;
                chnks[4].z = chunkPos.z + 1;
                chnks[5].x = chunkPos.x + 1;
                chnks[5].z = chunkPos.z - 1;
                chnks[6].x = chunkPos.x - 1;
                chnks[6].z = chunkPos.z + 1;
                chnks[7].x = chunkPos.x - 1;
                chnks[7].z = chunkPos.z - 1;

                for (int i = 0; i < chnks.length; i++) {
                    
                    // Chunk ch=world.getExistingChunk(chnks[i].x, chnks[i].z);
                    Chunk chnk = world.getChunk(chnks[i].x, chnks[i].z, ChunkStatus.STRUCTURE_STARTS);
                    ChunkPos chunkPos2 = chnk.getPos();
                    for (int j = 0; j < features.length; j++) {
                        if(features[j]== StructureFeature.VILLAGE && !config.villages)
                            continue;
                        StructureStart<?> fe = accessor.getStructureStart(ChunkSectionPos.from(chnk.getPos(), 0),
                                features[j], chnk);
                        if (fe != null && fe.hasChildren()) {
                            around = true;
                            circles[0].cx = (int) chunkPos2.getStartX()
                                    + (chunkPos2.getEndX() - chunkPos2.getStartX() + 1) / 2;
                            circles[0].cz = (int) chunkPos2.getStartZ()
                                    + (chunkPos2.getEndZ() - chunkPos2.getStartZ() + 1) / 2;
                            circles[0].rad = 400;
                            break;
                        }
                    }
                    if (around) {
                        break;
                    }

                    if (i < 4) {
                        ChunkSectionPos pos = ChunkSectionPos.from(chnks[i].x, 0, chnks[i].z);
                        for (int j = 0; j < features2.length; j++) {
                            if(features2[j]== StructureFeature.VILLAGE && !config.villages)
                                continue;
                            try {
                                Optional<? extends StructureStart<?>> vv = getStructuresWithChildren2(world, accessor, chnk,
                                        features2[j]).filter((structureStart) -> {
                                            return structureStart.getChildren().stream().anyMatch((piece) -> {
                                                return piece.getBoundingBox().intersectsXZ(pos.getMinX(), pos.getMinZ(),
                                                        pos.getMaxX(), pos.getMaxZ());
                                            });
                                        }).findFirst();
                                if (vv != null && vv.isPresent()) {
                                    around = true;
                                    circles[i].cx = (int) chunkPos2.getStartX()
                                            + (chunkPos2.getEndX() - chunkPos2.getStartX() + 1) / 2;
                                    circles[i].cz = (int) chunkPos2.getStartZ()
                                            + (chunkPos2.getEndZ() - chunkPos2.getStartZ() + 1) / 2;
                                    circles[i].rad = 250;
                                }
                            } catch (RuntimeException e) {

                            }
                        }

                    }
                }
            }
        }
       
        

        /*if (has_struct || around || inside_radius(spawn_cx, spawn_cz, chsx, chsz, rad2)
                || inside_radius(spawn_cx, spawn_cz, chex, chsz, rad2)
                || inside_radius(spawn_cx, spawn_cz, chsx, chez, rad2)
                || inside_radius(spawn_cx, spawn_cz, chex, chez, rad2)) */
        {
            if (has_struct || around) {
                height_end = 62;
                height_start = height_end - 20;
                height_end2 = height_end + height_variation;
            }else{
                height_end = 120;
                height_start = height_end - 20;
                height_end2 = height_end + height_variation;
            }
            float m = (height_end - height_start);
            int yy = height_start + (int) (m / 2) + 1;
            m = m * 2.5f;
            if (around) {
                yy += 6;
            }
            int he = height_end2;            

            for (int j = 0; j < 16; ++j) {
                bx = chsx + j;
                for (int k = 0; k < 16; ++k) {
                    bz = chsz + k;
                    for (int i = height_start; i < he; ++i) {
                        int y = chunk.getBottomY() + i;
                        // if(inside_radius(cshx+j, cshz+k))
                        {
                            if (has_struct || (circles[0].rad != 0 && circles[0].inside(bx, bz))
                                    || (circles[1].rad != 0 && circles[1].inside(bx, bz))
                                    || (circles[2].rad != 0 && circles[2].inside(bx, bz))
                                    || (circles[3].rad != 0 && circles[3].inside(bx, bz))
                                    || point_in_island(bx, i, bz)) {

                                double n = yy + perlinNoiseSampler.sample((bx) * 0.1, 0, (bz) * 0.1, 500, 100) * m;

                                double n2 = height_end
                                        + perlinNoiseSampler.sample((bx) * 0.01, 0, (bz) * 0.01, 500, 100)
                                                * height_variation;
                                if (n2 < height_end) {
                                    n2 = height_end + 1;
                                }
                                if (n >= height_end) {
                                    n = height_end - 3;
                                }
                                // if (!around && n >= height_end) {
                                // n = n + 1;
                                // }
                                if (i > n && i < n2) {
                                    // if (i > n || (!around && i>(height_end-3))) {
                                    // if (i > n || (i>(height_end-3))) {

                                    chunk.setBlockState(mutable.set(j, y, k), stone, false);
                                    heightmap.trackUpdate(j, y, k, stone);
                                    heightmap2.trackUpdate(j, y, k, stone);
                                }
                            }
                        }                        
                    }
                    if(config.generate_small_islands){
                        double scale=0.001;
                        int level=50;
                        int height=30;
                        local_max=0;
                        last_level=(int)(level+simplexSampler.sample((bx) * scale, 0, (bz) * scale)*30);
                        scale=0.06;
                        int midpoint=70;
                        double sp=perlinNoiseSampler.sample((bx) * scale, 0, (bz) * scale);
                        double n = midpoint+ sp * midpoint;
                        if(n<midpoint){
                            n+=last_level;
                            int stop=last_level+height;
                            for (int y2 = (int)n; y2 < stop; ++y2) 
                            {
                                chunk.setBlockState(mutable.set(j, y2, k), stone, false);
                                heightmap.trackUpdate(j, y2, k, stone);
                                heightmap2.trackUpdate(j, y2, k, stone);
                            }
                        }
                    }
                }
            }
        }        
        return CompletableFuture.completedFuture(chunk);
    }

    public void carve(long seed, BiomeAccess access, Chunk chunk, GenerationStep.Carver carver) {
    }

    public int getSeaLevel() {
        return 63;
        // return ((ChunkGeneratorSettings) this.settings.get()).getSeaLevel();
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
        test_block = Blocks.WHITE_CONCRETE.getDefaultState();
        stone = Blocks.STONE.getDefaultState();
        water = Blocks.WATER.getDefaultState();        
        circles[0] = new Circle(0, 0, 0);
        circles[1] = new Circle(0, 0, 0);
        circles[2] = new Circle(0, 0, 0);
        circles[3] = new Circle(0, 0, 0);
        chnks[0] = new Vec2i(0, 0);
        chnks[1] = new Vec2i(0, 0);
        chnks[2] = new Vec2i(0, 0);
        chnks[3] = new Vec2i(0, 0);
        chnks[4] = new Vec2i(0, 0);
        chnks[5] = new Vec2i(0, 0);
        chnks[6] = new Vec2i(0, 0);
        chnks[7] = new Vec2i(0, 0);
        
    }
}
