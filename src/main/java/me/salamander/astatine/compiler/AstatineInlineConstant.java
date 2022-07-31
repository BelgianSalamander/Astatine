package me.salamander.astatine.compiler;

import java.util.Set;

public class AstatineInlineConstant implements AstatineComponent {
    private final String value;
    private final Set<AstatineComponent> dependsOn;

    public AstatineInlineConstant(String value) {
        this.value = value;
        this.dependsOn = Set.of();
    }

    public AstatineInlineConstant(String value, Set<AstatineComponent> dependsOn) {
        this.value = value;
        this.dependsOn = dependsOn;
    }

    @Override
    public String call(String... args) {
        return value;
    }

    @Override
    public boolean needsPreDeclaration() {
        return false;
    }

    @Override
    public String declare() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<AstatineComponent> dependsOn() {
        return dependsOn;
    }
}
