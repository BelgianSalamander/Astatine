package me.salamander.astatine.compiler.constants;

import me.salamander.astatine.compiler.AstatineComponent;
import me.salamander.astatine.compiler.CompileContext;

import javax.annotation.Nullable;
import java.util.Set;

public class AstatineGlobal implements AstatineComponent {
    private final String typeName;
    private final int[] arraySizes;
    private final @Nullable String defaultValue;
    private final CompileContext context;

    public AstatineGlobal(CompileContext context, String typeName, @Nullable String defaultValue, int... arraySizes) {
        this.typeName = typeName;
        this.arraySizes = arraySizes;
        this.defaultValue = defaultValue;
        this.context = context;
    }


    @Override
    public String call(String... args) {
        if (args.length <= arraySizes.length) {
            StringBuilder sb = new StringBuilder();

            sb.append(context.getConstantName(this));

            for (int i = 0; i < arraySizes.length; i++) {
                sb.append("[");
                sb.append(args[i]);
                sb.append("]");
            }

            return sb.toString();
        }

        throw new IllegalArgumentException("Too many arguments");
    }

    @Override
    public boolean needsPreDeclaration() {
        return true;
    }

    @Override
    public String declare() {
        StringBuilder sb = new StringBuilder();

        sb.append(typeName).append(" ");
        sb.append(context.getConstantName(this));

        for (int arraySize : arraySizes) {
            sb.append("[");
            if (arraySize != -1) {
                sb.append(arraySize);
            }
            sb.append("]");
        }

        if (defaultValue != null) {
            sb.append(" = ").append(defaultValue);
        }

        sb.append(";\n");

        return sb.toString();
    }

    @Override
    public Set<AstatineComponent> dependsOn() {
        return Set.of();
    }
}
