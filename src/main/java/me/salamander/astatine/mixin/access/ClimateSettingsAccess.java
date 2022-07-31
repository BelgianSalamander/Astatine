package me.salamander.astatine.mixin.access;

import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Biome.ClimateSettings.class)
public interface ClimateSettingsAccess {
    @Accessor
    Biome.TemperatureModifier getTemperatureModifier();
}
