package net.nicguzzo;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class CondenserEntity extends BlockEntity{

    // Store the current value of the number
    private int time = 0;
    // private int time_limit = 0;
    private int level = 0;
    private static BlockPos.Mutable bp=new BlockPos.Mutable();
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
    public void writeNbt(NbtCompound tag) {
        // Save the current value of the number to the tag
        tag.putInt("number", time);
        tag.putInt("level", level);
        super.writeNbt(tag);
    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);
        //System.out.println("fromTag");
        time = tag.getInt("number");
        level = tag.getInt("level");
    }
        public static void tick(World world, BlockPos pos, BlockState state, CondenserEntity blockEntity) {
        if (!world.isClient) {
            // System.out.println("tick");
            int time_limit = 2400;
            Biome biome = world.getBiome(pos).value();
            float temperature = biome.getTemperature();
            boolean raining = world.isRaining();

            if (temperature >= 0.95f) {
                time_limit = time_limit * 2;
            }

            if (biome.getPrecipitation() == Biome.Precipitation.RAIN && raining) {
                //check sky access
                int l = world.getTopY(Heightmap.Type.WORLD_SURFACE, pos.getX(), pos.getZ())-1;
                bp.set(pos.getX(),l, pos.getZ());
                BlockState skystate = world.getBlockState(bp);
                if(skystate.getBlock() instanceof CondenserBlock) {
                    time_limit = (int) (time_limit * 0.05);
                }else{
                    time_limit = (int) (time_limit * 0.6);
                }
                //System.out.println("time_limit " + time_limit);
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