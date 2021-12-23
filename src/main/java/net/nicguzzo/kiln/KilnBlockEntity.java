package net.nicguzzo.kiln;

import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.nicguzzo.SkyutilsMod;

public class KilnBlockEntity extends LootableContainerBlockEntity implements BlockEntityClientSerializable {
    private DefaultedList<ItemStack> inventory;
    private int burn_time = 0;    
    private int cook_time = 0;
    private int fuel_time = 0;
    private int progress = 0;
    private static final int INVENTORY_SIZE = 4;
    public static final int CHARCOAL_BURN_TIME =  SkyutilsMod.config.kiln_charcoal_burn_time;
    public static final int CHARCOAL_BLOCK_BURN_TIME = SkyutilsMod.config.kiln_charcoal_block_burn_time;
    public static final int COBBLESTONE_COOK_TIME = SkyutilsMod.config.kiln_cobblestone_cook_time;
    public static final int COBBLESTONE_COST = SkyutilsMod.config.kiln_lava_cost;
    public static final int RAW_CRUCIBLE_COOK_TIME = SkyutilsMod.config.kiln_crucible_cook_time;
    
    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int key) {
            switch (key) {
                case 0:
                    return KilnBlockEntity.this.burn_time;
                case 1:
                    return KilnBlockEntity.this.cook_time;
                case 2:
                    return KilnBlockEntity.this.progress;
                case 3:
                    return KilnBlockEntity.this.fuel_time;
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
                case 3:
                    KilnBlockEntity.this.fuel_time = value;
                    break;
            }
        }

        public int size() {
            return 4;
        }
    };

    public KilnBlockEntity(BlockPos pos, BlockState state) {
        super(SkyutilsMod.KILN_ENTITY_TYPE, pos, state);
        this.inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
        // this.propertyDelegate
    };

    /*
     * @Override public Text getDisplayName() { return new
     * TranslatableText(getCachedState().getBlock().getTranslationKey()); }
     */
    @Override
    protected Text getContainerName() {
        return new TranslatableText("container.kiln");
    }

    @Override
    public ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new KilnScreenHandler(syncId, playerInventory, (Inventory) this, this.propertyDelegate);
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
    public int size() {
        return INVENTORY_SIZE;
    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);
        this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        Inventories.readNbt(tag, this.inventory);
        this.fuel_time = tag.getInt("fuel_time");
        this.burn_time = tag.getInt("burn_time");
        this.cook_time = tag.getInt("cook_time");
        this.progress = tag.getInt("progress");
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        super.writeNbt(tag);
        tag.putInt("fuel_time", this.fuel_time);
        tag.putInt("burn_time", this.burn_time);
        tag.putInt("cook_time", this.cook_time);
        tag.putInt("progress", this.progress);
        Inventories.writeNbt(tag, this.inventory);
        return tag;
    }

    public static void tick(World world, BlockPos pos, BlockState state, KilnBlockEntity blockEntity) {
        blockEntity.tick();
    }

    private void tick() {
        if (this.burn_time > 0) {
            this.burn_time--;
        }
        if (!this.world.isClient) {
            ItemStack item = (ItemStack) this.inventory.get(0);
            ItemStack crucible = (ItemStack) this.inventory.get(1);
            ItemStack fuel = (ItemStack) this.inventory.get(2);
            ItemStack out = (ItemStack) this.inventory.get(3);

            
            if (!crucible.isEmpty()) {
                if (out.isEmpty()) 
                {
                    if (!item.isEmpty() && item.getItem() == Items.COBBLESTONE && item.getCount() >= COBBLESTONE_COST) {
                        cook(item, fuel, crucible, COBBLESTONE_COOK_TIME, COBBLESTONE_COST,
                                (Item) SkyutilsMod.LAVA_CRUCIBLE);
                    } else {
                        this.cook_time = 0;
                        this.progress = 0;
                    }
                }
            } else {
                if (!item.isEmpty() && item.getItem() == SkyutilsMod.RAW_CRUCIBLE) {
                    cook(item, fuel, crucible, RAW_CRUCIBLE_COOK_TIME, 1, SkyutilsMod.CRUCIBLE);
                } else {
                    this.cook_time = 0;
                    this.progress = 0;
                }
            }
            
            sync();
        }

        this.markDirty();
    }

    @Override
    public void fromClientTag(NbtCompound tag) {
        this.readNbt(tag);
    }

    @Override
    public NbtCompound toClientTag(NbtCompound tag) {
        return this.writeNbt(tag);
    }

    private boolean cook(ItemStack item, ItemStack fuel, ItemStack crucible, int total_cook_time, int dec, Item out) {
        if (burn_time == 0) {
            if (!fuel.isEmpty()) {
                if(fuel.getItem()==Items.CHARCOAL){
                    this.burn_time = CHARCOAL_BURN_TIME;
                    this.fuel_time= CHARCOAL_BURN_TIME;
                }
                if(fuel.getItem()==SkyutilsMod.CHARCOAL_BLOCK_ITEM){
                    this.burn_time = CHARCOAL_BLOCK_BURN_TIME;
                    this.fuel_time = CHARCOAL_BLOCK_BURN_TIME;
                }
                if (burn_time > 0){                    
                    fuel.decrement(1);
                }
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
                    if(item.getItem() == SkyutilsMod.RAW_CRUCIBLE){
                        int c=this.inventory.get(3).getCount();                        
                        this.inventory.set(3, new ItemStack(out,c+1));
                    }else{
                        this.inventory.set(3, new ItemStack(out));
                    }
                    
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
