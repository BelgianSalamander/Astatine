package me.salamander.astatine.compiler;

import org.jetbrains.annotations.Nullable;

import java.util.Set;

public abstract class AstatineMethod implements AstatineComponent {
    private static final String INDENT = "    ";
    private final @Nullable String comment;
    protected final CompileContext context;

    private int currIndent = 0;

    public AstatineMethod(CompileContext context) {
        this(context, null);
    }

    public AstatineMethod(CompileContext context, String comment) {
        this.context = context;
        this.comment = comment;
    }

    @Override
    public final String declare() {
        String[] argTypes = getArgTypes();
        Writer writer = new Writer();

        if (comment != null) {
            if (comment.contains("\n")) {
                writer.sb.append("/*\n");
                for (String line : comment.split("\n")) {
                    writer.sb.append(" * ").append(line).append("\n");
                }
                writer.sb.append(" */\n");
            } else {
                writer.sb.append("// ").append(comment).append("\n");
            }
        }

        writer.sb.append(getReturnType());
        writer.sb.append(" ").append(context.getMethodName(this)).append("(");

        writer.sb.append(") {\n");

        writer.indent++;

        generateBody(writer);

        writer.indent--;

        writer.sb.append("}\n");

        return writer.sb.toString();
    }

    @Override
    public final boolean needsPreDeclaration() {
        return true;
    }

    @Override
    public String call(String... args) {
        StringBuilder sb = new StringBuilder();

        sb.append(context.getMethodName(this)).append("(");

        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }

            sb.append(args[i]);
        }

        sb.append(")");

        return sb.toString();
    }

    protected String getArgName(int index) {
        return "__arg_" + index;
    }

    protected abstract void generateBody(Writer writer);

    protected abstract String getReturnType();

    protected abstract String[] getArgTypes();

    public static abstract class Condition extends AstatineMethod {
        public Condition(CompileContext context) {
            super(context);
        }

        public Condition(CompileContext context, String comment) {
            super(context, comment);
        }

        @Override
        protected String getReturnType() {
            return "bool";
        }

        @Override
        protected String[] getArgTypes() {
            return new String[0];
        }

        @Override
        public Set<AstatineComponent> dependsOn() {
            return Set.of();
        }
    }

    public static abstract class SurfaceRule extends AstatineMethod {
        public SurfaceRule(CompileContext context) {
            super(context, null);
        }

        public SurfaceRule(CompileContext context, String comment) {
            super(context, null);
        }

        @Override
        protected String getReturnType() {
            return "int"; //-1 represent null
        }

        @Override
        protected String[] getArgTypes() {
            return new String[0];
        }
    }

    protected static class Writer {
        private final StringBuilder sb = new StringBuilder();
        private int indent = 0;

        public void addLine(String line) {
            int numOpeningBraces = 0;
            int numClosingBraces = 0;

            for (char c : line.toCharArray()) {
                if (c == '{') {
                    numOpeningBraces++;
                } else if (c == '}') {
                    numClosingBraces++;
                }
            }

            indent -= numClosingBraces;

            sb.append(INDENT.repeat(indent));
            sb.append(line);
            sb.append("\n");

            indent += numOpeningBraces;
        }
    }
}
