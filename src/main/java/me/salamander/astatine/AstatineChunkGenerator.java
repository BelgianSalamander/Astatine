package me.salamander.astatine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.salamander.astatine.compiler.CompileContext;
import me.salamander.astatine.mixin.access.NoiseBasedChunkGeneratorAccess;
import me.salamander.astatine.mixin.access.SurfaceSystemAccess;
import me.salamander.astatine.util.Format;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL45.*;

public class AstatineChunkGenerator extends NoiseBasedChunkGenerator {
    public static final Codec<AstatineChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> commonCodec(instance).and(
            instance.group(RegistryOps.retrieveRegistry(Registry.NOISE_REGISTRY).forGetter((noiseBasedChunkGenerator) -> {
                return a(noiseBasedChunkGenerator).getNoises();
            }), BiomeSource.CODEC.fieldOf("biome_source").forGetter((noiseBasedChunkGenerator) -> {
                return noiseBasedChunkGenerator.biomeSource;
            }), Codec.LONG.fieldOf("seed").stable().forGetter((noiseBasedChunkGenerator) -> {
                return a(noiseBasedChunkGenerator).getSeed();
            }), NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter((noiseBasedChunkGenerator) -> {
                return a(noiseBasedChunkGenerator).getSettings();
            }))
    ).apply(instance, instance.stable(AstatineChunkGenerator::new)));
    private static final String PRELUDE;

    private final GLState glState = new GLState();
    private final ExecutorService glExecutor = Executors.newSingleThreadExecutor();

    public AstatineChunkGenerator(Registry<StructureSet> registry, Registry<NormalNoise.NoiseParameters> registry2, BiomeSource biomeSource, long l, Holder<NoiseGeneratorSettings> holder) {
        super(registry, registry2, biomeSource, l, holder);

        glExecutor.execute(this::initGL);
    }

    private final AtomicBoolean generatedShaders = new AtomicBoolean();

    private void ensureSetup(WorldGenRegion level) {
        if (!generatedShaders.get()) {
            init(level);
            generatedShaders.set(true);
        }
    }

    private synchronized void init(WorldGenRegion level) {
        if (generatedShaders.get()) {
            return;
        }

        makeCalculator(level);

        makeSurfaceRule(level);

        generatedShaders.set(true);
    }

    private void makeSurfaceRule(WorldGenRegion level) {
        Format base = Format.ofRes("/glsl/rule.glsl");

        NoiseGeneratorSettings settings = this.settings.value();

        CompileContext context = new CompileContext(
                level.registryAccess(),
                new WorldGenerationContext(
                        this,
                        level
                ),
                ((NoiseBasedChunkGeneratorAccess) this).getSurfaceSystem()
        );

        CompileContext.CompileResult result = context.generate(settings.surfaceRule());

        String source = PRELUDE + base.generate(
                "define", result.code(),
                "rule", context.getMethodName(context.getMethod(settings.surfaceRule())),
                "defaultBlock", String.valueOf(Block.BLOCK_STATE_REGISTRY.getId(this.defaultBlock)),
                "localSizeY", "16"
        );

        System.out.println("### ASTATINE SURFACE RULE SOURCE (GLSL) BEGIN ###");
        System.out.println(source);
        System.out.println("### ASTATINE SURFACE RULE SOURCE (GLSL) END ###");

        createComputeShader(source, program -> glState.surfaceRuleProgram = program);
    }

    private void makeCalculator(WorldGenRegion level) {
        Format base = Format.ofRes("/glsl/calculator.glsl");

        CompileContext context = new CompileContext(level.registryAccess(), new WorldGenerationContext(
                this,
                level
        ), ((NoiseBasedChunkGeneratorAccess) this).getSurfaceSystem());

        SurfaceSystemAccess access = (SurfaceSystemAccess) context.getSurfaceSystem();

        CompileContext.CompileResult result = context.generate(access.getSurfaceNoise(), access.getSurfaceSecondaryNoise());

        int air = Block.BLOCK_STATE_REGISTRY.getId(Blocks.AIR.defaultBlockState());

        int[] isFluid = new int[(int) Math.ceil(Block.BLOCK_STATE_REGISTRY.size() / 32.f)];

        for (BlockState state : Block.BLOCK_STATE_REGISTRY) {
            if (!state.getFluidState().isEmpty()) {
                int id = Block.BLOCK_STATE_REGISTRY.getId(state);

                isFluid[id >> 5] |= 1 << (id & 31);
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("{");
        for (int i = 0; i < isFluid.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }

            builder.append(isFluid[i]);
        }
        builder.append("}");

        String source = PRELUDE + "\n" + result.code() + "\n" + base.generate(
                "air", String.valueOf(air),
                "isFluid", builder.toString(),
                "surfaceNoise", context.getMethodName(context.getMethod(access.getSurfaceNoise())),
                "secondaryNoise", context.getMethodName(context.getMethod(access.getSurfaceSecondaryNoise()))
        );

        System.out.println("### CALCULATOR SOURCE (GLSL) BEGIN ###");
        System.out.println(source);
        System.out.println("### CALCULATOR SOURCE (GLSL) END ###");

        createComputeShader(source, program -> glState.calculatorProgram = program);
    }

    private void createComputeShader(String source, IntConsumer action) {
        glExecutor.submit(() -> {
            int program = glCreateProgram();

            int shader = glCreateShader(GL_COMPUTE_SHADER);
            glShaderSource(shader, source);
            glCompileShader(shader);

            int[] success = new int[1];

            glGetShaderiv(shader, GL_COMPILE_STATUS, success);
            if (success[0] == 0) {
                printError(source, glGetShaderInfoLog(shader), "Compute shader compilation error!", "Compilation error details:");
                return;
            }

            glAttachShader(program, shader);

            glLinkProgram(program);

            glGetProgramiv(program, GL_LINK_STATUS, success);
            if (success[0] == 0) {
                printError(source, glGetProgramInfoLog(program), "Compute shader linking error!", "Linking error details:");
                return;
            }

            glDeleteShader(shader);

            action.accept(program);
        });
    }

    private void printError(String source, String log, String header, String s3) {
        System.out.println(header);
        System.out.println(s3);
        System.out.println(log);
        System.out.println("Code:");

        String[] lines = source.split("\\n");

        for (int i = 0; i < lines.length; i++) {
            System.out.printf("%.4d | %s\n", i + 1, lines[i]);
        }
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureFeatureManager structureFeatureManager, ChunkAccess chunk) {
        ensureSetup(level);

        //Fill block buffer
        IntBuffer blockBuffer = MemoryUtil.memAllocInt(16 * 16 * level.getHeight());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < level.getHeight(); y++) {
                for (int z = 0; z < 16; z++) {
                    blockBuffer.put(Block.BLOCK_STATE_REGISTRY.getId(chunk.getBlockState(pos.set(x, y, z))));
                }
            }
        }

        Registry<Biome> biomes = level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);

        //Fill biome buffer
        IntBuffer biomeBuffer = MemoryUtil.memAllocInt(16 * 16 * level.getHeight());
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < level.getHeight(); y++) {
                for (int z = 0; z < 16; z++) {
                    Holder<Biome> biome = level.getBiome(
                            pos.set(chunk.getPos().x * 16 + x, y, chunk.getPos().z * 16 + z)
                    );

                    biomeBuffer.put(biomes.getId(biome.value()));
                }
            }
        }



        MemoryUtil.memFree(blockBuffer);
        MemoryUtil.memFree(biomeBuffer);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return AstatineChunkGenerator.CODEC;
    }

    private static NoiseBasedChunkGeneratorAccess a(NoiseBasedChunkGenerator a) {
        return (NoiseBasedChunkGeneratorAccess) a;
    }

    private NoiseBasedChunkGeneratorAccess a() {
        return (NoiseBasedChunkGeneratorAccess) this;
    }

    //OpenGL (All these methods should be called from the GL thread)
    private void initGL() {
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 5);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        glState.window = glfwCreateWindow(1, 1, "YOU SHOULDN'T BE SEEING THIS", 0L, 0L);

        if (glState.window == 0L) {
            throw new IllegalStateException("Unable to create GLFW window");
        }

        glfwMakeContextCurrent(glState.window);

        GL.createCapabilities();
    }

    private void shutdownGL() {
        glfwDestroyWindow(glState.window);
        glfwTerminate();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        glExecutor.execute(this::shutdownGL);
        glExecutor.shutdown();
    }

    private static class GLState {
        private long window;

        private int surfaceRuleProgram;
        private int calculatorProgram;
    }

    static {
        PRELUDE = Format.ofRes("/glsl/prelude.glsl").generate();
    }
}
