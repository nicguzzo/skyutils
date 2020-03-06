package net.nicguzzo.mixin;

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

@Mixin(Block.class)
public abstract class BlockMixin {

	@Inject(at = @At("HEAD"), method = "afterBreak",cancellable = true)
	public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack stack,CallbackInfo info) {
		if(stack.getItem() instanceof  Hammer){			
			if(Hammer.remap_drop(world,player,pos,state)){
				info.cancel();
			}
		}
	}

}
