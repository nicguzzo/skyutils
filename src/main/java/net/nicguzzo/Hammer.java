package net.nicguzzo;


import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.stat.Stats;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hammer extends MiningToolItem {
    private static final TagKey<Block> EFFECTIVE_BLOCKS;
    private static final Item[] DIRT_DROPS = { Items.OAK_SAPLING, Items.ACACIA_SAPLING, Items.SPRUCE_SAPLING,
            Items.JUNGLE_SAPLING, Items.DARK_OAK_SAPLING, Items.BIRCH_SAPLING, Items.PUMPKIN_SEEDS, Items.MELON_SEEDS,
            Items.BEETROOT_SEEDS, Items.COCOA_BEANS, Items.SWEET_BERRIES, Items.BAMBOO, Items.SUGAR_CANE,Items.AZALEA,
            Items.POINTED_DRIPSTONE,Items.GLOW_BERRIES,Items.SMALL_DRIPLEAF,Items.BIG_DRIPLEAF,Items.MANGROVE_PROPAGULE};
    private static float sand_redsand_ratio=0.2f;
    private static float iron_nugget_chance=0.1f;
    private static float redstone_from_redsand_chance=0.2f;
    private static float redstone_from_sand_chance=0.1f;
    private static float podzol_extra_chance=0.2f;
    private static float grass_extra_chance=0.2f;
    private static float dirt_extra_chance=0.2f;
    private static float sand_extra_chance=0.2f;
    private static float diamond_nugget_chance=0.2f;
    private static float nether_wart_chance=0.1f;
    private static float netherite_scrap_chance=0.01f;
    private static float netherrack_extra_chance=0.2f;
    private static float basalt_amethyst_chance=0.1f;

    public Hammer(ToolMaterial material, int attackDamage, float attackSpeed, Item.Settings settings) {
        super((float) attackDamage, attackSpeed, material, EFFECTIVE_BLOCKS, settings);

        sand_redsand_ratio=SkyutilsMod.config.hammer_sand_redsand_ratio/100.0f;
        iron_nugget_chance=SkyutilsMod.config.hammer_iron_nugget_chance/100.0f;
        redstone_from_redsand_chance=SkyutilsMod.config.hammer_redstone_from_redsand_chance/100.0f;
        redstone_from_sand_chance=SkyutilsMod.config.hammer_redstone_from_sand_chance/100.0f;
        podzol_extra_chance=SkyutilsMod.config.hammer_podzol_extra_chance/100.0f;
        grass_extra_chance=SkyutilsMod.config.hammer_grass_extra_chance/100.0f;
        sand_extra_chance=SkyutilsMod.config.hammer_sand_extra_chance/100.0f;
        diamond_nugget_chance=SkyutilsMod.config.hammer_diamond_nugget_chance/100.0f;
        nether_wart_chance=SkyutilsMod.config.hammer_nether_wart_chance/100.0f;
        netherite_scrap_chance=SkyutilsMod.config.hammer_netherite_scarp_chance/100.0f;
        netherrack_extra_chance=SkyutilsMod.config.hammer_netherrack_extra_chance/100.0f;
        basalt_amethyst_chance=SkyutilsMod.config.basalt_amethyst_chance/100.0f;
    }

    @Override
    public boolean isSuitableFor(BlockState state) {
        if ( state.isIn(EFFECTIVE_BLOCKS)) {
            return true;
        }
        return false;
    }

    public static List<ItemStack> remap_drop(World world, PlayerEntity player, BlockPos pos, BlockState state) {

        Block block=state.getBlock();
        //Identifier identifier = Registry.BLOCK.getId(state.getBlock());
        //String path = identifier.getPath();
        // System.out.println("path " + path);
        ItemStack stack = null;
        ItemStack stack2 = null;
        float chance = 1.0f;
        if (!player.getInventory().main.isEmpty()) {
            ItemStack tool = player.getInventory().getMainHandStack();
            int i = EnchantmentHelper.getLevel(Enchantments.FORTUNE, tool);
            // System.out.println(tool+" FORTUNE " + i);
            chance += i;
        }

        // System.out.println("chance " + chance);
        if ( block== Blocks.COBBLESTONE  || block==Blocks.STONE /*path.equals("cobblestone") || path.equals("stone")*/) {
            stack = new ItemStack(Items.GRAVEL, (int) chance);
        } else if (block== Blocks.GRAVEL) {
            if (world.random.nextFloat() < sand_redsand_ratio) {
                stack = new ItemStack(Items.SAND, (int) chance);
            }else{
                stack = new ItemStack(Items.RED_SAND, (int) chance);
            }
            if (world.random.nextFloat() < iron_nugget_chance * chance) {
                stack2 = new ItemStack(Items.IRON_NUGGET, (int) chance);
            }
        }else if (block== Blocks.RED_SAND) {
            if (world.random.nextFloat() < redstone_from_redsand_chance * chance) {
                stack = new ItemStack(Items.REDSTONE, 1);
            }
        }else if (block== Blocks.SAND) {
            stack = new ItemStack(Items.CLAY_BALL, (int) chance + 4);
            if (world.random.nextFloat() < redstone_from_sand_chance * chance) {
                stack2 = new ItemStack(Items.REDSTONE, 1);
            }
            if (world.random.nextFloat() < sand_extra_chance * chance) {                
                if (world.random.nextFloat() < 0.5) {
                    stack2 = new ItemStack(Items.CACTUS, 1);
                } else {
                    stack2 = new ItemStack(Items.KELP, 1);
                }
            }
        } else if (state.isIn(BlockTags.LOGS)) {
            stack = new ItemStack(SkyutilsMod.WOODCHIPS, (int) chance);
        } else if (block== Blocks.DIRT) {
            stack = new ItemStack(SkyutilsMod.PEBBLE, (int) chance + 3);
            if (world.random.nextFloat() < dirt_extra_chance) {
                switch((int)(world.random.nextFloat()*4)){
                    case 0: stack2 = new ItemStack(SkyutilsMod.ANDESITE_PEBBLE, (int) chance); break;
                    case 1: stack2 = new ItemStack(SkyutilsMod.DIORITE_PEBBLE, (int) chance);   break;
                    case 2: stack2 = new ItemStack(SkyutilsMod.GRANITE_PEBBLE, (int) chance);   break;
                    case 3: stack2 = new ItemStack(SkyutilsMod.CALCITE_FRAGMENT, (int) chance);   break;
                }
            }
        } else if (block== Blocks.PODZOL) {
            stack = new ItemStack(SkyutilsMod.PEBBLE, (int) chance + 3);
            if (world.random.nextFloat() < podzol_extra_chance) {                
                switch((int)(world.random.nextFloat()*3)){
                    case 0: stack2 = new ItemStack(Items.BROWN_MUSHROOM, (int) chance); break;
                    case 1: stack2 = new ItemStack(Items.RED_MUSHROOM, (int) chance);   break;
                    case 2: stack2 = new ItemStack(Items.FERN, (int) chance);   break;
                }                
            }
        } else if (block== Blocks.GRASS_BLOCK) {
            stack = new ItemStack(SkyutilsMod.PEBBLE, (int) chance + 3);
            if (world.random.nextFloat() < grass_extra_chance * chance) {
                int r = (int) (world.random.nextFloat() * DIRT_DROPS.length);
                if (r >= 0 && r < DIRT_DROPS.length) {
                    stack2 = new ItemStack(DIRT_DROPS[r], 1);
                }else{
                    stack2 = new ItemStack(DIRT_DROPS[0], 1);
                }
            }
        } else if (block== SkyutilsMod.CHARCOAL_BLOCK || block== Blocks.COAL_BLOCK) {
            if (block== SkyutilsMod.CHARCOAL_BLOCK)
                stack = new ItemStack(Items.CHARCOAL, 8);
            if (block== Blocks.COAL_BLOCK)
                stack = new ItemStack(Items.COAL, 8);

            if (world.random.nextFloat() < diamond_nugget_chance * chance) {
                stack2 = new ItemStack(SkyutilsMod.DIAMOND_NUGGET);
            }            
        } else if (block== Blocks.QUARTZ_BLOCK) {
            stack = new ItemStack(Items.QUARTZ, 4);
        } else if (block== Blocks.NETHERRACK) {
            if (world.random.nextFloat() < nether_wart_chance * chance) {
                stack = new ItemStack(Items.NETHER_WART, 1);
            }
            if (world.random.nextFloat() < netherite_scrap_chance * chance) {
                stack2 = new ItemStack(Items.NETHERITE_SCRAP, 1);
            } else {
                if (world.random.nextFloat() < netherrack_extra_chance * chance) {
                    if (world.random.nextFloat() < 0.5f) {
                        stack2 = new ItemStack(Items.CRIMSON_ROOTS, 1);
                    } else {
                        stack2 = new ItemStack(Items.WARPED_ROOTS, 1);
                    }
                }
            }
        } else if (block== Blocks.BASALT) {
            if (world.random.nextFloat() < basalt_amethyst_chance * chance) {
                stack = new ItemStack(Items.AMETHYST_SHARD, 1);
            }
        }

        if (stack != null) {
            ObjectArrayList<ItemStack> list=new ObjectArrayList<>();
            list.add(stack);
            if (stack2 != null) {
                list.add(stack2);
            }
            /*player.incrementStat(Stats.MINED.getOrCreateStat(state.getBlock()));
            player.addExhaustion(0.005F);
            Block.dropStack(world, pos, stack);
            //Block.dropStacks(Block.getBlockFromItem(stack.getItem()).getDefaultState(),world, pos,null,player,player.getMainHandStack() );
            if (stack2 != null) {
                //Block.dropStacks(Block.getBlockFromItem(stack2.getItem()).getDefaultState(),world, pos,null,player,player.getMainHandStack() );
                Block.dropStack(world, pos, stack2);
            }*/
            return list;
        }
        //return true;
        return Collections.emptyList();
    }

    static {
        EFFECTIVE_BLOCKS = BlocksTags.HAMMER_MINABLE;
    }
}