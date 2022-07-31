package me.salamander.astatine.compiler;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import me.salamander.astatine.compiler.constants.AstatineGlobal;
import me.salamander.astatine.compiler.constants.PackedIntArray;
import me.salamander.astatine.mixin.access.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

import java.util.*;
import java.util.function.Predicate;

public class MethodGenerators {
    public static AstatineMethod stoneDepthCheck(CompileContext context, SurfaceRules.StoneDepthCheck check) {
        return new AstatineMethod.Condition(context, "stone depth") {
            @Override
            protected void generateBody(Writer result) {
                StringBuilder value = new StringBuilder();

                value.append(check.surfaceType() == CaveSurface.CEILING ? context.getStoneDepthBelow() : context.getStoneDepthAbove());

                value.append(" <= 1 + ");
                value.append(check.offset());

                if (check.addSurfaceDepth()) {
                    value.append(" + ");
                    value.append(context.getSurfaceDepth());
                }

                if (check.secondaryDepthRange() != 0) {
                    value.append(" + ");

                    value.append("int(map(");
                    value.append(context.getSecondaryDepth());
                    value.append(", -1.0, 1.0, 0.0, double(");
                    value.append(check.secondaryDepthRange());
                    value.append(")))");
                }

                result.addLine("return " + value + ";");
            }

            @Override
            public Set<AstatineComponent> dependsOn() {
                return Set.of();
            }
        };
    }

    public static AstatineMethod not(CompileContext context, SurfaceRules.NotConditionSource not) {
        return new AstatineMethod.Condition(context, "not") {
            @Override
            protected void generateBody(Writer result) {
                result.addLine("return !" + context.getMethod(not.target()).call() + ";");
            }

            @Override
            public Set<AstatineComponent> dependsOn() {
                return Set.of(context.getMethod(not.target()));
            }
        };
    }

    public static AstatineMethod y(CompileContext context, SurfaceRules.YConditionSource y) {
        return new AstatineMethod.Condition(context, "y") {
            @Override
            protected void generateBody(Writer result) {
                StringBuilder sb = new StringBuilder();
                sb.append("return ");

                sb.append(context.getY());

                if (y.addStoneDepth()) {
                    sb.append(" + ");
                    sb.append(context.getStoneDepthAbove());
                }

                sb.append(" >= ");

                sb.append(y.anchor().resolveY(context.getContext()));

                sb.append(" + ");

                sb.append(context.getSurfaceDepth());
                sb.append(" * ");
                sb.append(y.surfaceDepthMultiplier());

                sb.append(";");

                result.addLine(sb.toString());
            }

            @Override
            public Set<AstatineComponent> dependsOn() {
                return Set.of();
            }
        };
    }

    public static AstatineMethod water(CompileContext context, SurfaceRules.WaterConditionSource water) {
        return new AstatineMethod.Condition(context, "water") {
            @Override
            protected void generateBody(Writer result) {
                result.addLine("if (" + context.getWaterHeight() + " == " + Integer.MIN_VALUE + ") {");

                result.addLine("return true;");

                result.addLine("} else {");

                StringBuilder sb = new StringBuilder("return ");
                sb.append(context.getY());

                if (water.addStoneDepth()) {
                    sb.append(" + ");
                    sb.append(context.getStoneDepthAbove());
                }

                sb.append(" >= ");

                sb.append(context.getWaterHeight());

                sb.append(" + ").append(water.offset());

                sb.append(" + ").append(context.getSurfaceDepth()).append(" * ").append(water.surfaceDepthMultiplier());

                sb.append(";");

                result.addLine(sb.toString());

                result.addLine("}");
            }

            @Override
            public Set<AstatineComponent> dependsOn() {
                return Set.of();
            }
        };
    }

    public static AstatineMethod biome(CompileContext context, SurfaceRules.BiomeConditionSource biomeSource) {
        return makeBiomeCheck(context, ((BiomeConditionSourceAccess) (Object) biomeSource).getBiomeNameTest());
    }

