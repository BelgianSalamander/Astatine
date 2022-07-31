package me.salamander.astatine.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Format {
    private final List<StringPiece> pieces;

    public Format(String format) {
        this.pieces = Format.parse(format);
    }

    public String generate(Function<String, String> lookup) {
        StringBuilder sb = new StringBuilder();

        for (StringPiece piece : pieces) {
            sb.append(piece.getString(lookup));
        }

        return sb.toString();
    }

    public String generate(String... args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("Odd number of arguments");
        }

        Map<String, String> lookup = new HashMap<>();

        for (int i = 0; i < args.length; i += 2) {
            lookup.put(args[i], args[i + 1]);
        }

        return generate(lookup::get);
    }

    public static Format ofRes(String name) {
        try {
            return new Format(new String(Format.class.getResourceAsStream(name).readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<StringPiece> parse(String format) {
        StringBuilder accum = new StringBuilder();
        List<StringPiece> pieces = new ArrayList<>();

        int curr = 0;
        while (curr < format.length()) {
            char c = format.charAt(curr);

            if (c == '$') {
                if (curr + 1 < format.length() && format.charAt(curr + 1) == '{') {
                    if (accum.length() > 0) {
                        pieces.add(StringPiece.of(accum.toString()));
                        accum.setLength(0);
                    }

                    curr += 2;

                    int end = format.indexOf('}', curr);

                    if (end == -1) {
                        throw new IllegalArgumentException("Unclosed format string");
                    }

                    pieces.add(StringPiece.lookup(format.substring(curr, end)));

                    curr = end + 1;
                }
            } else {
                accum.append(c);
                curr++;
            }
        }

        if (accum.length() > 0) {
            pieces.add(StringPiece.of(accum.toString()));
        }

        return pieces;
    }

    private interface StringPiece {
        String getString(Function<String, String> lookup);

        static StringPiece of(String str) {
            return lookup -> str;
        }

        static StringPiece lookup(String str) {
            return lookup -> {
                String value = lookup.apply(str);

                if (value == null) {
                    throw new IllegalArgumentException("No value for key: " + str);
                }

                return value;
            };
        }
    }
}
