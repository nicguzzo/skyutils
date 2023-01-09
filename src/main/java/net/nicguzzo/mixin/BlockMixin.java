package net.nicguzzo.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.nicguzzo.Hammer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Block.class)
public abstract class BlockMixin {

	/*@Inject(at = @At("HEAD"), method = "afterBreak",cancellable = true)
	public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack stack,CallbackInfo info) {
		if(stack.getItem() instanceof  Hammer){			
			if(Hammer.remap_drop(world,player,pos,state)){
				info.cancel();
			}
		}
	}*/

	@Inject(at = @At("HEAD"), method = "getDroppedStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Ljava/util/List;",cancellable = true)
	private static void getDroppedStacks(BlockState state, ServerWorld world, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity entity, ItemStack stack, CallbackInfoReturnable<List<ItemStack>> info){
		if(stack.getItem() instanceof  Hammer){
		   List<ItemStack> list=Hammer.remap_drop(world,(PlayerEntity)entity,pos,state);
		   info.setReturnValue(list);
		}
	}
}
