package me.salamander.astatine.mixin.access;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Predicate;

@Mixin(SurfaceRules.BiomeConditionSource.class)
public interface BiomeConditionSourceAccess {
    @Accessor
    Predicate<ResourceKey<Biome>> getBiomeNameTest();
}
