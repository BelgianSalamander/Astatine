package me.salamander.astatine.compiler;

import com.google.common.collect.ImmutableList;
import me.salamander.astatine.compiler.constants.ConstantGenerator;
import me.salamander.astatine.compiler.constants.ConstantGenerators;
import me.salamander.astatine.compiler.constants.PackedIntArray;
import me.salamander.astatine.util.Utils;
import net.minecraft.core.IdMapper;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class CompileContext {
    //Biome actually uses SimplexNoise for this but I'm using these because I can't be bothered making them in GLSL right now :/
    static final PerlinNoise TEMPERATURE_NOISE = PerlinNoise.create(new WorldgenRandom(new LegacyRandomSource(1234L)), ImmutableList.of(0));
    static final PerlinNoise FROZEN_TEMPERATURE_NOISE = PerlinNoise.create(new WorldgenRandom(new LegacyRandomSource(3456L)), ImmutableList.of(-2, -1, 0));
    static final PerlinNoise BIOME_INFO_NOISE = PerlinNoise.create(new WorldgenRandom(new LegacyRandomSource(2345L)), ImmutableList.of(0));

    private static final Class<?>[] CACHE_CLASSES = {
            SurfaceRules.RuleSource.class,
            SurfaceRules.ConditionSource.class,
            NormalNoise.class,
            PerlinNoise.class
    };

    private final AtomicInteger methodNameCounter = new AtomicInteger();
    private final Map<AstatineComponent, String> methodNames = new HashMap<>();
    private final Map<Object, AstatineComponent> methodLookup = new HashMap<>();

    private final AtomicInteger constantNameCounter = new AtomicInteger();
    private final Map<Object, String> constantNames = new HashMap<>();
    private final Map<Object, AstatineComponent> constantLookup = new HashMap<>();

    private final Map<Class<?>, BiFunction<CompileContext, Object, AstatineComponent>> methodGenerators = new HashMap<>();
    private final Map<Class<?>, BiFunction<CompileContext, Object, AstatineComponent>> constantGenerators = new HashMap<>();

    private final Registry<Biome> biomes;
    private final Registry<NormalNoise.NoiseParameters> noise;
    private final IdMapper<BlockState> blockStates;

    private final WorldGenerationContext context;
    private final SurfaceSystem surfaceSystem;

    public CompileContext(RegistryAccess access, WorldGenerationContext context, SurfaceSystem surfaceSystem) {
        this.biomes = access.registryOrThrow(Registry.BIOME_REGISTRY);
        this.noise = access.registryOrThrow(Registry.NOISE_REGISTRY);
        this.blockStates = Block.BLOCK_STATE_REGISTRY;
        this.context = context;
        this.surfaceSystem = surfaceSystem;

        registerDefault();
    }

    public CompileResult generate(Object... calls) {
        StringBuilder sb = new StringBuilder();

        Map<AstatineComponent, List<AstatineComponent>> dependents = new HashMap<>();
        Map<AstatineComponent, String> declarations = new HashMap<>();

        Set<AstatineComponent> allComponents = new HashSet<>();
        Queue<AstatineComponent> toProcess = new ArrayDeque<>();

        Map<Object, AstatineCallable> callables = new HashMap<>();

        for (Object call : calls) {
            AstatineComponent callable;
            if (methodGenerators.containsKey(call.getClass())) {
                callable = getMethod(call);
            } else {
                callable = getConstant(call);
            }
            callables.put(call, callable);
            toProcess.add(callable);
        }

        while (!toProcess.isEmpty()) {
            AstatineComponent component = toProcess.poll();
            allComponents.add(component);

            if (!component.needsPreDeclaration() || declarations.containsKey(component)) {
                continue;
            }

            String result = component.declare();

            declarations.put(component, result);

            for (AstatineComponent dependency: component.dependsOn()) {
                dependents.computeIfAbsent(dependency, k -> new ArrayList<>()).add(component);
                toProcess.add(dependency);
            }
        }

        List<AstatineComponent> declarationOrder = Utils.topologicalSort(
                allComponents,
                (method) -> dependents.getOrDefault(method, Collections.emptyList())
        );

        for (AstatineComponent method : declarationOrder) {
            sb.append(declarations.get(method)).append("\n\n");
        }

        return new CompileResult(sb.toString(), callables);
    }

    public Registry<Biome> getBiomes() {
        return biomes;
    }

    public IdMapper<BlockState> getBlockStates() {
        return blockStates;
    }

    public Registry<NormalNoise.NoiseParameters> getNoise() {
        return noise;
    }

    public SurfaceSystem getSurfaceSystem() {
        return surfaceSystem;
    }

    public String getMethodName(AstatineComponent callable) {
        return methodNames.computeIfAbsent(callable, (k) -> "__element_" + methodNameCounter.getAndIncrement());
    }

    public AstatineComponent getMethod(Object key) {
        for (Class<?> clazz : CACHE_CLASSES) {
            if (clazz.isAssignableFrom(key.getClass())) {
                if (!methodLookup.containsKey(key)) {
                    methodLookup.put(key, generateMethod(key));
                }
                return methodLookup.get(key);
            }
        }

        throw new IllegalArgumentException("Unsupported key type: " + key.getClass().getName());
    }

    private AstatineComponent generateMethod(Object o) {
        BiFunction<CompileContext, Object, AstatineComponent> generator = methodGenerators.get(o.getClass());

        if (generator == null) {
            throw new IllegalArgumentException("Unsupported key type: " + o.getClass().getName());
        }

        AstatineComponent method = generator.apply(this, o);

        return method;
    }

    public <T> String getConstantName(T constant) {
        return constantNames.computeIfAbsent(constant, (k) -> "constant_" + constantNameCounter.getAndIncrement());
    }

    public AstatineComponent getConstant(Object key) {
        if (!constantLookup.containsKey(key)) {
            constantLookup.put(key, generateConstant(key));
        }

        return constantLookup.get(key);
    }

    private AstatineComponent generateConstant(Object o) {
        BiFunction<CompileContext, Object, AstatineComponent> generator = constantGenerators.get(o.getClass());

        if (generator == null) {
            throw new IllegalArgumentException("Unsupported key type: " + o.getClass().getName());
        }

        AstatineComponent constant = generator.apply(this, o);

        return constant;
    }

    public WorldGenerationContext getContext() {
        return context;
    }

    public String getX() {
        return "block_x";
    }

    public String getY() {
        return "block_y";
    }

    public String getZ() {
        return "block_z";
    }

    public String getSurfaceDepth() {
        return "b_surfaceDepth.data[" + get2DIndex(getX(), getZ()) + "]"; //TODO: Calculate this
    }

    public String getSecondaryDepth() {
        return "b_secondaryDepth.data[" + get2DIndex(getX(), getZ()) + "]";
    }

    public String getWaterHeight() {
        return "b_waterHeight.data[" + get3DIndex(getX(), getY(), getZ()) + "]";
    }

    public String getStoneDepthAbove() {
        return "b_stoneDepthAbove.data[" + get3DIndex(getX(), getY(), getZ()) + "]";
    }

    public String getStoneDepthBelow() {
        return "b_stoneDepthBelow.data[" + get3DIndex(getX(), getY(), getZ()) + "]";
    }

    public String getBiome() {
        return "b_biomeData.data[" + get3DIndex(getX(), getY(), getZ()) + "]";
    }

    public String getHeight(String x, String z) {
        return "b_heightmap.data[" + get2DIndex(x, z) + "]";
    }

    public String getMinSurfaceLevel() {
        return "b_minSurfaceLevel.data[" + get2DIndex(getX(), getZ()) + "]";
    }

    private String getBlockType(String x, String y, String z) {
        return "b_blockData.data[" + get3DIndex(x, y, z) + "]";
    }

    private String get2DIndex(String x, String z) {
        return wrap(x) + " * 16 + " + wrap(z);
    }

    private String get3DIndex(String x, String y, String z) {
        return wrap(x) + " * 16 * u_height + (" + y + " - low_y) * 16 + " + wrap(z);
    }

    private String wrap(String s) {
        return "(" + s + " & 0xF)";
    }

    public int getBiomeId(Biome biome) {
        return biomes.getId(biome);
    }

    private <T> void registerMethodGenerator(Class<T> clazz, MethodGenerator<? super T> generator) {
        methodGenerators.put(clazz, generator::applyUnsafe);
    }

    private <T> void registerConstantGenerator(Class<T> clazz, ConstantGenerator<? super T> generator) {
        constantGenerators.put(clazz, generator::applyUnsafe);
    }

    private void registerDefault() {
        //Methods
        registerMethodGenerator(SurfaceRules.StoneDepthCheck.class, MethodGenerators::stoneDepthCheck);
        registerMethodGenerator(SurfaceRules.NotConditionSource.class, MethodGenerators::not);
        registerMethodGenerator(SurfaceRules.YConditionSource.class, MethodGenerators::y);
        registerMethodGenerator(SurfaceRules.WaterConditionSource.class, MethodGenerators::water);
        registerMethodGenerator(SurfaceRules.BiomeConditionSource.class, MethodGenerators::biome);
        registerMethodGenerator(SurfaceRules.NoiseThresholdConditionSource.class, MethodGenerators::noise);
        registerMethodGenerator(SurfaceRules.VerticalGradientConditionSource.class, MethodGenerators::verticalGradient);
        registerMethodGenerator(SurfaceRules.Steep.class, MethodGenerators::steep);
        registerMethodGenerator(SurfaceRules.Hole.class, MethodGenerators::hole);
        registerMethodGenerator(SurfaceRules.AbovePreliminarySurface.class, MethodGenerators::abovePreliminarySurface);
        registerMethodGenerator(SurfaceRules.Temperature.class, MethodGenerators::temperature);

        registerMethodGenerator(NormalNoise.class, MethodGenerators::normalNoise);
        registerMethodGenerator(PerlinNoise.class, MethodGenerators::perlinNoise);

        registerMethodGenerator(SurfaceRules.Bandlands.class, MethodGenerators::bandlands);
        registerMethodGenerator(SurfaceRules.BlockRuleSource.class, MethodGenerators::block);
        registerMethodGenerator(SurfaceRules.TestRuleSource.class, MethodGenerators::test);
        registerMethodGenerator(SurfaceRules.SequenceRuleSource.class, MethodGenerators::sequence);

        //Constants
        registerConstantGenerator(PackedIntArray.class, ConstantGenerators::generatePackedIntArray);
        registerConstantGenerator(ImprovedNoise[].class, ConstantGenerators::generateNoiseArray);
        registerConstantGenerator(ImprovedNoise.class, ConstantGenerators::generateNoise);
        registerConstantGenerator(int[].class, ConstantGenerators::generateIntArray);
        registerConstantGenerator(double[].class, ConstantGenerators::generateDoubleArray);
        registerConstantGenerator(float[].class, ConstantGenerators::generateFloatArray);
    }

    public static record CompileResult(String code, Map<Object, AstatineCallable> functions) {
        public AstatineCallable get(Object key) {
            return functions.get(key);
        }

        public String call(Object key, String... args) {
            AstatineCallable callable = functions.get(key);
            return callable.call(args);
        }
    }
}
