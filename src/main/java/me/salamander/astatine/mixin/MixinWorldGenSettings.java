package me.salamander.astatine.mixin;

import me.salamander.astatine.Astatine;
import me.salamander.astatine.compiler.CompileContext;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldGenSettings.class)
public class MixinWorldGenSettings {
    @Inject(method = "create", at = @At("HEAD"), cancellable = true)
    private static void makeAstatine(RegistryAccess registryAccess, DedicatedServerProperties.WorldGenProperties properties, CallbackInfoReturnable<WorldGenSettings> cir) {
        System.out.println("Making world gen settings");
    }
}
