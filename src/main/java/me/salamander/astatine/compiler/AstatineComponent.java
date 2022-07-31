package me.salamander.astatine.compiler;

import java.util.Set;

public interface AstatineComponent extends AstatineCallable {
    boolean needsPreDeclaration();

    String declare();

    Set<AstatineComponent> dependsOn();
}
