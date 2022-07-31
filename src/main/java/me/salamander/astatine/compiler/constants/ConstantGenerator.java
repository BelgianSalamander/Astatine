package me.salamander.astatine.compiler.constants;

import me.salamander.astatine.compiler.AstatineComponent;
import me.salamander.astatine.compiler.CompileContext;

import java.util.function.BiFunction;

public interface ConstantGenerator<T> extends BiFunction<CompileContext, T, AstatineComponent> {
    @SuppressWarnings("unchecked")
    default AstatineComponent applyUnsafe(CompileContext context, Object value) {
        return apply(context, (T) value);
    }
}