    private static AstatineMethod makeBiomeCheck(CompileContext context, Predicate<ResourceKey<Biome>> test) {
        Registry<Biome> biomes = context.getBiomes();

        boolean[] predicate = new boolean[biomes.size()];

        for (int i = 0; i < biomes.size(); i++) {
            Biome biome = biomes.byId(i);
            int finalI = i;
            biomes.getResourceKey(biome).ifPresentOrElse(key -> {
                    if (test.test(key)) {
                        predicate[finalI] = true;
                    }
                },
                () -> { throw new IllegalStateException("Biome " + biome + " has no key"); }
            );
        }

        AstatineComponent bitSet = context.getConstant(PackedIntArray.ofBool(predicate));

        return new AstatineMethod.Condition(context, "biome") {
            @Override
            protected void generateBody(Writer result) {
                result.addLine("uint biomeIndex = " + context.getBiome() + ";");

                result.addLine("return bool(" + bitSet.call("biomeIndex") + ");");
            }

            @Override
            public Set<AstatineComponent> dependsOn() {
                return Set.of(bitSet);
            }
        };
    }

    public static AstatineMethod noise(CompileContext context, SurfaceRules.NoiseThresholdConditionSource noiseThreshold) {
        ResourceKey<NormalNoise.NoiseParameters> noiseKey = noiseThreshold.noise();

        NormalNoise noise = ((SurfaceSystemAccess) context.getSurfaceSystem()).invokeGetOrCreateNoise(noiseKey);

        AstatineComponent sampler = context.getMethod(noise);

        return new AstatineMethod.Condition(context, "noise threshold") {
            @Override
            protected void generateBody(AstatineMethod.Writer result) {
                //result.addLine("double value = " + sampler.getName() + "(double(" + context.getX() + "), 0.0, double(" + context.getZ() + "));");
                result.addLine("double value = " + sampler.call("double(" + context.getX() + ")", "0.0", "double(" + context.getZ() + ")") + ";");

                result.addLine("return d >= " + noiseThreshold.minThreshold() + " && d <= " + noiseThreshold.maxThreshold() + ";");
            }

            @Override
            public Set<AstatineComponent> dependsOn() {
                return Set.of(sampler);
            }
        };
    }

    public static AstatineMethod verticalGradient(CompileContext context, SurfaceRules.VerticalGradientConditionSource verticalGradient) {
        int trueAtAndBelow = verticalGradient.trueAtAndBelow().resolveY(context.getContext());
        int falseAtAndAbove = verticalGradient.falseAtAndAbove().resolveY(context.getContext());

        return new AstatineMethod.Condition(context, "gradient") {
            @Override
            protected void generateBody(AstatineMethod.Writer result) {
                result.addLine("if (" + context.getY() + " <= " + trueAtAndBelow + ") {");
                result.addLine("    return true;");
                result.addLine("} else if (" + context.getY() + " >= " + falseAtAndAbove + ") {");
                result.addLine("    return false;");
                result.addLine("}");

                result.addLine("double threshold = map(" + context.getY() + ", " + trueAtAndBelow + ", " + falseAtAndAbove + ", 1.0, 0.0);");

                result.addLine("double value = double(nextFloat(xoroshiroAt(" + context.getX() + ", " + context.getZ() + ")));");

                result.addLine("return value < threshold;");
            }

            @Override
            public Set<AstatineComponent> dependsOn() {
                return Set.of();
            }
        };
    }

