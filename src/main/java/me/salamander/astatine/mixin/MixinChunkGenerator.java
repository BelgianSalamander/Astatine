package me.salamander.astatine.mixin;

import me.salamander.astatine.AstatineChunkGenerator;
import net.minecraft.core.Registry;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGenerator.class)
public class MixinChunkGenerator {
    @Inject(method = "<clinit>", at = @At("HEAD"))
    private static void onInit(CallbackInfo ci) {
        Registry.register(Registry.CHUNK_GENERATOR, "astatine", AstatineChunkGenerator.CODEC);
    }
}
