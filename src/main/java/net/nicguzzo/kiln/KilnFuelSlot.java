package net.nicguzzo.kiln;

import net.minecraft.screen.slot.Slot;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.nicguzzo.SkyutilsMod;

public class KilnFuelSlot extends Slot {
   

   public KilnFuelSlot(Inventory inventory, int invSlot, int xPosition, int yPosition) {
      super(inventory, invSlot, xPosition, yPosition);      
   }

   public boolean canInsert(ItemStack stack) {       
      return (stack.getItem() == SkyutilsMod.CHARCOAL_BLOCK_ITEM.asItem() || stack.getItem() == Items.CHARCOAL.asItem());
   }
}
