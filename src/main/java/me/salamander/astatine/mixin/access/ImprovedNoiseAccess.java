package me.salamander.astatine.mixin.access;

import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ImprovedNoise.class)
public interface ImprovedNoiseAccess {
    @Accessor("p")
    byte[] getP();

    @Accessor
    double getXo();

    @Accessor
    double getYo();

    @Accessor
    double getZo();
}
