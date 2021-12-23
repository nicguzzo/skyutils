package net.nicguzzo.kiln;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.FurnaceOutputSlot;
import net.minecraft.screen.slot.Slot;
import net.nicguzzo.SkyutilsMod;

public class KilnScreenHandler extends ScreenHandler {
    private final Inventory inventory; 
    private static final int INVENTORY_SIZE = 4; 
    PropertyDelegate propertyDelegate;

    public KilnScreenHandler(int syncId, PlayerInventory playerInventory) {
        
        this(syncId, playerInventory, new SimpleInventory(INVENTORY_SIZE),new ArrayPropertyDelegate(4));
    }
 
    protected KilnScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory,PropertyDelegate pd) {
        super(SkyutilsMod.KILN_SCREEN_HANDLER, syncId); // Since we didn't create a ContainerType, we will place null here.
        this.inventory = inventory;
        checkSize(inventory, INVENTORY_SIZE);
        //checkContainerDataCount(pd, 3);
        this.propertyDelegate = pd;
        inventory.onOpen(playerInventory.player);
        
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
        this.addProperties(propertyDelegate);
    }
 
    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }
    @Environment(EnvType.CLIENT)
    public int getBurnTime() {
        return this.propertyDelegate.get(0);
    }
    @Environment(EnvType.CLIENT)
    public int getCoocktime() {
        return this.propertyDelegate.get(1);        
    }
    @Environment(EnvType.CLIENT)
    public int getProgress() {
        return this.propertyDelegate.get(2);
    }    
    @Environment(EnvType.CLIENT)
    public int getFuelTime() {
        return this.propertyDelegate.get(3);
    }
    
 
    // Shift + Player Inv Slot
    public ItemStack transferSlot(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot < this.inventory.size()) {
                if (!this.insertItem(originalStack, this.inventory.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, 0, this.inventory.size(), false)) {
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