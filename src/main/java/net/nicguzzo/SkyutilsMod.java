package net.nicguzzo;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.container.ContainerProviderRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.block.MaterialColor;
import net.minecraft.block.Block.Settings;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ToolMaterials;
import net.minecraft.util.Identifier;
import net.minecraft.util.PackedIntegerArray;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.IWorld;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.level.LevelGeneratorType;
import net.minecraft.server.world.ServerLightingProvider;

import net.minecraft.world.ChunkRegion;

import net.minecraft.world.World;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.BiomeSourceType;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSourceConfig;
import net.minecraft.world.gen.chunk.CavesChunkGenerator;
import net.minecraft.world.gen.chunk.CavesChunkGeneratorConfig;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorConfig;
import net.minecraft.world.gen.chunk.ChunkGeneratorType;
import net.minecraft.world.gen.chunk.OverworldChunkGenerator;
import net.minecraft.world.gen.chunk.OverworldChunkGeneratorConfig;

//import net.nicguzzo.condenser.CondenserBlock;
//import net.nicguzzo.condenser.CondenserEntity;
import net.nicguzzo.kiln.KilnBlock;
import net.nicguzzo.kiln.KilnBlockEntity;

public class SkyutilsMod implements ModInitializer {

	public static LevelGeneratorType SKB_LEVEL_GENERATOR_TYPE;
	public static SkyutilsConfig config;

	public static final Identifier KILN               = new Identifier("skyutils", "kiln");
	public static final Identifier CONDENSER = new Identifier("skyutils", "condenser");
	public static final Identifier CHARCOAL_BLOCK_ID  = new Identifier("skyutils", "charcoal_block");

	
	public static final Block CHARCOAL_BLOCK = new Block(Settings.of(Material.STONE, MaterialColor.BLACK).strength(5.0F, 6.0F));
	public static final BlockItem CHARCOAL_BLOCK_ITEM = new BlockItem(CHARCOAL_BLOCK,new Item.Settings().group(ItemGroup.MISC));
	public static final Item DIAMOND_NUGGET = new Item(new Item.Settings().group(ItemGroup.MISC));
	public static final Item WOODCHIPS = new Item(new Item.Settings().group(ItemGroup.MISC));
	public static final Item PEBBLE = new Item(new Item.Settings().group(ItemGroup.MISC));
	public static final Item RAW_CRUCIBLE = new Item(new Item.Settings().group(ItemGroup.MISC));
	public static final Crucible CRUCIBLE = new Crucible(Fluids.EMPTY, new Item.Settings().group(ItemGroup.MISC));
	public static final Crucible WATER_CRUCIBLE = new Crucible(Fluids.WATER,new Item.Settings().group(ItemGroup.MISC).maxCount(1));
	public static final Crucible LAVA_CRUCIBLE = new Crucible(Fluids.LAVA,new Item.Settings().group(ItemGroup.MISC).maxCount(1));
	public static final Hammer WOODEN_HAMMER = new Hammer(ToolMaterials.WOOD, 6, -2.8F,(new Item.Settings()).group(ItemGroup.TOOLS));
	public static final Hammer STONE_HAMMER = new Hammer(ToolMaterials.STONE, 6, -2.8F,(new Item.Settings()).group(ItemGroup.TOOLS));
	public static final Hammer IRON_HAMMER = new Hammer(ToolMaterials.IRON, 6, -2.8F,(new Item.Settings()).group(ItemGroup.TOOLS));
	public static final Hammer DIAMOND_HAMMER = new Hammer(ToolMaterials.DIAMOND, 6, -2.8F,(new Item.Settings()).group(ItemGroup.TOOLS));

	//KILN
	public static final Block KILN_BLOCK = new KilnBlock(Settings.of(Material.STONE).strength(3.5F, 3.5F));
	public static final String KILN_BLOCK_TRANSLATION_KEY = Util.createTranslationKey("container", KILN);
	public static BlockEntityType<KilnBlockEntity> KILN_ENTITY_TYPE;

	//CONDENSER
	public static final Block CONDENSER_BLOCK = new CondenserBlock();
	public static BlockEntityType<CondenserEntity> CONDENSER_ENTITY;		
	public static final BlockItem CONDENSER_BLOCK_ITEM = new BlockItem(CONDENSER_BLOCK,new Item.Settings().group(ItemGroup.MISC));

