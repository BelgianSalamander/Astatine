package me.salamander.astatine.mixin.access;

import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NormalNoise.class)
public interface NormalNoiseAccess {
    @Accessor
    static double getINPUT_FACTOR() {
        throw new Error("Mixin did not apply");
    }

    @Accessor
    static double getTARGET_DEVIATION() {
        throw new Error("Mixin did not apply");
    }

    @Accessor
    double getValueFactor();

    @Accessor
    PerlinNoise getFirst();

    @Accessor
    PerlinNoise getSecond();

    @Accessor
    double getMaxValue();

    @Accessor
    NormalNoise.NoiseParameters getParameters();
}
