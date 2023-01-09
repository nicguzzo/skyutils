package net.nicguzzo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.noise.PerlinNoiseSampler;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.util.PlacedFeatureIndexer;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.*;
import net.nicguzzo.utils.Circle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.function.Supplier;

public final class SkyblockChunkGenerator extends NoiseChunkGenerator {
    private static final Logger LOGGER = LogManager.getLogger();

    private int spawn_cx = 0;
    private int spawn_cz = 0;
    private long seed=0;
    private Circle island;
    private Circle end_void;
    SkyutilsConfig conf=null;
    /*int spawn_radius =21;
    int island_height=30;
    double noise_factor=0.1;//island_bottom_noise_xz_factor
    double noise_factor2=0.01;//island_chance_noise_xz_factor
    double noise_factor3=0.001;//island_top_noise_xz_factor
    int noise3_scale=64;
    double island_chance=0.3;
    int base_island_level=120;*/

    private boolean first=true;
    private PerlinNoiseSampler noise1;
    private PerlinNoiseSampler noise2;
    private PerlinNoiseSampler noise3;
    private boolean is_overworld=false;
    private boolean is_end=false;
    private boolean is_nether=false;


     public static final Codec<SkyblockChunkGenerator> CODEC =
    RecordCodecBuilder.create(
      instance ->
        instance
          .group(
            (BiomeSource.CODEC.fieldOf("biome_source")).forGetter(ChunkGenerator::getBiomeSource),
            (ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings")).forGetter(SkyblockChunkGenerator::getSettings))
          .apply(instance, instance.stable(SkyblockChunkGenerator::new)));

    //private final Supplier<List<PlacedFeatureIndexer.IndexedFeatures>> indexedFeaturesListSupplier;

    public SkyblockChunkGenerator(BiomeSource biomeSource, RegistryEntry<ChunkGeneratorSettings> settings) {
        super(biomeSource, settings);

        if(settings.value().defaultBlock().getBlock()==Blocks.STONE){
            is_overworld=true;
        }else if(settings.value().defaultBlock().getBlock()==Blocks.END_STONE){
            is_end=true;
        }else if(settings.value().defaultBlock().getBlock()==Blocks.NETHERRACK){
            is_nether=true;
        }
        //this.indexedFeaturesListSupplier = Suppliers.memoize(() -> PlacedFeatureIndexer.collectIndexedFeatures(List.copyOf(biomeSource.getBiomes()), biomeEntry -> biomeEntry.value().getGenerationSettings().getFeatures(), true));

    }
    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(
            Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor accessor, Chunk chunk) {
        if(is_nether){
            return CompletableFuture.completedFuture(chunk);
        }
        WorldAccess world = accessor.world;
        if (first) {
            conf=SkyutilsConfig.get_instance();
            first = false;
            spawn_cx = world.getLevelProperties().getSpawnX();
            spawn_cz = world.getLevelProperties().getSpawnZ();
            if(is_overworld) {
                island = new Circle(spawn_cx, spawn_cz, conf.spawn_island_radius);
            }else if(is_end) {
                island = new Circle(0, 0, 49);
                end_void = new Circle(0, 0, 1000);
                conf.base_island_level=conf.end_base_island_level;
            }
            seed=accessor.options.getSeed();
            noise1=new PerlinNoiseSampler(Random.create(seed));
            noise2=new PerlinNoiseSampler(Random.create(seed+1));
            noise3=new PerlinNoiseSampler(Random.create(seed+2));
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
        int y0 ;
        int xx,zz;
        boolean has_struct=false;
        if(is_overworld) {
            /*if(pos.x==-9 && pos.z==6){
                System.out.println("pos");
            }*/
            Map<Structure, LongSet> structs=chunk.getStructureReferences();

            //has_struct =! structs.isEmpty();
            for (Structure key : structs.keySet()) {
                has_struct = has_struct||
                           (key instanceof SwampHutStructure)
                        || (key instanceof DesertPyramidStructure
                        || (key instanceof WoodlandMansionStructure)
                        || (key instanceof IglooStructure)
                        || (key instanceof JungleTempleStructure)
                        || (key instanceof EndCityStructure)
                        || (key instanceof StrongholdStructure)
                        //|| (key.== StructureKeys.VILLAGE_PLAINS)
                );
                /*if(key instanceof JigsawStructure){
                    StructureKeys.VILLAGE_PLAINS.
                    JigsawStructure s= (JigsawStructure)key;
                    s.getFeatureGenerationStep()
                    //StructureType<?> st= s.getType();
                }*/

            }

            /*has_struct= has_struct|| (StructurePresence.START_PRESENT==accessor.getStructurePresence(pos,Structures.VILLAGE_DESERT.value(),false));
            has_struct= has_struct|| (StructurePresence.START_PRESENT==accessor.getStructurePresence(pos,Structures.VILLAGE_PLAINS.value(),false));
            has_struct= has_struct|| (StructurePresence.START_PRESENT==accessor.getStructurePresence(pos,Structures.VILLAGE_SAVANNA.value(),false));
            has_struct= has_struct|| (StructurePresence.START_PRESENT==accessor.getStructurePresence(pos,Structures.VILLAGE_SNOWY.value(),false));
            has_struct= has_struct|| (StructurePresence.START_PRESENT==accessor.getStructurePresence(pos,Structures.VILLAGE_TAIGA.value(),false));

            has_struct= has_struct|| (StructurePresence.START_PRESENT==accessor.getStructurePresence(pos,Structures.SWAMP_HUT.value(),false));*/

        }
        //flat world with all biomes
        /*for (int x = 0; x < 16; ++x) {
            xx=x0 + x;
             for (int z = 0; z < 16; ++z) {
                 for (int y =50; y <= 64; ++y) {
                     chunk.setBlockState(mutable.set(x, y, z), blockState, false);
                     heightmap.trackUpdate(x, y, z, blockState);
                     heightmap2.trackUpdate(x, y, z, blockState);
                 }
            }
        }*/
        for (int x = 0; x < 16; ++x) {
            xx=x0 + x;
             for (int z = 0; z < 16; ++z) {
                 zz=z0+z;
                 double island_n=noise2.sample((xx)*conf.noise_factor2,0,(zz)*conf.noise_factor2);
                 boolean spawn=island.inside(xx,zz);
                 if(island_n>conf.island_chance || spawn || has_struct) {
                     y0=conf.base_island_level+(int)(noise3.sample((xx)*conf.noise_factor3,0,(zz)*conf.noise_factor3)*conf.noise3_scale);
                     int n = (int) (noise1.sample(xx * conf.noise_factor, 0, zz * conf.noise_factor) * (conf.island_height*(island_n*2)));
                     //if(si){
                     //    chunk.setBlockState(mutable.set(x, 0, z), blockState, false);
                     //}
                     //|| (is_end && end_portal_island.inside(xx,zz))
                     if(spawn || has_struct ) {
                         if (n > 0) n=-n;
                     }else{
                         if(is_end && end_void.inside(xx,zz)){
                             n=1;
                         }
                     }
                     if (n <= 0) {
                         int m = y0 + n;
                         int y2=y0+3;//3 solid blocks always
                         for (int y = m; y <= y2; ++y) {
                             chunk.setBlockState(mutable.set(x, y, z), blockState, false);
                             heightmap.trackUpdate(x, y, z, blockState);
                             heightmap2.trackUpdate(x, y, z, blockState);
                         }
                     }
                 }
            }
        }

        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess access, StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carver) {
    }
    public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {
        ChunkPos chunkPos2 = chunk.getPos();
        if (SharedConstants.isOutsideGenerationArea(chunkPos2)) {
            return;
        }
        ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(chunkPos2, world.getBottomSectionCoord());
        BlockPos blockPos = chunkSectionPos.getMinPos();
        Registry<Structure> registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        Map<Integer, List<Structure>> map = registry.stream().collect(Collectors.groupingBy(structureType -> structureType.getFeatureGenerationStep().ordinal()));
        List<PlacedFeatureIndexer.IndexedFeatures> list = this.indexedFeaturesListSupplier.get();
        ChunkRandom chunkRandom = new ChunkRandom(new Xoroshiro128PlusPlusRandom(RandomSeed.getSeed()));
        long l = chunkRandom.setPopulationSeed(world.getSeed(), blockPos.getX(), blockPos.getZ());
        ObjectArraySet<RegistryEntry<Biome>> set = new ObjectArraySet<>();

        ChunkPos.stream(chunkSectionPos.toChunkPos(), 1).forEach(chunkPos -> {
            Chunk _chunk = world.getChunk(chunkPos.x, chunkPos.z);
            for (ChunkSection chunkSection : _chunk.getSectionArray()) {
                chunkSection.getBiomeContainer().forEachValue(set::add);
            }
        });
        set.retainAll(this.biomeSource.getBiomes());
        int i = list.size();
        try {
            Registry<PlacedFeature> registry2 = world.getRegistryManager().get(RegistryKeys.PLACED_FEATURE);
            int j = Math.max(GenerationStep.Feature.values().length, i);
            for (int k = 0; k < j; ++k) {
                int m = 0;
                if (structureAccessor.shouldGenerateStructures()) {
                    List<Structure> list2 = map.getOrDefault(k, Collections.emptyList());
                    for (Structure structure : list2) {
                        chunkRandom.setDecoratorSeed(l, m, k);
                        Supplier<String> supplier = () -> registry.getKey(structure).map(Object::toString).orElseGet(structure::toString);
                        try {
                            if (    !(   structure instanceof NetherFortressStructure
                                    || structure instanceof MineshaftStructure
                                    || structure instanceof OceanMonumentStructure
                                    || structure instanceof JigsawStructure
                                    )
                            ){
                                world.setCurrentlyGeneratingStructureName(supplier);
                                structureAccessor.getStructureStarts(chunkSectionPos, structure).forEach(start -> start.place(world, structureAccessor, this, chunkRandom, ChunkGenerator.getBlockBoxForChunk(chunk), chunkPos2));
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
                for (RegistryEntry<Biome> registryEntry : set) {
                    List<RegistryEntryList<PlacedFeature>> list3 = this.generationSettingsGetter.apply(registryEntry).getFeatures();
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
}
