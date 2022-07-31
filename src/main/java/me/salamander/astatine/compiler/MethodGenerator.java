package me.salamander.astatine.compiler;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface MethodGenerator<T> extends BiFunction<CompileContext, T, AstatineComponent> {
    @SuppressWarnings("unchecked")
    default AstatineComponent applyUnsafe(CompileContext context, Object o) {
        return apply(context, (T) o);
    }
}
