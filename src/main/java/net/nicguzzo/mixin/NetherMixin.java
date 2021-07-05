package net.nicguzzo.mixin;

//import net.minecraft.world.biome.Biome;
//import net.minecraft.world.biome.source.BiomeSource;
//import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
//import net.minecraft.util.registry.Registry;
//import net.minecraft.world.gen.chunk.ChunkGenerator;
//import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
//import net.minecraft.world.level.LevelProperties;
//import net.nicguzzo.SkyblockChunkGenerator;
//import net.nicguzzo.SkyblockNetherChunkGenerator;
import net.nicguzzo.SkyutilsConfig;
import net.nicguzzo.SkyutilsMod;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


// @Mixin(DimensionType.class)
// public abstract class NetherMixin {
//     @Inject(method = "createNetherGenerator", at = @At("HEAD"), cancellable = true)
//     private static void createNetherGenerator(Registry<Biome> biomeRegistry, Registry<ChunkGeneratorSettings> chunkGeneratorSettingsRegistry, long seed,
//     CallbackInfoReturnable<ChunkGenerator> cir) {
//         //ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
//         System.out.println(" NetherMixin createNetherGenerator is skyblock? "+SkyutilsMod.is_skyblock);
//         {
//             BiomeSource bs=MultiNoiseBiomeSource.Preset.NETHER.getBiomeSource(biomeRegistry, seed);
//             SkyblockNetherChunkGenerator chunk_generator= new SkyblockNetherChunkGenerator(bs, seed, () -> {
//                 return (ChunkGeneratorSettings)chunkGeneratorSettingsRegistry.getOrThrow(ChunkGeneratorSettings.NETHER);
//                 });
//             cir.setReturnValue((ChunkGenerator)chunk_generator);
//         }   
//     }
// }
@Mixin(NoiseChunkGenerator.class)
public class NetherMixin{
    static private boolean skb=false;
    static private boolean nth=false;
    @Inject(method = "populateNoise(Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/chunk/Chunk;II)Lnet/minecraft/world/chunk/Chunk;", at = @At("HEAD"), cancellable = true)
    public void populateNoise(StructureAccessor accessor, Chunk chunk, int startY, int noiseSizeY,CallbackInfoReturnable<Chunk> cir) {
        
        //System.out.println("populateNoise SkyutilsMod.is_skyblock: "+SkyutilsMod.is_skyblock);
        //System.out.println("nether hascode: " + accessor.world.getDimension().getSkyProperties());
        /*if(accessor.world.getLevelProperties() instanceof LevelProperties){
            LevelProperties p=(LevelProperties)accessor.world.getLevelProperties();
            if(p.getGeneratorOptions().getChunkGenerator() instanceof SkyblockChunkGenerator){
                System.out.println("yes!!");
                SkyutilsMod.is_skyblock=true;
            }else{
                System.out.println("no!");
            }
        }else{
            System.out.println("nope :(");
        }*/
        skb=SkyutilsMod.is_skyblock;
        nth=false;
        if(SkyutilsMod.is_skyblock  && accessor.world.getDimension().getSkyProperties()==DimensionType.THE_NETHER_ID){
            nth=true;
            cir.setReturnValue(chunk);
        }        
    }
    @Inject(method = "buildBedrock", at = @At("HEAD"), cancellable = true)
    public void buildBedrock(Chunk chunk, Random random,CallbackInfo ci){
        SkyutilsConfig config=SkyutilsConfig.get_instance();
        if(skb && nth && !config.nether_bedrock)
            ci.cancel();
    }
}