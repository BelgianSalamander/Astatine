package me.salamander.astatine.util;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class Utils {
    public static <T> List<T> topologicalSort(Collection<T> data, Function<T, List<T>> getNexts) {
        List<T> reversed = new ArrayList<>();

        class Sorter {
            final Object2BooleanMap<T> visited = new Object2BooleanOpenHashMap<>();

            void visit(T node) {
                if (visited.containsKey(node)) return;
                visited.put(node, true);
                for (T next : getNexts.apply(node)) {
                    visit(next);
                }
                reversed.add(node);
            }
        }

        Sorter sorter = new Sorter();

        for (T node : data) {
            sorter.visit(node);
        }

        //reverse
        List<T> result = new ArrayList<>();

        for (int i = reversed.size() - 1; i >= 0; i--) {
            result.add(reversed.get(i));
        }

        return result;
    }

    public static int[] pack(int bits, List<Number> data) {
        int[] result = new int[(data.size() + bits - 1) / bits];
        for (int i = 0; i < data.size(); i++) {
            result[i / bits] |= data.get(i).intValue() << (i % bits);
        }
        return result;
    }

    public static int[] pack(int bits, Number... data) {
        return pack(bits, List.of(data));
    }

    public static Integer[] boxArray(int[] data) {
        Integer[] result = new Integer[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = data[i];
        }
        return result;
    }

    public static Byte[] boxArray(byte[] data) {
        Byte[] result = new Byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = data[i];
        }
        return result;
    }
}
