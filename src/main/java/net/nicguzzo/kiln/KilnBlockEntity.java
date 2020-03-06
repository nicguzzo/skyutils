package net.nicguzzo.kiln;

import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.container.Container;
import net.minecraft.container.PropertyDelegate;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.Tickable;
import net.nicguzzo.SkyutilsMod;

public class KilnBlockEntity extends LootableContainerBlockEntity implements Tickable {
    private DefaultedList<ItemStack> inventory;
    private int time=0;
    private static final int INVENTORY_SIZE = 4;
    public static final int BURN_TIME = 6000;
    protected final PropertyDelegate propertyDelegate;
    private boolean done=false;      
    public KilnBlockEntity() {
        super(SkyutilsMod.KILN_ENTITY_TYPE);
        this.inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
        this.propertyDelegate = new PropertyDelegate() {
            public int get(int key) {
               switch(key) {
               case 0:
                  return KilnBlockEntity.this.time;
               default:
                  return 0;
               }
            }   
            public void set(int key, int value) {
               switch(key) {
               case 0:
                    KilnBlockEntity.this.time = value;
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
        return new KilnContainer(syncId, playerInventory, (Inventory) this,this.propertyDelegate);
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
        if (!this.deserializeLootTable(tag)) {
            Inventories.fromTag(tag, this.inventory);
        }
    }
 
    @Override
    public CompoundTag toTag(CompoundTag tag) {
        super.toTag(tag);
        if (!this.serializeLootTable(tag)) {
            Inventories.toTag(tag, this.inventory);
        }
        return tag;
    }

    @Override
    public void tick() {  
        //System.out.println("time= "+time);
        if(this.time > 0){
            this.time--;            
            if(this.time==0){
                this.done=true;
            }
        }
        {
            ItemStack item     = (ItemStack)this.inventory.get(0);
            ItemStack crucible = (ItemStack)this.inventory.get(1);
            ItemStack fuel     = (ItemStack)this.inventory.get(2);
            ItemStack out      = (ItemStack)this.inventory.get(3);
            if(!fuel.isEmpty()&& out.isEmpty()){
                if(!crucible.isEmpty()){
                    if(!item.isEmpty() && item.getItem()==Items.COBBLESTONE && item.getCount()==64){
                        if(done){
                            System.out.print("done!");                            
                            if (!this.world.isClient) {
                                this.inventory.set(3,new ItemStack(SkyutilsMod.LAVA_CRUCIBLE));     
                                item.decrement(64);                       
                                crucible.decrement(1);
                                fuel.decrement(1);
                            }
                            this.done=false;
                        }else{
                            if(this.time==0){
                                this.time=BURN_TIME;
                            }
                        }                        
                    }else{
                        this.time=0;
                        this.done=false;
                    }
                }else{
                    if(!item.isEmpty() && item.getItem()==SkyutilsMod.RAW_CRUCIBLE){
                        if(done){
                            System.out.print("done!");                            
                            if (!this.world.isClient) {
                                this.inventory.set(3,new ItemStack(SkyutilsMod.CRUCIBLE));     
                                item.decrement(1);
                                fuel.decrement(1);
                            }
                            this.done=false;
                        }else{
                            if(this.time==0){
                                this.time=BURN_TIME;
                            }
                        } 
                    }else{
                        this.time=0;
                        this.done=false;
                    }
                }
            }else{
                this.time=0;
                this.done=false;
            }
        }
        
    }
}
