package net.nicguzzo.kiln;

import net.minecraft.screen.slot.Slot;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.nicguzzo.SkyutilsMod;

public class KilnCrucibleSlot extends Slot {
   

   public KilnCrucibleSlot(Inventory inventory, int invSlot, int xPosition, int yPosition) {
      super(inventory, invSlot, xPosition, yPosition);      
   }

   public boolean canInsert(ItemStack stack) {       
      return stack.getItem() == SkyutilsMod.CRUCIBLE;
   }
}
