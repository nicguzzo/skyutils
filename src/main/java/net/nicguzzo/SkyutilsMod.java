package net.nicguzzo;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.block.FabricBlockSettings;
import net.fabricmc.fabric.api.container.ContainerProviderRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.MaterialColor;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ToolMaterials;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.registry.Registry;

import net.nicguzzo.condenser.CondenserBlock;
import net.nicguzzo.condenser.CondenserEntity;
import net.nicguzzo.kiln.KilnBlock;
import net.nicguzzo.kiln.KilnBlockEntity;

public class SkyutilsMod implements ModInitializer {

	public static SkyutilsConfig config;

	public static final Identifier CONDENSER_BLOCK_ID = new Identifier("skyutils", "condenser");
	public static final Identifier CHARCOAL_BLOCK_ID = new Identifier("skyutils", "charcoal_block");

	public static BlockEntityType<CondenserEntity> CONDENSER_ENTITY;
	public static final Block CONDENSER_BLOCK = new CondenserBlock();
	public static final BlockItem CONDENSER_BLOCK_ITEM = new BlockItem(CONDENSER_BLOCK,
			new Item.Settings().group(ItemGroup.MISC));

	public static final Block CHARCOAL_BLOCK = new Block(
			FabricBlockSettings.of(Material.STONE, MaterialColor.BLACK).strength(5.0F, 6.0F).build());
	public static final BlockItem CHARCOAL_BLOCK_ITEM = new BlockItem(CHARCOAL_BLOCK,
			new Item.Settings().group(ItemGroup.MISC));
	public static final Item DIAMOND_NUGGET = new Item(new Item.Settings().group(ItemGroup.MISC));
	public static final Item WOODCHIPS = new Item(new Item.Settings().group(ItemGroup.MISC));
	public static final Item PEBBLE = new Item(new Item.Settings().group(ItemGroup.MISC));
	public static final Item RAW_CRUCIBLE = new Item(new Item.Settings().group(ItemGroup.MISC));
	public static final Crucible CRUCIBLE = new Crucible(Fluids.EMPTY, new Item.Settings().group(ItemGroup.MISC));
	public static final Crucible WATER_CRUCIBLE = new Crucible(Fluids.WATER,
			new Item.Settings().group(ItemGroup.MISC).maxCount(1));
	public static final Crucible LAVA_CRUCIBLE = new Crucible(Fluids.LAVA,
			new Item.Settings().group(ItemGroup.MISC).maxCount(1));

			public static final Hammer WOODEN_HAMMER = new Hammer(ToolMaterials.WOOD, 6, -2.8F,
			(new Item.Settings()).group(ItemGroup.TOOLS));
	public static final Hammer STONE_HAMMER = new Hammer(ToolMaterials.STONE, 6, -2.8F,
			(new Item.Settings()).group(ItemGroup.TOOLS));
	public static final Hammer IRON_HAMMER = new Hammer(ToolMaterials.IRON, 6, -2.8F,
			(new Item.Settings()).group(ItemGroup.TOOLS));
	public static final Hammer DIAMOND_HAMMER = new Hammer(ToolMaterials.DIAMOND, 6, -2.8F,
			(new Item.Settings()).group(ItemGroup.TOOLS));

	public static final Identifier KILN = new Identifier("skyutils", "kiln");
	public static final Block KILN_BLOCK = new KilnBlock(
			FabricBlockSettings.of(Material.STONE).strength(3.5F, 3.5F).build());
	public static final String KILN_BLOCK_TRANSLATION_KEY = Util.createTranslationKey("container", KILN);

	public static BlockEntityType<KilnBlockEntity> KILN_ENTITY_TYPE;

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
		Registry.register(Registry.BLOCK, CONDENSER_BLOCK_ID, CONDENSER_BLOCK);
		Registry.register(Registry.ITEM, CONDENSER_BLOCK_ID, CONDENSER_BLOCK_ITEM);
		CONDENSER_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, "skyutils:condenser_entity", BlockEntityType.Builder.create(CondenserEntity::new, CONDENSER_BLOCK).build(null));

		Registry.register(Registry.BLOCK, CHARCOAL_BLOCK_ID, CHARCOAL_BLOCK);
		Registry.register(Registry.ITEM, CHARCOAL_BLOCK_ID, CHARCOAL_BLOCK_ITEM);

		Registry.register(Registry.BLOCK, KILN, KILN_BLOCK);
		Registry.register(Registry.ITEM, KILN, new BlockItem(KILN_BLOCK, new Item.Settings().group(ItemGroup.REDSTONE)));
		KILN_ENTITY_TYPE = Registry.register(Registry.BLOCK_ENTITY_TYPE, KILN,
				BlockEntityType.Builder.create(KilnBlockEntity::new, KILN_BLOCK).build(null));
		ContainerProviderRegistry.INSTANCE.registerFactory(KILN, (syncId, identifier, player, buf) -> {
			final BlockEntity blockEntity = player.world.getBlockEntity(buf.readBlockPos());
			return ((KilnBlockEntity) blockEntity).createContainer(syncId, player.inventory);
		});

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

}