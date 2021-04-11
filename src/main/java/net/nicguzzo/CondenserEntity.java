package net.nicguzzo;

import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tickable;
import net.minecraft.world.biome.Biome;

public class CondenserEntity extends BlockEntity implements BlockEntityClientSerializable, Tickable {

    // Store the current value of the number
    private int time = 0;
    private int time_limit = 0;
    private int level = 0;

    public CondenserEntity() {
        super(SkyutilsMod.CONDENSER_ENTITY);

        /*
         * Biome biome=world.getBiome(this.getPos()); if ((biome == Biomes.BADLANDS||
         * biome == Biomes.BADLANDS_PLATEAU|| biome == Biomes.DESERT|| biome ==
         * Biomes.DESERT_HILLS|| biome == Biomes.DESERT_LAKES)) { time=time*2; }
         */
    }

    public int getLevel() {
        return level;
    }

    public void empty() {
        level = 0;
        time = 0;
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        super.toTag(tag);

        // Save the current value of the number to the tag
        tag.putInt("number", time);
        tag.putInt("level", level);

        return tag;
    }

    @Override
    public void fromTag(BlockState state, CompoundTag tag) {
        super.fromTag(this.getCachedState(), tag);
        System.out.println("fromTag");
        time = tag.getInt("number");
        level = tag.getInt("level");
    }

    @Override
    public void fromClientTag(CompoundTag tag) {
        this.fromTag(this.getCachedState(), tag);
    }

    @Override
    public CompoundTag toClientTag(CompoundTag tag) {
        return this.toTag(tag);
    }

    @Override
    public void tick() {
        if (!this.world.isClient) {
            time_limit = 2400;
            Biome biome = this.world.getBiome(this.getPos());
            float temperature = biome.getTemperature(pos);
            boolean raining = this.world.isRaining();
            if (temperature >= 0.95f) {
                time_limit = time_limit * 2;
            }
            if (biome.getPrecipitation() == Biome.Precipitation.RAIN && raining) {
                time_limit = (int) (time_limit * 0.1);
            }
            int t = time_limit;

            int d = t / 7;
            BlockState state = this.world.getBlockState(pos);

            CondenserBlock block = null;
            if (state.getBlock() instanceof CondenserBlock) {
                block = (CondenserBlock) state.getBlock();
                if (time < t) {
                    time++;
                    // System.out.println("condenser level " + level);
                    if (time % d == 0 && this.level < 7) {
                        level++;
                        block.incLevel(world, pos, state);
                        System.out.println("condenser level " + level);
                    }
                }
                markDirty();
            }
        }
    }
}