	public static BlockPos spwn;

	@Override
	public void onInitialize() {

		load_config();
		// items
		Registry.register(Registry.ITEM, new Identifier("skyutils", "wooden_hammer"), WOODEN_HAMMER);
		Registry.register(Registry.ITEM, new Identifier("skyutils", "stone_hammer"), STONE_HAMMER);
		Registry.register(Registry.ITEM, new Identifier("skyutils", "iron_hammer"), IRON_HAMMER);
		Registry.register(Registry.ITEM, new Identifier("skyutils", "diamond_hammer"), DIAMOND_HAMMER);
		Registry.register(Registry.ITEM, new Identifier("skyutils", "woodchips"), WOODCHIPS);
		Registry.register(Registry.ITEM, new Identifier("skyutils", "pebble"), PEBBLE);
		Registry.register(Registry.ITEM, new Identifier("skyutils", "raw_crucible"), RAW_CRUCIBLE);
		Registry.register(Registry.ITEM, new Identifier("skyutils", "crucible"), CRUCIBLE);
		Registry.register(Registry.ITEM, new Identifier("skyutils", "water_crucible"), WATER_CRUCIBLE);
		Registry.register(Registry.ITEM, new Identifier("skyutils", "lava_crucible"), LAVA_CRUCIBLE);
		Registry.register(Registry.ITEM, new Identifier("skyutils", "diamond_nugget"), DIAMOND_NUGGET);

		// blocks
		
		//charcoal block
		Registry.register(Registry.BLOCK, CHARCOAL_BLOCK_ID, CHARCOAL_BLOCK);
		Registry.register(Registry.ITEM, CHARCOAL_BLOCK_ID, CHARCOAL_BLOCK_ITEM);

		//kiln
		Registry.register(Registry.BLOCK, KILN, KILN_BLOCK);
		Registry.register(Registry.ITEM, KILN, new BlockItem(KILN_BLOCK, new Item.Settings().group(ItemGroup.REDSTONE)));
		KILN_ENTITY_TYPE = Registry.register(Registry.BLOCK_ENTITY_TYPE, KILN,BlockEntityType.Builder.create(KilnBlockEntity::new, KILN_BLOCK).build(null));
		
		ContainerProviderRegistry.INSTANCE.registerFactory(KILN, (syncId, identifier, player, buf) -> {
			final BlockEntity blockEntity = player.world.getBlockEntity(buf.readBlockPos());
			return ((KilnBlockEntity) blockEntity).createContainer(syncId, player.inventory);
		});
		
		//condenser
		Registry.register(Registry.BLOCK, CONDENSER, CONDENSER_BLOCK);
		Registry.register(Registry.ITEM,  CONDENSER, CONDENSER_BLOCK_ITEM);
		CONDENSER_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, CONDENSER, BlockEntityType.Builder.create(CondenserEntity::new, CONDENSER_BLOCK).build(null));

	}

	private void load_config() {
		File configFile = new File(FabricLoader.getInstance().getConfigDirectory(), "skyutils.json");
		try (FileReader reader = new FileReader(configFile)) {
			config = new Gson().fromJson(reader, SkyutilsConfig.class);
			try (FileWriter writer = new FileWriter(configFile)) {
				writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(config));
			} catch (IOException e2) {
				System.out.println("Failed to update config file!");
			}
			System.out.println("Config loaded!");

		} catch (IOException e) {
			System.out.println("No config found, generating!");
			config = new SkyutilsConfig();
			try (FileWriter writer = new FileWriter(configFile)) {
				writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(config));
			} catch (IOException e2) {
				System.out.println("Failed to generate config file!");
			}
		}
	}

	private static void deleteBlocks(ProtoChunk chunk, IWorld world) {
		ChunkSection[] sections = chunk.getSectionArray();
		for (int i = 0; i < sections.length; i++) {
				sections[i] = WorldChunk.EMPTY_SECTION;
		}
		for (BlockPos bePos : chunk.getBlockEntityPositions()) {
				chunk.removeBlockEntity(bePos);
		}
		//((ProtoChunkAccessor) chunk).getLightSources().clear();
		long[] emptyHeightmap = new PackedIntegerArray(9, 256).getStorage();
		for (Map.Entry<Heightmap.Type, Heightmap> heightmapEntry : chunk.getHeightmaps()) {
				heightmapEntry.getValue().setTo(emptyHeightmap);
		}
		//processStronghold(chunk, world);
		Heightmap.populateHeightmaps(chunk, EnumSet.allOf(Heightmap.Type.class));
	}

	private static void clearChunk(ProtoChunk chunk, IWorld world) {
		deleteBlocks(chunk, world);
		// erase entities
		//chunk.getEntities().clear();
		try {
			((ServerLightingProvider) chunk.getLightingProvider()).light(chunk, true).get();
			ChunkPos pos=chunk.getPos();
			int x=world.getLevelProperties().getSpawnX();
			int y=world.getLevelProperties().getSpawnY();
			int z=world.getLevelProperties().getSpawnZ();			
			
			if(    x > pos.getStartX() && x < pos.getEndX()
				&& z > pos.getStartZ() && z < pos.getEndZ()
			){
				System.out.println(" placing spawn point: " + SkyutilsMod.spwn);
				world.setBlockState(spwn, Blocks.GRASS_BLOCK.getDefaultState(), 2);			
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
	
	public static class SkbGenerator extends OverworldChunkGenerator {

		public SkbGenerator(IWorld world, BiomeSource biomeSource, OverworldChunkGeneratorConfig config) {
			super(world, biomeSource, config);
			
			//world.getDimension().
			int x=world.getLevelProperties().getSpawnX();
			int y=world.getLevelProperties().getSpawnY();
			int z=world.getLevelProperties().getSpawnZ();
			SkyutilsMod.spwn = new BlockPos(x,y,z);
			System.out.println("spawn point: " + SkyutilsMod.spwn);								
			/*if(World.isValid(spw) && world.isAir(spw)){
				world.setBlockState(spw, Blocks.GRASS_BLOCK.getDefaultState(), 2);			
			}*/
		}

		@Override
		public void populateEntities(ChunkRegion region) {
			ProtoChunk chunk = (ProtoChunk) region.getChunk(region.getCenterChunkX(), region.getCenterChunkZ());
			SkyutilsMod.clearChunk(chunk, world);
		}
	}

	public static class CavesGenerator extends CavesChunkGenerator {
		public CavesGenerator(World world, BiomeSource biomeSource, CavesChunkGeneratorConfig config) {
			super(world, biomeSource, config);
		}

		@Override
		public void populateEntities(ChunkRegion region) {
			ProtoChunk chunk = (ProtoChunk) region.getChunk(region.getCenterChunkX(), region.getCenterChunkZ());
			clearChunk(chunk, world);
		}
	}

	public static ChunkGenerator<? extends ChunkGeneratorConfig> createOWGen(World world) {
		ChunkGeneratorType<OverworldChunkGeneratorConfig, OverworldChunkGenerator> chunkGeneratorType = ChunkGeneratorType.SURFACE;
		BiomeSourceType<VanillaLayeredBiomeSourceConfig, VanillaLayeredBiomeSource> biomeSourceType = BiomeSourceType.VANILLA_LAYERED;
		OverworldChunkGeneratorConfig chunkGeneratorConfig = chunkGeneratorType.createSettings();
		VanillaLayeredBiomeSourceConfig biomeSourceConfig = biomeSourceType.getConfig(world.getLevelProperties());
		//.setLevelProperties(world.getLevelProperties())

		biomeSourceConfig.setGeneratorSettings(chunkGeneratorConfig);
		return new SkbGenerator(world, biomeSourceType.applyConfig(biomeSourceConfig), chunkGeneratorConfig);
	}
	
	public static ChunkGenerator<? extends ChunkGeneratorConfig> createNthChunkGenerator(World world) {
		CavesChunkGeneratorConfig config = ChunkGeneratorType.CAVES.createSettings();
		config.setDefaultBlock(Blocks.NETHERRACK.getDefaultState());
		config.setDefaultFluid(Blocks.LAVA.getDefaultState());
		return new CavesGenerator(world, BiomeSourceType.FIXED
				.applyConfig((BiomeSourceType.FIXED.getConfig(world.getLevelProperties())).setBiome(Biomes.NETHER)), config);
	}
	
}