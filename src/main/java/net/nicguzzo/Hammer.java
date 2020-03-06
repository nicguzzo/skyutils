package net.nicguzzo;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class  Hammer extends MiningToolItem
{
    private static final Set<Block> EFFECTIVE_BLOCKS;
    public Hammer(ToolMaterial material,     int attackDamage,     float attackSpeed, Item.Settings settings)
    {   
        super((float)attackDamage, attackSpeed, material, EFFECTIVE_BLOCKS, settings);
    }
    @Override
    public boolean isEffectiveOn(BlockState state) {
        Block block = state.getBlock();        
        if (block == Blocks.COBBLESTONE  || block == Blocks.SAND || block ==  Blocks.GRAVEL ||
            block == Blocks.ACACIA_LOG ||
            block == Blocks.OAK_LOG ||
            block == Blocks.SPRUCE_LOG||
            block == Blocks.DARK_OAK_LOG||
            block == Blocks.JUNGLE_LOG
        ) {
            return true;
        }
        return false;
     }     

     public static boolean remap_drop(World world, PlayerEntity player, BlockPos pos, BlockState state){
        
        Identifier identifier = Registry.BLOCK.getId(state.getBlock());
        String path=identifier.getPath();
        //System.out.println("path  "+path);
        ItemStack stack=null;
        if(path.equals("cobblestone")){
           stack=new ItemStack(Items.GRAVEL);
        }else if(path.equals("gravel")){
            stack=new ItemStack(Items.SAND);			
        }else if(path.equals("sand")){
            stack=new ItemStack(Items.CLAY_BALL);
        }else if(path.contains("_log")){
            stack=new ItemStack(SkyutilsMod.WOODCHIPS);
        }
        if(stack!=null){
            player.incrementStat(Stats.MINED.getOrCreateStat(state.getBlock() ));
            player.addExhaustion(0.005F);
            Block.dropStack(world,pos, stack);
            return true;
        }
        
        return false;
    }
    static {
      EFFECTIVE_BLOCKS = ImmutableSet.of( Blocks.COBBLESTONE,  Blocks.SAND,  Blocks.GRAVEL ,
      Blocks.ACACIA_LOG ,
      Blocks.OAK_LOG ,
      Blocks.SPRUCE_LOG,
      Blocks.DARK_OAK_LOG,
      Blocks.JUNGLE_LOG);
   }
}