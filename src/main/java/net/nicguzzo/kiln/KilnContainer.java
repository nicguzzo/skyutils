package net.nicguzzo.kiln;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.container.Container;
import net.minecraft.container.FurnaceOutputSlot;
import net.minecraft.container.PropertyDelegate;
import net.minecraft.container.Slot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

public class KilnContainer extends Container {
    private final Inventory inventory; 
    private static final int INVENTORY_SIZE = 4; 
    private final PropertyDelegate propertyDelegate;
 
    protected KilnContainer(int syncId, PlayerInventory playerInventory, Inventory inventory,PropertyDelegate pd) {
        super(null, syncId); // Since we didn't create a ContainerType, we will place null here.
        this.inventory = inventory;
        checkContainerSize(inventory, INVENTORY_SIZE);
        checkContainerDataCount(pd, 1);
        this.propertyDelegate = pd;
        inventory.onInvOpen(playerInventory.player);
        
        this.addSlot(new KilnInputSlot(inventory, 0, 56, 17));//input
        this.addSlot(new KilnCrucibleSlot(inventory, 1, 23, 17));//crucible
        this.addSlot(new KilnFuelSlot(inventory, 2, 56, 53));
        this.addSlot(new FurnaceOutputSlot(playerInventory.player, inventory, 3, 116, 35));
         
        int k;
        for(k = 0; k < 3; ++k) {
            for(int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + k * 9 + 9, 8 + j * 18, 84 + k * 18));
            }
        }
        for(k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
    }
 
    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUseInv(player);
    }
    @Environment(EnvType.CLIENT)
    public int getProgress() {
        return this.propertyDelegate.get(0);
        //return (int)(100-(this.propertyDelegate.get(0)/(float)KilnBlockEntity.BURN_TIME)*100);
    }
    
 
    // Shift + Player Inv Slot
    public ItemStack transferSlot(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slotList.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < this.inventory.getInvSize()) {
                if (!this.insertItem(originalStack, this.inventory.getInvSize(), this.slotList.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, 0, this.inventory.getInvSize(), false)) {
                return ItemStack.EMPTY;
            }
 
            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
 
        return newStack;
    }
}