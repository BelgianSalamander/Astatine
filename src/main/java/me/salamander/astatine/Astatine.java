package me.salamander.astatine;

import me.salamander.astatine.mixin.access.WorldPresetAccess;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.gui.screens.worldselection.WorldPreset;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

public class Astatine implements ModInitializer {
    public static final AstatineWorldPreset PRESET = new AstatineWorldPreset("astatine");

    @Override
    public void onInitialize() {
        WorldPresetAccess.getPRESETS().add(0, PRESET);
    }

    public static class AstatineWorldPreset extends WorldPreset {
        public AstatineWorldPreset(String string) {
            super(string);
        }

        @Override
        public ChunkGenerator generator(RegistryAccess registry, long seed) {
            return new AstatineChunkGenerator(
                    registry.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY),
                    registry.registryOrThrow(Registry.NOISE_REGISTRY),
                    MultiNoiseBiomeSource.Preset.OVERWORLD.biomeSource(
                            registry.registryOrThrow(Registry.BIOME_REGISTRY)
                    ),
                    seed,
                    registry.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY).getOrCreateHolder(NoiseGeneratorSettings.OVERWORLD)
            );
        }
    }
}
