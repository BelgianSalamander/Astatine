package me.salamander.astatine.compiler.constants;

import me.salamander.astatine.compiler.AstatineComponent;
import me.salamander.astatine.compiler.CompileContext;

public record PackedIntArray(int[] values, int bitsPer) {
    public static PackedIntArray ofBool(boolean[] values) {
        int[] packed = new int[values.length];
        int bitsPer = 1;

        for (int i = 0; i < values.length; i++) {
            packed[i] = values[i] ? 1 : 0;
        }

        return new PackedIntArray(packed, bitsPer);
    }

    public static Object ofBytes(byte[] biomeTemperatureType) {
        int[] packed = new int[biomeTemperatureType.length];
        int bitsPer = 8;

        for (int i = 0; i < biomeTemperatureType.length; i++) {
            packed[i] = biomeTemperatureType[i];
        }

        return new PackedIntArray(packed, bitsPer);
    }
}