    public static AstatineMethod steep(CompileContext context, SurfaceRules.Steep dud) {
        return new AstatineMethod.Condition(context, "steep") {
            @Override
            protected void generateBody(AstatineMethod.Writer result) {
                result.addLine("int xo = " + context.getX() + " & 0xf;");
                result.addLine("int zo = " + context.getZ() + " & 0xf;");

                result.addLine("int z_below = max(0, zo - 1);");
                result.addLine("int z_above = min(15, zo + 1);");

                result.addLine("int height_z_below = " + context.getHeight("xo", "z_below") + ";");
                result.addLine("int height_z_above = " + context.getHeight("xo", "z_above") + ";");

                result.addLine("if (height_z_above >= height_z_below + 4) {");
                result.addLine("    return true;");
                result.addLine("} else {");
                result.addLine("int x_below = max(0, xo - 1);");
                result.addLine("int x_above = min(15, xo + 1);");

                result.addLine("int height_x_below = " + context.getHeight("x_below", "zo") + ";");
                result.addLine("int height_x_above = " + context.getHeight("x_above", "zo") + ";");

                result.addLine("return height_x_below >= height_x_above + 4;");

                result.addLine("}");
            }

            @Override
            public int hashCode() {
                return context.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == null) {
                    return false;
                }

                if (obj == this) {
                    return true;
                }

                if (obj.getClass() != getClass()) {
                    return false;
                }

                return context.equals(((AstatineMethod.Condition) obj).context);
            }

            @Override
            public Set<AstatineComponent> dependsOn() {
                return Set.of();
            }
        };
    }

    public static AstatineMethod hole(CompileContext context, SurfaceRules.Hole dud) {
        return new AstatineMethod.Condition(context, "hole") {
            @Override
            protected void generateBody(AstatineMethod.Writer result) {
                result.addLine("return " + context.getSurfaceDepth() + " <= 0;");
            }

            @Override
            public int hashCode() {
                return context.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == null) {
                    return false;
                }

                if (obj == this) {
                    return true;
                }

                if (obj.getClass() != getClass()) {
                    return false;
                }

                return context.equals(((AstatineMethod.Condition) obj).context);
            }
        };
    }

    public static AstatineMethod abovePreliminarySurface(CompileContext context, SurfaceRules.AbovePreliminarySurface dud) {
        return new AstatineMethod.Condition(context, "abovePreliminarySurface") {
            @Override
            protected void generateBody(AstatineMethod.Writer result) {
                result.addLine("return " + context.getY() + " >= " + context.getMinSurfaceLevel() + ";");
            }

            @Override
            public int hashCode() {
                return context.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == null) {
                    return false;
                }

                if (obj == this) {
                    return true;
                }

                if (obj.getClass() != getClass()) {
                    return false;
                }

                return context.equals(((AstatineMethod.Condition) obj).context);
            }
        };
    }


    public static AstatineMethod test(CompileContext context, SurfaceRules.TestRuleSource test) {
        AstatineComponent condition = context.getMethod(test.ifTrue());
        AstatineComponent then = context.getMethod(test.thenRun());

        return new AstatineMethod.SurfaceRule(context, "Test") {
            @Override
            protected void generateBody(AstatineMethod.Writer result) {
                result.addLine("if (" + condition.call() + ") {");
                result.addLine("return " + then.call() + ";");
                result.addLine("}");

                result.addLine("return -1;");
            }

            @Override
            public Set<AstatineComponent> dependsOn() {
                return Set.of(condition, then);
            }
        };
    }

    public static AstatineMethod sequence(CompileContext context, SurfaceRules.SequenceRuleSource sequence) {
        List<AstatineComponent> components = sequence.sequence().stream()
                .map(context::getMethod)
                .toList();

        Set<AstatineComponent> dependsOn = new HashSet<>(components);

        return new AstatineMethod.SurfaceRule(context, "Sequence") {
            @Override
            protected void generateBody(AstatineMethod.Writer result) {
                for (AstatineComponent rule : components) {
                    result.addLine("int block = " + rule.call() + ";");
                    result.addLine("if (block != -1) {");
                    result.addLine("return block;");
                    result.addLine("}");
                }

                result.addLine("return -1;");
            }

            @Override
            public Set<AstatineComponent> dependsOn() {
                return dependsOn;
            }
        };
    }

    public static AstatineComponent block(CompileContext context, SurfaceRules.BlockRuleSource block) {
        return new AstatineInlineConstant(context.getBlockStates().getId(block.resultState()) + " /*" + block.resultState() + " */");
    }

    public static AstatineMethod bandlands(CompileContext context, SurfaceRules.Bandlands dud) {
        SurfaceSystemAccess access = (SurfaceSystemAccess) context.getSurfaceSystem();

        int[] clayBands = Arrays.stream(access.getClayBands()).mapToInt(b -> context.getBlockStates().getId(b)).toArray();
        AstatineComponent clayBandsConst = context.getConstant(clayBands);
        AstatineComponent noise = context.getMethod(access.getClayBandsOffsetNoise());

        return new AstatineMethod.SurfaceRule(context, "Bandlands") {
            @Override
            protected void generateBody(AstatineMethod.Writer result) {

                //.map(b -> context.getBlockStates().getId(b)).toList();

                result.addLine("int offset = int(round(" + noise.call(context.getX(), "0", context.getZ()) + " * 4));");

                result.addLine("int bandIndex = (" + context.getY() + " offset + " + clayBands.length + ") % " + clayBands.length + ";");

                result.addLine("return " + clayBandsConst + "[bandIndex];");
            }

            @Override
            public Set<AstatineComponent> dependsOn() {
                return Set.of();
            }
        };
    }

    public static AstatineMethod temperature(CompileContext context, SurfaceRules.Temperature dud) {
        Map<Biome.TemperatureModifier, AstatineComponent> temperatureModifiers = new HashMap<>();

        AstatineComponent temperatureNoise = context.getMethod(CompileContext.TEMPERATURE_NOISE);
        AstatineComponent frozenTemperatureNoise = context.getMethod(CompileContext.FROZEN_TEMPERATURE_NOISE);
        AstatineComponent biomeInfoNoise = context.getMethod(CompileContext.BIOME_INFO_NOISE);

        AstatineComponent memoizedTemperature = new AstatineGlobal(context, "float", "0.f / 0.f");

        temperatureModifiers.put(Biome.TemperatureModifier.NONE, new AstatineMethod(context) {
            @Override
            protected void generateBody(Writer writer) {
                writer.addLine("return " + this.getArgName(0) + ";");
            }

            @Override
            protected String getReturnType() {
                return "float";
            }

            @Override
            protected String[] getArgTypes() {
                return new String[] {"float"};
            }

            @Override
            public Set<AstatineComponent> dependsOn() {
                return Set.of();
            }
        });

        temperatureModifiers.put(Biome.TemperatureModifier.FROZEN, new AstatineMethod(context) {
            @Override
            protected void generateBody(Writer result) {
                result.addLine("double d = " + frozenTemperatureNoise.call(context.getX() + " * 0.05", "0.0", context.getZ() + " * 0.05") + " * 7.0;");
                result.addLine("double e = " + biomeInfoNoise.call(context.getX() + " * 0.2", "0.0", context.getZ() + " * 0.2") + ";");
                result.addLine("double f = e + f;");

                result.addLine("if (f < 0.3) {");
                result.addLine("double g = " + biomeInfoNoise.call(context.getX() + " * 0.09", "0.0", context.getZ() + " * 0.09") + ";");
                result.addLine("if (g < 0.8) {");
                result.addLine("return 0.2f;");
                result.addLine("}");
                result.addLine("}");

                result.addLine("return " + this.getArgName(0) + ";");
            }

            @Override
            protected String getReturnType() {
                return "float";
            }

            @Override
            protected String[] getArgTypes() {
                return new String[] {"float"};
            }

            @Override
            public Set<AstatineComponent> dependsOn() {
                return Set.of(frozenTemperatureNoise, biomeInfoNoise);
            }
        });

        byte[] biomeTemperatureType = new byte[context.getBiomes().size()];
        float[] baseTemperature = new float[context.getBiomes().size()];

        for(int i = 0; i < biomeTemperatureType.length; i++) {
            Biome biome = context.getBiomes().byId(i);
            Biome.ClimateSettings climateSettings = ((BiomeAccess) (Object) biome).getClimateSettings();
            Biome.TemperatureModifier temperatureModifier = ((ClimateSettingsAccess) climateSettings).getTemperatureModifier();

            biomeTemperatureType[i] = (byte) temperatureModifier.ordinal();
            baseTemperature[i] = biome.getBaseTemperature();
        }

        AstatineComponent biomeTemperatureTypeConstant = context.getConstant(PackedIntArray.ofBytes(biomeTemperatureType));
        AstatineComponent baseTemperatureConstant = context.getConstant(baseTemperature);

        AstatineMethod getTemperature = new AstatineMethod(context) {
            @Override
            protected void generateBody(Writer result) {
                result.addLine("if(!isnan(" + memoizedTemperature.call() + ")) {");
                result.addLine("return " + memoizedTemperature.call() + ";");
                result.addLine("}");

                result.addLine("int biomeID = " + context.getBiome() + ";");
                result.addLine("int temperatureType = " + biomeTemperatureTypeConstant.call("biomeID") + ";");
                result.addLine("float baseTemperature = " + baseTemperatureConstant.call("biomeID") + ";");
                result.addLine("float modifiedTemperature;");

                result.addLine("switch(temperatureType) {");

                for(Biome.TemperatureModifier modifier : Biome.TemperatureModifier.values()) {
                    result.addLine("case " + modifier.ordinal() + ":");
                    result.addLine("modifiedTemperature = " + temperatureModifiers.get(modifier).call("baseTemperature") + ";");
                    result.addLine("break;");
                }

                result.addLine("default:");
                result.addLine(memoizedTemperature.call() + " = -1;");
                result.addLine("return -1;");

                result.addLine("}");

                result.addLine("if (" + context.getY() + " > 80) {");
                result.addLine("float g = " + temperatureNoise.call(context.getX() + " / 8.0f, 0, " + context.getZ() + " / 8.0f") + ";");
                result.addLine("float result = baseTemperature - (g + " + context.getY() + " - 80.0f) * 0.05f / 40.0f");
                result.addLine(memoizedTemperature.call() + " = result;");
                result.addLine("return result;");
                result.addLine("}");

                result.addLine(memoizedTemperature.call() + " = modifiedTemperature;");
                result.addLine("return modifiedTemperature;");
            }

            @Override
            protected String getReturnType() {
                return "float";
            }

            @Override
            protected String[] getArgTypes() {
                return new String[] {};
            }

            @Override
            public Set<AstatineComponent> dependsOn() {
                Set<AstatineComponent> result = new HashSet<>();

                result.add(temperatureNoise);

                for(Biome.TemperatureModifier modifier : Biome.TemperatureModifier.values()) {
                    result.add(temperatureModifiers.get(modifier));
                }

                result.add(biomeTemperatureTypeConstant);
                result.add(baseTemperatureConstant);
                result.add(memoizedTemperature);

                return result;
            }
        };

        return new AstatineMethod.Condition(context) {
            @Override
            protected void generateBody(Writer writer) {
                writer.addLine("return " + getTemperature.call() + " < 0.15f;");
            }

            @Override
            public Set<AstatineComponent> dependsOn() {
                return Set.of(getTemperature);
            }
        };
    }

    public static AstatineMethod normalNoise(CompileContext context, NormalNoise normalNoise) {
        NormalNoiseAccess access = (NormalNoiseAccess) normalNoise;

        AstatineComponent firstSampler = context.getMethod(access.getFirst());
        AstatineComponent secondSampler = context.getMethod(access.getSecond());

        return new AstatineMethod(context) {
            @Override
            protected void generateBody(AstatineMethod.Writer result) {
                String mult = " * " + NormalNoiseAccess.getINPUT_FACTOR() + ";";

                result.addLine("double new_x = " + this.getArgName(0) + mult);
                result.addLine("double new_y = " + this.getArgName(1) + mult);
                result.addLine("double new_z = " + this.getArgName(2) + mult);

                result.addLine("double first_result = " + firstSampler.call("new_x", "new_y", "new_z") + ";");

                result.addLine("double second_result = " + secondSampler.call("new_x", "new_y", "new_z") + ";");

                result.addLine("return (first_result + second_result) * " + access.getValueFactor() + ";");
            }

            @Override
            protected String getReturnType() {
                return "double";
            }

            @Override
            protected String[] getArgTypes() {
                return new String[]{
                        "double",
                        "double",
                        "double"
                };
            }

            @Override
            public Set<AstatineComponent> dependsOn() {
                return Set.of(firstSampler, secondSampler);
            }
        };
    }

    public static AstatineMethod perlinNoise(CompileContext context, PerlinNoise perlinNoise) {
        PerlinNoiseAccess perlinAccess = (PerlinNoiseAccess) perlinNoise;

        List<ImprovedNoise> notNullNoises = new ArrayList<>();
        DoubleList amplitudes = new DoubleArrayList();
        DoubleList frequencies = new DoubleArrayList();
        DoubleList amplitudes2 = new DoubleArrayList();

        double frequency = perlinAccess.getLowestFreqInputFactor();
        double amplitude = perlinAccess.getLowestFreqValueFactor();
        for (int i = 0; i < perlinAccess.getNoiseLevels().length; i++) {
            ImprovedNoise n = perlinAccess.getNoiseLevels()[i];

            if (n != null) {
                notNullNoises.add(n);
                amplitudes.add(perlinAccess.getAmplitudes().getDouble((i)));
                frequencies.add(frequency);
                amplitudes2.add(amplitude);
            }

            frequency *= 2;
            amplitude /= 2;
        }

        AstatineComponent noises = context.getConstant(notNullNoises.toArray(new ImprovedNoise[0]));

        AstatineComponent amplitudeParams = context.getConstant(perlinAccess.getAmplitudes().toDoubleArray());
        AstatineComponent freq = context.getConstant(frequencies.toDoubleArray());
        AstatineComponent amp = context.getConstant(amplitudes.toDoubleArray());

        return new AstatineMethod(context) {
            @Override
            protected void generateBody(AstatineMethod.Writer result) {
                result.addLine("double d = 0.0;");

                result.addLine("for (int i = 0; i < " + notNullNoises.size() + "; i++) {");

                StringBuilder sb = new StringBuilder();

                sb.append("(" + noises.call(
                        "i",
                        "wrap(" + this.getArgName(0) + " * " + freq.call("i") + ")",
                        this.getArgName(5) + " ? -" + noises.call("i") + ".yo : " + "wrap(" + this.getArgName(1) + " * " + freq.call("i") + ")",
                        "wrap(" + this.getArgName(2) + " * " + freq.call("i") + ")",
                        "wrap(" + this.getArgName(3) + " * " + freq.call("i") + ")",
                        "wrap(" + this.getArgName(4) + " * " + freq.call("i") + ")"
                ) + " * " + amp.call("i") + " * " + amplitudeParams.call("i") + ")");

                result.addLine("}");

                result.addLine("return d;");
            }

            @Override
            public String call(String... args) {
                if (args.length == 3) {
                    return super.call(args[0], args[1], args[2], "0.0", "0.0", "false");
                } else {
                    return super.call(args);
                }
            }

            @Override
            protected String getReturnType() {
                return "double";
            }

            @Override
            protected String[] getArgTypes() {
                return new String[]{
                        "double",
                        "double",
                        "double",
                        "double",
                        "double",
                        "bool"
                };
            }

            @Override
            public Set<AstatineComponent> dependsOn() {
                return Set.of(
                        noises,
                        amplitudeParams,
                        freq,
                        amp
                );
            }
        };
    }
}
