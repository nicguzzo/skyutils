package net.nicguzzo.mixin;

import net.minecraft.inventory.SidedInventory;
import net.nicguzzo.composter.Composter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BlockState;
import net.minecraft.block.ComposterBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

@Mixin(ComposterBlock.class)
public abstract class ComposterMixin {

  /*private static boolean addToComposter(int level, BlockState state, WorldAccess world, BlockPos pos, ItemStack item) {

    System.out.println("composter level: " + level);
    int j = level + 1;
    world.setBlockState(pos, (BlockState) state.with(ComposterBlock.LEVEL, j), 3);
    if (j == 7) {
      world.getBlockTickScheduler().schedule(pos, state.getBlock(), 20);
    }
    return true;

  }*/

  @Shadow
  native private static BlockState emptyComposter(BlockState state, WorldAccess world, BlockPos pos);

  @Inject(at = @At("HEAD"), method = "onUse", cancellable = true)

  public void onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit,
      CallbackInfoReturnable<ActionResult> info) {
    int i = (Integer) state.get(ComposterBlock.LEVEL);
    System.out.println("composter on use level: " + i);
    ItemStack itemStack = player.getStackInHand(hand);
    if (i < 8 && ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.containsKey(itemStack.getItem())) {
      if (i < 7 && !world.isClient) {
        boolean bl = Composter.addToComposter(i, state, world, pos, itemStack);
        world.syncWorldEvent(1500, pos, bl ? 1 : 0);
        if (!player.getAbilities().creativeMode) {
          itemStack.decrement(1);
        }
      }

      info.setReturnValue(ActionResult.SUCCESS);
    } else if (i == 8) {
      if (!world.isClient) {

        double d = (double) (world.random.nextFloat() * 0.7F) + 0.15000000596046448D;
        double e = (double) (world.random.nextFloat() * 0.7F) + 0.06000000238418579D + 0.6D;
        double g = (double) (world.random.nextFloat() * 0.7F) + 0.15000000596046448D;
        ItemEntity itemEntity = new ItemEntity(world, (double) pos.getX() + d, (double) pos.getY() + e,
            (double) pos.getZ() + g, new ItemStack(Items.GRASS_BLOCK));
        itemEntity.setToDefaultPickupDelay();
        world.spawnEntity(itemEntity);
      }

      emptyComposter(state, world, pos);
      world.playSound((PlayerEntity) null, pos, SoundEvents.BLOCK_COMPOSTER_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);

      info.setReturnValue(ActionResult.SUCCESS);
    } else {

      info.setReturnValue(ActionResult.PASS);
    }
    info.cancel();
    return;
  }

  @Inject(at = @At("HEAD"), method = "getInventory", cancellable = true)
  public void getInventory(BlockState state, WorldAccess world, BlockPos pos, CallbackInfoReturnable<SidedInventory> info) {
    int i = (Integer)state.get(ComposterBlock.LEVEL);
    SidedInventory ret;
    if (i == 8) {
      ret = new Composter.FullComposterInventory(state, world, pos, new ItemStack(Items.GRASS_BLOCK));
    } else {
      ret = (i < 7 ? new Composter.ComposterInventory(state, world, pos) : new Composter.DummyInventory());
    }
    info.setReturnValue(ret);
    return;
  }


}
