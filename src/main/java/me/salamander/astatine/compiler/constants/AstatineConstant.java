package me.salamander.astatine.compiler.constants;

import me.salamander.astatine.compiler.AstatineComponent;
import me.salamander.astatine.compiler.CompileContext;

import java.util.Set;

public abstract class AstatineConstant implements AstatineComponent {
    private final Set<AstatineComponent> dependsOn;
    private final CompileContext context;

    public AstatineConstant(CompileContext context, Set<AstatineComponent> dependsOn) {
        this.dependsOn = dependsOn;
        this.context = context;
    }

    @Override
    public String call(String... args) {
        if (args.length != 0) {
            throw new IllegalArgumentException("AstatineConstant does not take any arguments");
        }

        return context.getConstantName(this);
    }

    @Override
    public boolean needsPreDeclaration() {
        return true;
    }


    @Override
    public Set<AstatineComponent> dependsOn() {
        return dependsOn;
    }
}
