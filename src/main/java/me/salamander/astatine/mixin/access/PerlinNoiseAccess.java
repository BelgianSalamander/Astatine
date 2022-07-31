package me.salamander.astatine.mixin.access;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import org.checkerframework.checker.units.qual.A;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PerlinNoise.class)
public interface PerlinNoiseAccess {
    @Accessor
    static int getROUND_OFF() {
        throw new Error("Mixin did not apply!");
    }

    @Accessor
    ImprovedNoise[] getNoiseLevels();

    @Accessor
    int getFirstOctave();

    @Accessor
    DoubleList getAmplitudes();

    @Accessor
    double getLowestFreqValueFactor();

    @Accessor
    double getLowestFreqInputFactor();

    @Accessor
    double getMaxValue();
}
