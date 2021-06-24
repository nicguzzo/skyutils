package net.nicguzzo;

import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class CondenserEntity extends BlockEntity implements BlockEntityClientSerializable {

    // Store the current value of the number
    private int time = 0;
    // private int time_limit = 0;
    private int level = 0;

    public CondenserEntity(BlockPos pos, BlockState state) {
        super(SkyutilsMod.CONDENSER_ENTITY, pos, state);

        /*
         * Biome biome=world.getBiome(this.getPos()); if ((biome == Biomes.BADLANDS||
         * biome == Biomes.BADLANDS_PLATEAU|| biome == Biomes.DESERT|| biome ==
         * Biomes.DESERT_HILLS|| biome == Biomes.DESERT_LAKES)) { time=time*2; }
         */
    }

    public int getLevel() {
        return level;
    }

    public int getTime() {
        return time;
    }

    public void setLevel(int l) {
        level = l;
    }

    public void incLevel() {
        if (level < 7)
            level++;
    }

    public void setTime(int t) {
        time = t;
    }

    public void incTime() {
        time++;
    }

    public void empty() {
        level = 0;
        time = 0;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        super.writeNbt(tag);

        // Save the current value of the number to the tag
        tag.putInt("number", time);
        tag.putInt("level", level);

        return tag;
    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);
        //System.out.println("fromTag");
        time = tag.getInt("number");
        level = tag.getInt("level");
    }

    @Override
    public void fromClientTag(NbtCompound tag) {
        this.readNbt(tag);
    }

    @Override
    public NbtCompound toClientTag(NbtCompound tag) {
        return this.writeNbt(tag);
    }

    public static void tick(World world, BlockPos pos, BlockState state, CondenserEntity blockEntity) {
        if (!world.isClient) {
            // System.out.println("tick");
            int time_limit = 2400;
            Biome biome = world.getBiome(pos);
            float temperature = biome.getTemperature(pos);
            boolean raining = world.isRaining();

            if (temperature >= 0.95f) {
                time_limit = time_limit * 2;
            }
            if (biome.getPrecipitation() == Biome.Precipitation.RAIN && raining) {
                time_limit = (int) (time_limit * 0.05);
                // System.out.println("time_limit " + time_limit);
            }

            int d = time_limit / 7;
            // BlockState state = this.world.getBlockState(pos);

            if (state.getBlock() instanceof CondenserBlock) {
                CondenserBlock block = (CondenserBlock) state.getBlock();
                // System.out.println("condenser time " + blockEntity.getTime());
                if (blockEntity.getTime() > time_limit) {
                    blockEntity.setTime(0);
                    if (blockEntity.getTime() % d == 0 && blockEntity.getLevel() < 7) {
                        blockEntity.incLevel();
                        block.incLevel(world, pos, state);
                        //System.out.println("condenser level " + blockEntity.getLevel());
                    }
                }
                blockEntity.incTime();
                blockEntity.markDirty();
                // markDirty();
            }
        }
    }
}