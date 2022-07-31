package me.salamander.astatine.compiler.constants;

import me.salamander.astatine.compiler.AstatineComponent;
import me.salamander.astatine.compiler.CompileContext;
import me.salamander.astatine.mixin.access.ImprovedNoiseAccess;
import me.salamander.astatine.util.Utils;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import org.lwjgl.system.CallbackI;

import java.util.Set;

public class ConstantGenerators {
    public static AstatineComponent generatePackedIntArray(CompileContext context, PackedIntArray arr) {
        int[] data = Utils.pack(arr.bitsPer(), Utils.boxArray(arr.values()));
        int valuesPerInt = 32 / arr.bitsPer();

        return new AstatineConstant(context, Set.of()) {
            @Override
            public String declare() {
                StringBuilder sb = new StringBuilder("const int ");
                sb.append(context.getConstantName(this));
                sb.append("[").append(data.length).append("] = {\n");

                for (int i = 0; i < data.length; i++) {
                    sb.append("    ");
                    sb.append(data[i]);

                    int extra = 4;

                    if (i != data.length - 1) {
                        sb.append(",");
                        extra--;
                    }

                    sb.append(" ".repeat(extra));
                    sb.append("// ");

                    for (int j = 0; j < valuesPerInt; j++) {
                        int idx = i * valuesPerInt + j;
                        if (idx >= arr.values().length) {
                            sb.append("[NO VALUE]");
                        } else {
                            sb.append(arr.values()[idx]);
                        }

                        if (j != valuesPerInt - 1) {
                            sb.append(", ");
                        }
                    }

                    sb.append("\n");
                }

                sb.append("};");

                return sb.toString();
            }

            @Override
            public String call(String... args) {
                if (args.length == 1) {
                    String indexName = args[0];
                    //(name[(indexName / valuesPerInt)] >> (indexName % valuesPerInt) * bitsPer) & ((1 << bitsPer) - 1)
                    return String.format(
                            "(%s[(%s / %d)] >> (%s %d) * %d) & (%d)",
                            context.getConstantName(this),
                            indexName,
                            valuesPerInt,
                            indexName,
                            valuesPerInt,
                            arr.bitsPer(),
                            (1 << arr.bitsPer()) - 1
                    );
                } else {
                    return super.call(args);
                }
            }
        };
    }

    private static void writeImprovedNoise(StringBuilder sb, ImprovedNoise improvedNoise) {
        ImprovedNoiseAccess access = (ImprovedNoiseAccess) (Object) improvedNoise;

        int[] perm = new int[64]; //256 bytes
        byte[] p = access.getP();

        for (int i = 0; i < p.length; i++) {
            perm[i / 4] |= p[i] << ((i % 4) * 8);
        }

        sb.append("ImprovedNoise(\n");

        sb.append("    ").append("{");

        for (int i = 0; i < perm.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }

            sb.append(perm[i]);
        }

        sb.append("},\n");

