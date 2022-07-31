package me.salamander.astatine.mixin.access;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SurfaceSystem.class)
public interface SurfaceSystemAccess {
    @Invoker
    NormalNoise invokeGetOrCreateNoise(ResourceKey<NormalNoise.NoiseParameters> noiseKey);

    @Accessor
    NormalNoise getClayBandsOffsetNoise();

    @Accessor
    BlockState[] getClayBands();

    @Accessor
    NormalNoise getSurfaceNoise();

    @Accessor
    NormalNoise getSurfaceSecondaryNoise();
}
