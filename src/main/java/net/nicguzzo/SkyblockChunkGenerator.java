package net.nicguzzo;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.SharedConstants;
import net.minecraft.structure.PillagerOutpostGenerator;
import net.minecraft.structure.StructureSet;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.dynamic.RegistryOps;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryEntryList;
import net.minecraft.world.*;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ReadableContainer;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.util.PlacedFeatureIndexer;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.*;
import net.nicguzzo.utils.Circle;
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
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;

public final class SkyblockChunkGenerator extends NoiseChunkGenerator {
    private static final Logger LOGGER = LogManager.getLogger();
    private int spawn_radius =60;
    private int spawn_cx = 0;
    private int spawn_cz = 0;
    private Circle island;
    private Circle end_void;
    int island_height=30;
    double noise_factor=0.1;
    double noise_factor2=0.01;
    double noise_factor3=0.001;
    private boolean first=true;
    private PerlinNoiseSampler noise1;
    private PerlinNoiseSampler noise2;
    private PerlinNoiseSampler noise3;
    private boolean is_overworld=false;
    private boolean is_end=false;
    private boolean is_nether=false;
    private int base_island_level=64;
    public static final Codec<SkyblockChunkGenerator> CODEC =
            RecordCodecBuilder.create(instance ->
                            NoiseChunkGenerator.createStructureSetRegistryGetter(instance).and(
                            instance.group(
                               RegistryOps.createRegistryCodec(Registry.NOISE_KEY).forGetter(generator -> generator.noiseRegistry),
                                                            (BiomeSource.CODEC.fieldOf("biome_source")).forGetter(ChunkGenerator::getBiomeSource),
                                                            (ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings")).forGetter(SkyblockChunkGenerator::getSettings)))
                               .apply(instance, instance.stable(SkyblockChunkGenerator::new)));
    private final Registry<DoublePerlinNoiseSampler.NoiseParameters> noiseRegistry;
    private final Supplier<List<PlacedFeatureIndexer.IndexedFeatures>> indexedFeaturesListSupplier;
    public SkyblockChunkGenerator(Registry<StructureSet> structureRegistry, Registry<DoublePerlinNoiseSampler.NoiseParameters> noiseRegistry,
                                  BiomeSource biomeSource, RegistryEntry<ChunkGeneratorSettings> settings) {
        super(structureRegistry, noiseRegistry, biomeSource, settings);

        if(settings.value().defaultBlock().getBlock()==Blocks.STONE){
            is_overworld=true;
        }else if(settings.value().defaultBlock().getBlock()==Blocks.END_STONE){
            is_end=true;
        }else if(settings.value().defaultBlock().getBlock()==Blocks.NETHERRACK){
            is_nether=true;
        }
        this.noiseRegistry = noiseRegistry;
        this.indexedFeaturesListSupplier = Suppliers.memoize(() -> PlacedFeatureIndexer.collectIndexedFeatures(List.copyOf(biomeSource.getBiomes()), biomeEntry -> biomeEntry.value().getGenerationSettings().getFeatures(), true));
    }
    public RegistryEntry<ChunkGeneratorSettings> getSettings() {
        return this.settings;
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
        super.buildSurface(region,structures,noiseConfig,chunk);
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(
            Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor accessor, Chunk chunk) {
        if(is_nether){
            return CompletableFuture.completedFuture(chunk);
        }
        WorldAccess world = accessor.world;
        if (first) {
            first = false;
            spawn_cx = world.getLevelProperties().getSpawnX();
            spawn_cz = world.getLevelProperties().getSpawnZ();
            if(is_overworld) {
                island = new Circle(spawn_cx, spawn_cz, 21);
            }else if(is_end) {
                island = new Circle(0, 0, 49);
                end_void = new Circle(0, 0, 1000);
                base_island_level=50;
            }
            noise1=new PerlinNoiseSampler(Random.create(noiseConfig.getLegacyWorldSeed()));
            noise2=new PerlinNoiseSampler(Random.create(noiseConfig.getLegacyWorldSeed()+1));
            noise3=new PerlinNoiseSampler(Random.create(noiseConfig.getLegacyWorldSeed()+2));
            /*int sample_dist=16;
            boolean center_found=false;
            for (int x = 0; !center_found && x < spawn_cx+sample_dist*2; ++x) {
                for (int z = 0;!center_found && z < spawn_cz+sample_dist*2; ++z) {
                    double n=noise1.sample((spawn_cx-sample_dist+x)*noise_factor,0,(spawn_cz-sample_dist+z)*noise_factor);
                    //LOGGER.info("n: "+n);
                    if(n<-0.3){
                        offx=x;
                        offz=z;
                        center_found=true;
                    }
                }
            }
            LOGGER.info("spawn_cx: "+spawn_cx+ " spawn_cz: "+spawn_cz);
            LOGGER.info("Center found "+ center_found + " offx: "+offx+ " offz: "+offz);*/
        }
        ChunkPos pos=chunk.getPos();
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        Heightmap heightmap = chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
        Heightmap heightmap2 = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
        //BlockState blockState = (BlockState) Blocks.WHITE_STAINED_GLASS.getDefaultState();
        //BlockState blockState = (BlockState) Blocks.STONE.getDefaultState();
        BlockState blockState = getSettings().value().defaultBlock();
        int x0=pos.getStartX();
        int z0=pos.getStartZ();
        int y0 =base_island_level ;
        int xx,zz;
        boolean has_struct=false;
        if(is_overworld) {
            if(pos.x==-9 && pos.z==6){
                System.out.println("pos");
            }
            Map<Structure, LongSet> structs=chunk.getStructureReferences();

            //has_struct =! structs.isEmpty();
            for (Structure key : structs.keySet()) {
                has_struct = has_struct||
                        (key instanceof SwampHutStructure) ||
                        (key instanceof DesertPyramidStructure || (key instanceof WoodlandMansionStructure) ||
                        (key instanceof JungleTempleStructure) || (key instanceof IglooStructure));
                /*if(key instanceof JigsawStructure){
                    JigsawStructure s= (JigsawStructure)key;

                }*/
            }

            /*has_struct= has_struct|| (StructurePresence.START_PRESENT==accessor.getStructurePresence(pos,Structures.VILLAGE_DESERT.value(),false));
            has_struct= has_struct|| (StructurePresence.START_PRESENT==accessor.getStructurePresence(pos,Structures.VILLAGE_PLAINS.value(),false));
            has_struct= has_struct|| (StructurePresence.START_PRESENT==accessor.getStructurePresence(pos,Structures.VILLAGE_SAVANNA.value(),false));
            has_struct= has_struct|| (StructurePresence.START_PRESENT==accessor.getStructurePresence(pos,Structures.VILLAGE_SNOWY.value(),false));
            has_struct= has_struct|| (StructurePresence.START_PRESENT==accessor.getStructurePresence(pos,Structures.VILLAGE_TAIGA.value(),false));

            has_struct= has_struct|| (StructurePresence.START_PRESENT==accessor.getStructurePresence(pos,Structures.SWAMP_HUT.value(),false));*/

        }
        for (int x = 0; x < 16; ++x) {
            xx=x0 + x;
             for (int z = 0; z < 16; ++z) {
                 zz=z0+z;
                 double island_n=noise2.sample((xx)*noise_factor2,0,(zz)*noise_factor2);
                 boolean spawn=island.inside(xx,zz);
                 if(island_n>0.3 || spawn || has_struct) {
                     y0=base_island_level+(int)(noise3.sample((xx)*noise_factor3,0,(zz)*noise_factor3)*64);
                     int n = (int) (noise1.sample(xx * noise_factor, 0, zz * noise_factor) * (island_height*(island_n*2)));
                     /*if(si){
                         chunk.setBlockState(mutable.set(x, 0, z), blockState, false);
                     }*/
                     if(spawn || has_struct /*|| (is_end && end_portal_island.inside(xx,zz))*/) {
                         if (n > 0) n=-n;
                     }else{
                         if(is_end && end_void.inside(xx,zz)){
                             n=1;
                         }
                     }
                     if (n <= 0) {
                         int m = y0 + n;
                         for (int y = m; y <= y0; ++y) {
                             chunk.setBlockState(mutable.set(x, y, z), blockState, false);
                             heightmap.trackUpdate(x, y, z, blockState);
                             heightmap2.trackUpdate(x, y, z, blockState);
                         }
                     }
                 }
            }
        }
        /*if(spawn_cx >=pos.getStartX() && spawn_cx < pos.getEndX() && spawn_cz >=pos.getStartZ() && spawn_cz < pos.getEndZ()) {
            //List<BlockState> list = this.config.getLayerBlocks();
            BlockPos.Mutable mutable = new BlockPos.Mutable();
            Heightmap heightmap = chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
            Heightmap heightmap2 = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);

            //for (int i = 0; i < Math.min(chunk.getHeight(), list.size()); ++i) {
                //BlockState blockState = (BlockState) list.get(i);
            int i=64;
            BlockState blockState = (BlockState) Blocks.GLASS.getDefaultState();
                if (blockState != null) {
                    int j = chunk.getBottomY() + i;

                    for (int k = 0; k < 16; ++k) {
                        for (int l = 0; l < 16; ++l) {
                            chunk.setBlockState(mutable.set(k, j, l), blockState, false);
                            heightmap.trackUpdate(k, j, l, blockState);
                            heightmap2.trackUpdate(k, j, l, blockState);
                        }
                    }
                }
            //}
        }*/
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess access, StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carver) {
    }

    @Override
    public void populateEntities(ChunkRegion region) {
    }

    public NoiseConfig getNoiseConfig(StructureWorldAccess world) {
        return NoiseConfig.create(this.settings.value(), world.getRegistryManager().get(Registry.NOISE_KEY), world.getSeed());
    }
    static {
        Registry.register(Registry.CHUNK_GENERATOR, SkyutilsMod.SKYBLOCK, SkyblockChunkGenerator.CODEC);
    }

    public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {
        ChunkPos chunkPos2 = chunk.getPos();
        if (SharedConstants.isOutsideGenerationArea(chunkPos2)) {
            return;
        }
        ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(chunkPos2, world.getBottomSectionCoord());
        BlockPos blockPos = chunkSectionPos.getMinPos();
        Registry<Structure> registry = world.getRegistryManager().get(Registry.STRUCTURE_KEY);
        Map<Integer, List<Structure>> map = registry.stream().collect(Collectors.groupingBy(structureType -> structureType.getFeatureGenerationStep().ordinal()));
        List<PlacedFeatureIndexer.IndexedFeatures> list = this.indexedFeaturesListSupplier.get();
        ChunkRandom chunkRandom = new ChunkRandom(new Xoroshiro128PlusPlusRandom(RandomSeed.getSeed()));
        long l = chunkRandom.setPopulationSeed(world.getSeed(), blockPos.getX(), blockPos.getZ());
        ObjectArraySet<Biome> biome_set = new ObjectArraySet();
        ChunkPos.stream(chunkSectionPos.toChunkPos(), 1).forEach(chunkPos -> {
            Chunk chunk2 = world.getChunk(chunkPos.x, chunkPos.z);
            for (ChunkSection chunkSection : chunk2.getSectionArray()) {
                chunkSection.getBiomeContainer().forEachValue(registryEntry -> biome_set.add(registryEntry.value()));
                //chunkSection.getBiomeContainer().forEachValue(set::add);
            }
        });
        biome_set.retainAll(this.biomeSource.getBiomes());
        int i = list.size();
        try {
            Registry<PlacedFeature> registry2 = world.getRegistryManager().get(Registry.PLACED_FEATURE_KEY);
            int j = Math.max(GenerationStep.Feature.values().length, i);
            for (int k = 0; k < j; ++k) {
                int m = 0;
                if (structureAccessor.shouldGenerateStructures()) {
                    List<Structure> list2 = map.getOrDefault(k, Collections.emptyList());
                    for (Structure structure : list2) {
                        chunkRandom.setDecoratorSeed(l, m, k);
                        Supplier<String> supplier = () -> registry.getKey(structure).map(Object::toString).orElseGet(structure::toString);
                        try {
                            if (       structure instanceof NetherFortressStructure
                                    || structure instanceof MineshaftStructure
                                    || structure instanceof OceanMonumentStructure
                                    || structure instanceof JigsawStructure
                            ){

                            }else {
                                world.setCurrentlyGeneratingStructureName(supplier);
                                structureAccessor.getStructureStarts(chunkSectionPos, structure).forEach(
                                        start -> start.place(world, structureAccessor,
                                                this, chunkRandom,
                                                ChunkGenerator.getBlockBoxForChunk(chunk), chunkPos2));
                            }
                        }
                        catch (Exception exception) {
                            CrashReport crashReport = CrashReport.create(exception, "Feature placement");
                            crashReport.addElement("Feature").add("Description", supplier::get);
                            throw new CrashException(crashReport);
                        }
                        ++m;
                    }
                }
                if (k >= i) continue;
                IntArraySet intSet = new IntArraySet();
                for (Biome biome : biome_set) {
                    List<RegistryEntryList<PlacedFeature>> list3 = biome.getGenerationSettings().getFeatures();
                    if (k >= list3.size()) continue;
                    RegistryEntryList<PlacedFeature> registryEntryList = list3.get(k);
                    PlacedFeatureIndexer.IndexedFeatures indexedFeatures = list.get(k);
                    registryEntryList.stream().map(RegistryEntry::value).forEach(placedFeature -> intSet.add(indexedFeatures.indexMapping().applyAsInt((PlacedFeature)placedFeature)));
                }
                int n = intSet.size();
                int[] is = intSet.toIntArray();
                Arrays.sort(is);
                PlacedFeatureIndexer.IndexedFeatures indexedFeatures2 = list.get(k);
                for (int o = 0; o < n; ++o) {
                    int p = is[o];
                    PlacedFeature placedFeature2 = indexedFeatures2.features().get(p);
                    Supplier<String> supplier2 = () -> registry2.getKey(placedFeature2).map(Object::toString).orElseGet(placedFeature2::toString);
                    chunkRandom.setDecoratorSeed(l, p, k);
                    try {
                        world.setCurrentlyGeneratingStructureName(supplier2);
                        placedFeature2.generate(world, this, chunkRandom, blockPos);
                        continue;
                    }
                    catch (Exception exception2) {
                        CrashReport crashReport2 = CrashReport.create(exception2, "Feature placement");
                        crashReport2.addElement("Feature").add("Description", supplier2::get);
                        throw new CrashException(crashReport2);
                    }
                }
            }
            world.setCurrentlyGeneratingStructureName(null);
        }
        catch (Exception exception3) {
            CrashReport crashReport3 = CrashReport.create(exception3, "Biome decoration");
            crashReport3.addElement("Generation").add("CenterX", chunkPos2.x).add("CenterZ", chunkPos2.z).add("Seed", l);
            throw new CrashException(crashReport3);
        }
    }

    /*
    public static final Codec<SkyblockChunkGenerator> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter((skyblockChunkGenerator) -> {
            return skyblockChunkGenerator.biomeSource;
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
    private int spawn_cx = 0;
    private int spawn_cz = 0;
    private int separation = 20;
    private int height_variation = 30;
    private int height_end = 120;
    private int height_end2 = height_end + height_variation;
    private int height_start = height_end - 20;
    private int bx = 0;
    private int bz = 0;
    private int rad2 = 0;

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
    //protected final Supplier<ChunkGeneratorSettings> settings;

    Structure<?>[] features = { Structure.SWAMP_HUT, Structure.VILLAGE,
            Structure.DESERT_PYRAMID, Structure.IGLOO, Structure.JUNGLE_PYRAMID,
            Structure.PILLAGER_OUTPOST, Structure.MANSION };
    Structure<?>[] features2 = { Structure.VILLAGE, Structure.DESERT_PYRAMID,
            Structure.PILLAGER_OUTPOST, Structure.MANSION };

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

        ChunkRandom chunkRandom = new ChunkRandom(Random.create(seed));
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
        return new SkyblockChunkGenerator(this.biomeSource.withSeed(seed), seed, this.settings);
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
        if (accessor.getStructureAt(pos, true, Structure.SWAMP_HUT).hasChildren()) {
            if (group == SpawnGroup.MONSTER) {
                return Structure.SWAMP_HUT.getMonsterSpawns();
            }

            if (group == SpawnGroup.CREATURE) {
                return Structure.SWAMP_HUT.getCreatureSpawns();
            }
        }

        if (group == SpawnGroup.MONSTER) {
            if (accessor.getStructureAt(pos, false, Structure.PILLAGER_OUTPOST).hasChildren()) {
                return Structure.PILLAGER_OUTPOST.getMonsterSpawns();
            }

            if (accessor.getStructureAt(pos, false, Structure.MONUMENT).hasChildren()) {
                return Structure.MONUMENT.getMonsterSpawns();
            }

            if (accessor.getStructureAt(pos, true, Structure.FORTRESS).hasChildren()) {
                return Structure.FORTRESS.getMonsterSpawns();
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
            boolean matchChildren, Structure<?> feature) {
        Optional<? extends StructureStart<?>> s = getStructuresWithChildren2(world, accessor, chunk, feature)
                .filter((structureStart) -> {
                    return structureStart.getChildren().stream().anyMatch((piece) -> {
                        return piece.getBoundingBox().intersectsXZ(pos.getMinX(), pos.getMinZ(), pos.getMaxX(),
                                pos.getMaxZ());
                    });
                }).findFirst();

        if (s.isPresent())
            return true;
        return false;
    }

    public Stream<? extends StructureStart<?>> getStructuresWithChildren2(WorldAccess world, StructureAccessor accessor,
            Chunk chunk, Structure<?> feature) {
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
                if(features[j]== Structure.VILLAGE && !config.villages)
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
                        if(features[j]== Structure.VILLAGE && !config.villages)
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
                            if(features2[j]== Structure.VILLAGE && !config.villages)
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
            ChunkRandom chunkRandom = new ChunkRandom(Random.create());
            chunkRandom.setPopulationSeed(region.getSeed(), chunkPos.getStartX(), chunkPos.getStartZ());
            SpawnHelper.populateEntities(region, region.getBiome(chunkPos.getStartPos()), chunkPos, chunkRandom);
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
        
    }*/
}
