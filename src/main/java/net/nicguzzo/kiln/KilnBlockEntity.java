package net.nicguzzo.kiln;

import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.container.Container;
import net.minecraft.container.PropertyDelegate;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.Tickable;
import net.nicguzzo.SkyutilsMod;

public class KilnBlockEntity extends LootableContainerBlockEntity implements BlockEntityClientSerializable, Tickable {
    private DefaultedList<ItemStack> inventory;
    private int burn_time = 0;
    private int cook_time = 0;
    private int progress = 0;
    private static final int INVENTORY_SIZE = 4;
    public static final int CHARCOAL_BURN_TIME =  1000;
    public static final int COBBLESTONE_COOK_TIME = 8000;
    public static final int COBBLESTONE_COST = 8;
    public static final int RAW_CRUCIBLE_COOK_TIME = 2000;
    protected final PropertyDelegate propertyDelegate;
    

    public KilnBlockEntity() {
        super(SkyutilsMod.KILN_ENTITY_TYPE);
        this.inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
        this.propertyDelegate = new PropertyDelegate() {
            public int get(int key) {
                switch (key) {
                    case 0:
                        return KilnBlockEntity.this.burn_time;
                    case 1:
                        return KilnBlockEntity.this.cook_time;
                    case 2:
                        return KilnBlockEntity.this.progress;
                    default:
                        return 0;
                }
            }

            public void set(int key, int value) {
                switch (key) {
                    case 0:
                        KilnBlockEntity.this.burn_time = value;
                        break;
                    case 1:
                        KilnBlockEntity.this.cook_time = value;
                        break;
                    case 2:
                        KilnBlockEntity.this.progress = value;
                        break;
                }
            }

            public int size() {
                return 1;
            }
        };
    }

    @Override
    protected Text getContainerName() {
        return new TranslatableText("container.kiln");
    }

    @Override
    public Container createContainer(int syncId, PlayerInventory playerInventory) {
        return new KilnContainer(syncId, playerInventory, (Inventory) this, this.propertyDelegate);
    }

    @Override
    protected DefaultedList<ItemStack> getInvStackList() {
        return this.inventory;
    }

    @Override
    protected void setInvStackList(DefaultedList<ItemStack> list) {
        this.inventory = list;
    }

    @Override
    public int getInvSize() {
        return INVENTORY_SIZE;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        super.fromTag(tag);
        this.inventory = DefaultedList.ofSize(this.getInvSize(), ItemStack.EMPTY);
        Inventories.fromTag(tag, this.inventory);
        this.burn_time = tag.getInt("burn_time");
        this.cook_time = tag.getInt("cook_time");
        this.progress = tag.getInt("progress");
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        super.toTag(tag);
        tag.putInt("burn_time", this.burn_time);
        tag.putInt("cook_time", this.cook_time);
        tag.putInt("progress", this.progress);
        Inventories.toTag(tag, this.inventory);
        return tag;
    }

    @Override
    public void tick() {
        if (this.burn_time > 0) {
            this.burn_time--;
        }
        if (!this.world.isClient) {
            ItemStack item = (ItemStack) this.inventory.get(0);
            ItemStack crucible = (ItemStack) this.inventory.get(1);
            ItemStack fuel = (ItemStack) this.inventory.get(2);
            ItemStack out = (ItemStack) this.inventory.get(3);

            if (out.isEmpty()) {
                if (!crucible.isEmpty()) {
                    if (!item.isEmpty() && item.getItem() == Items.COBBLESTONE && item.getCount() == COBBLESTONE_COST) {
                         cook(item, fuel, crucible, COBBLESTONE_COOK_TIME, COBBLESTONE_COST,
                                (Item) SkyutilsMod.LAVA_CRUCIBLE);
                    } else {
                        this.cook_time = 0;
                        this.progress = 0;
                    }
                } else {
                    if (!item.isEmpty() && item.getItem() == SkyutilsMod.RAW_CRUCIBLE) {
                         cook(item, fuel, crucible, RAW_CRUCIBLE_COOK_TIME, 1, SkyutilsMod.CRUCIBLE);
                    } else {
                        this.cook_time = 0;
                        this.progress = 0;
                    }
                }
            }
            sync();
        }

        this.markDirty();
    }

    @Override
    public void fromClientTag(CompoundTag tag) {
        this.fromTag(tag);
    }

    @Override
    public CompoundTag toClientTag(CompoundTag tag) {
        return this.toTag(tag);
    }

    private boolean cook(ItemStack item, ItemStack fuel, ItemStack crucible, int total_cook_time, int dec, Item out) {
        if (burn_time == 0) {
            if(!fuel.isEmpty()){
                this.burn_time = CHARCOAL_BURN_TIME;                
                fuel.decrement(1);
            }
        }
        if (burn_time > 0) {

            if (this.cook_time == 0) {
                this.cook_time = total_cook_time;
            }
            if (this.cook_time > 0) {
                this.cook_time--;
                this.progress = (int) ((1.0f - (this.cook_time / (float) total_cook_time)) * 1000.0f);
                if (this.cook_time == 0) {
                    this.inventory.set(3, new ItemStack(out));
                    item.decrement(dec);
                    if (crucible != null)
                        crucible.decrement(1);
                    this.cook_time = 0;
                    this.progress = 0;
                    return false;
                }
            }
        }
        
        return this.cook_time > 0;
    }
}