        sb.append("    ").append(access.getXo()).append(",\n");
        sb.append("    ").append(access.getYo()).append(",\n");
        sb.append("    ").append(access.getZo()).append("\n");
        sb.append(")");
    }

    public static AstatineComponent generateNoise(CompileContext context, ImprovedNoise noise) {
        return new AstatineConstant(context, Set.of()) {
            @Override
            public String declare() {
                StringBuilder sb = new StringBuilder();

                sb.append("const ImprovedNoise ").append(context.getConstantName(noise));
                sb.append(" = ");
                writeImprovedNoise(sb, noise);
                sb.append(";");

                return sb.toString();
            }

            @Override
            public String call(String... args) {
                if (args.length == 3) {
                    return "noise(" + context.getConstantName(this) + args[0] + ", " + args[1] + ", " + args[2] + ", 0.0, 0.0)";
                } else if (args.length == 5) {
                    return "noise(" + context.getConstantName(this) + args[0] + ", " + args[1] + ", " + args[2] + ", " + args[3] + ", " + args[4] + ")";
                } else {
                    return super.call(args);
                }
            }
        };
    }

    public static AstatineComponent generateNoiseArray(CompileContext context, ImprovedNoise[] noises) {
        return new AstatineConstant(context, Set.of()) {
            @Override
            public String declare() {
                StringBuilder sb = new StringBuilder();
                sb.append("const ImprovedNoise ").append(context.getConstantName(noises)).append("[").append(noises.length).append("] = {\n");

                int k = 0;
                for (ImprovedNoise improvedNoise : noises) {
                    if (k > 0) {
                        sb.append(",\n");
                    }
                    k++;
                    StringBuilder noiseBuilder = new StringBuilder();
                    writeImprovedNoise(noiseBuilder, improvedNoise);
                    sb.append(noiseBuilder.toString().indent(4));
                }

                sb.append("};\n");

                return sb.toString();
            }

            @Override
            public String call(String... args) {
                if (args.length == 1) {
                    return context.getConstantName(this) + "[" + args[0] + "]";
                } else if (args.length == 4) {
                    return "noise(" + context.getConstantName(this) + "[" + args[0] + "], " + args[1] + ", " + args[2] + ", " + args[3] + ", 0.0, 0.0)";
                } else if (args.length == 6) {
                    return "noise(" + context.getConstantName(this) + "[" + args[0] + "], " + args[1] + ", " + args[2] + ", " + args[3] + ", " + args[4] + ", " + args[5] + ")";
                } else {
                    return super.call(args);
                }
            }
        };
    }

    public static AstatineComponent generateIntArray(CompileContext context, int[] array) {
        return new AstatineConstant(context, Set.of()) {
            @Override
            public String declare() {
                StringBuilder sb = new StringBuilder();
                sb.append("const int ").append(context.getConstantName(array)).append("[").append(array.length).append("] = {\n");

                for (int i = 0; i < array.length; i++) {
                    sb.append("    ");
                    sb.append(array[i]);

                    if (i != array.length - 1) {
                        sb.append(",");
                    }

                    sb.append("\n");
                }

                sb.append("};\n");

                return sb.toString();
            }

            @Override
            public String call(String... args) {
                if (args.length == 1) {
                    return context.getConstantName(array) + "[" + args[0] + "]";
                } else {
                    return super.call(args);
                }
            }
        };
    }

    public static AstatineComponent generateFloatArray(CompileContext context, float[] array) {
        return new AstatineConstant(context, Set.of()) {
            @Override
            public String declare() {
                StringBuilder sb = new StringBuilder();
                sb.append("const float ").append(context.getConstantName(array)).append("[").append(array.length).append("] = {\n");

                for (int i = 0; i < array.length; i++) {
                    sb.append("    ");
                    sb.append(array[i]);

                    if (i != array.length - 1) {
                        sb.append(",");
                    }

                    sb.append("\n");
                }

                sb.append("};\n");

                return sb.toString();
            }

            @Override
            public String call(String... args) {
                if (args.length == 1) {
                    return context.getConstantName(array) + "[" + args[0] + "]";
                } else {
                    return super.call(args);
                }
            }
        };
    }

    public static AstatineComponent generateDoubleArray(CompileContext context, double[] array) {
        return new AstatineConstant(context, Set.of()) {
            @Override
            public String declare() {
                StringBuilder sb = new StringBuilder();
                sb.append("const double ").append(context.getConstantName(array)).append("[").append(array.length).append("] = {\n");

                for (int i = 0; i < array.length; i++) {
                    sb.append("    ");
                    sb.append(array[i]);

                    if (i != array.length - 1) {
                        sb.append(",");
                    }

                    sb.append("\n");
                }

                sb.append("};\n");

                return sb.toString();
            }

            @Override
            public String call(String... args) {
                if (args.length == 1) {
                    return context.getConstantName(array) + "[" + args[0] + "]";
                } else {
                    return super.call(args);
                }
            }
        };
    }
}
