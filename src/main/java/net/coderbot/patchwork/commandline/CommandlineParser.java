package net.coderbot.patchwork.commandline;

import net.coderbot.patchwork.commandline.types.BooleanTypeMapper;
import net.coderbot.patchwork.commandline.types.IntegerTypeMapper;
import net.coderbot.patchwork.commandline.types.StringTypeMapper;
import org.fusesource.jansi.Ansi;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CommandlineParser<T> {
    public static final MethodHandles.Lookup METHOD_LOOKUP = MethodHandles.lookup();

    private static final Map<Class<?>, TypeMapper.Constructor> TYPE_MAPPERS = new HashMap<>();
    static {
        TYPE_MAPPERS.put(Boolean.class, BooleanTypeMapper::new);
        TYPE_MAPPERS.put(boolean.class, BooleanTypeMapper::new);

        TYPE_MAPPERS.put(String.class, StringTypeMapper::new);

        TYPE_MAPPERS.put(Integer.class, IntegerTypeMapper::new);
        TYPE_MAPPERS.put(int.class, IntegerTypeMapper::new);
    }

    private static final Map<Class<?>, MethodHandle> CACHED_TYPE_MAPPER_CONSTRUCTORS = new HashMap<>();

    private final T argumentHolder;
    private final String[] arguments;

    private final List<FieldAccessor> parameterAccessors;
    private final List<FieldAccessor> flagAccessors;
    private FieldAccessor remainingAccessor;

    private boolean parseSucceeded;
    private String parseError;

    public CommandlineParser(Supplier<T> argumentHolderSupplier, String[] arguments) {
        this.argumentHolder = argumentHolderSupplier.get();
        this.arguments = arguments;

        this.parameterAccessors = new ArrayList<>();
        this.flagAccessors = new ArrayList<>();
    }

    public T parse() throws CommandlineException {
        buildAccessors();
        processArgs();
        return argumentHolder;
    }

    private void buildAccessors() throws CommandlineException {
        Class<?> targetClass = argumentHolder.getClass();

        Map<Integer, FieldAccessor> unorderedParameters = new HashMap<>();

        for(Field field : targetClass.getDeclaredFields()) {
            if(Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            Parameter parameterAnnotation;
            Flag flagAnnotation;

            if((parameterAnnotation = field.getAnnotation(Parameter.class)) != null) {
                if(unorderedParameters.containsKey(parameterAnnotation.position())) {
                    throw new CommandlineException("Duplicated parameter position " + parameterAnnotation.position());
                }

                if(parameterAnnotation.position() == -1) {
                    if(remainingAccessor != null) {
                        throw new CommandlineException("Duplicated remaining accessor, already have " +
                                remainingAccessor.names()[0] + " and tried to add " + parameterAnnotation.name());
                    } else {
                        remainingAccessor = new FieldAccessor(
                                getTypeMapper(parameterAnnotation.typeMapper(), field),
                                new String[]{parameterAnnotation.name()},
                                parameterAnnotation.description(),
                                parameterAnnotation.required()
                        );
                    }
                }

                unorderedParameters.put(
                        parameterAnnotation.position(),
                        new FieldAccessor(
                                getTypeMapper(parameterAnnotation.typeMapper(), field),
                                new String[]{parameterAnnotation.name()},
                                parameterAnnotation.description(),
                                parameterAnnotation.required()
                        )
                );
            } else if((flagAnnotation = field.getAnnotation(Flag.class)) != null) {
                flagAccessors.add(new FieldAccessor(
                        getTypeMapper(flagAnnotation.typeMapper(), field),
                        flagAnnotation.names(),
                        flagAnnotation.description(),
                        false
                ));
            }
        }

        for(Map.Entry<Integer, FieldAccessor> entry : unorderedParameters
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList())
        ) {
            int index = parameterAccessors.size();
            if(entry.getKey() != index) {
                throw new CommandlineException(
                        "Parameter index error, position gap? Expected " + entry.getKey() + " for parameter " +
                                Arrays.toString(entry.getValue().names()) + " but got " + index);
            }

            if(index != 0 && !parameterAccessors.get(index - 1).required() && entry.getValue().required()) {
                throw new CommandlineException("Parameter " + Arrays.toString(entry.getValue().names()) + " which is " +
                        "required follows optional parameter " +
                        Arrays.toString(parameterAccessors.get(index - 1).names()));
            } else if(!entry.getValue().required() && remainingAccessor != null) {
                throw new CommandlineException("Optional parameter and remaining accessor found");
            }

            parameterAccessors.add(entry.getValue());
        }
    }

    private TypeMapper getTypeMapper(Class<? extends TypeMapper> annotatedClass, Field f) throws CommandlineException {
        if(annotatedClass != TypeMapper.NullTypeMapper.class) {
            try {
                if(CACHED_TYPE_MAPPER_CONSTRUCTORS.containsKey(annotatedClass)) {
                    return (TypeMapper) CACHED_TYPE_MAPPER_CONSTRUCTORS.get(annotatedClass).invokeExact(f);
                }

                MethodHandle constructor =
                        METHOD_LOOKUP.findConstructor(annotatedClass, MethodType.methodType(void.class, Object.class, Field.class));

                CACHED_TYPE_MAPPER_CONSTRUCTORS.put(annotatedClass, constructor);

                return (TypeMapper) constructor.invoke(argumentHolder, f);
            } catch(NoSuchMethodException e) {
                throw new CommandlineException("Creating custom type mapper " + annotatedClass.getName() +
                        " failed due to missing a constructor accepting a Field as the only parameter", e);
            } catch(IllegalAccessException e) {
                throw new CommandlineException("Creating custom type mapper " + annotatedClass.getName() +
                        " failed due to bad access rights (invisible constructor?)", e);
            } catch(Throwable t) {
                throw new CommandlineException("Creating custom type mapper " + annotatedClass.getName() +
                        " failed due to error in constructor", t);
            }

        } else {
            Class<?> annotatedType = f.getType();

            if(!TYPE_MAPPERS.containsKey(annotatedType)) {
                throw new CommandlineException("Failed to find type mapper for type " + annotatedType.getName());
            }

            try {
                return TYPE_MAPPERS.get(annotatedType).create(argumentHolder, f);
            } catch(Throwable t) {
                throw new CommandlineException("Error while creating default type mapper for type " +
                        annotatedType.getName(), t);
            }
        }
    }

    private void processArgs() throws CommandlineException {
        int parameterIndex = 0;

        for(int i = 0; i < arguments.length; i++) {
            final String currentArg = arguments[i];

            String name;
            String value = null;

            if(currentArg.startsWith("--") || currentArg.startsWith("-")) {
                if(currentArg.startsWith("--")) {
                    if(currentArg.contains("=")) {
                        String[] keyValue = currentArg.substring(2).split("=", 2);
                        name = keyValue[0];
                        value = keyValue[1];
                    } else {
                        name = currentArg.substring(2);
                    }
                } else {
                    String trimmed = currentArg.substring(1);
                    if(trimmed.length() > 1) {
                        name = trimmed.substring(0, 1);
                        value = trimmed.substring(1);
                    } else {
                        name = trimmed;
                    }
                }

                boolean found = false;
                for(FieldAccessor accessor : flagAccessors) {
                    for(String accessorName : accessor.names()) {
                        if(accessorName.equals(name)) {
                            if(accessor.acceptsValue()) {
                                if(value == null) {
                                    if(++i >= arguments.length) {
                                        parseError = "Missing value for flag " + currentArg;
                                        return;
                                    }
                                    value = arguments[i];
                                }

                                if(accessor.filled()) {
                                    parseError = "Flag " + currentArg + " used too many times";
                                    return;
                                }
                                accessor.set(value);
                            } else {
                                if(value != null) {
                                    parseError = "Unexpected value '" + value + "' for flag " + currentArg;
                                    return;
                                } else if(accessor.filled()) {
                                    parseError = "Flag " + currentArg + " used too many times";
                                    return;
                                }
                                accessor.set(null);
                            }

                            found = true;
                            break;
                        }
                    }

                    if(found) {
                        break;
                    }
                }

                if(!found) {
                    parseError = "Unknown flag " + currentArg;
                    return;
                }
            } else {
                if(i >= parameterAccessors.size()) {
                    if(remainingAccessor != null && !remainingAccessor.filled()) {
                        try {
                            remainingAccessor.set(currentArg);
                        } catch(CommandlineParseException e) {
                            parseError = e.getMessage();
                            return;
                        }
                    } else {
                        parseError = "Too many arguments";
                        return;
                    }
                } else {
                    FieldAccessor accessor = parameterAccessors.get(parameterIndex);
                    try {
                        accessor.set(currentArg);
                    } catch(CommandlineParseException e) {
                        parseError = e.getMessage();
                        return;
                    }

                    if(accessor.filled()) {
                        parameterIndex++;
                    }
                }
            }
        }

        if(parameterIndex < parameterAccessors.size() && parameterAccessors.get(parameterIndex).required()) {
            FieldAccessor accessor = parameterAccessors.get(parameterIndex);
            parseError = "Missing required argument " + accessor.names()[0];
            return;
        }

        parseSucceeded = true;
    }

    public boolean parseSucceeded() {
        if(parseError == null && !parseSucceeded) {
            throw new IllegalStateException("Called parseSucceeded() before calling parse()");
        } else {
            return parseSucceeded;
        }
    }

    public String getParseError() {
        if(parseSucceeded) {
            throw new IllegalStateException("Called getParseError() after parsing succeeded");
        } else if(parseError == null) {
            throw new IllegalStateException("Called getParseError() before calling parse()");
        } else {
            return parseError;
        }
    }

    public String generateHelpMessage(String executableName, String programName,
                                      String intro, String outro, boolean colors) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Usage: ").append(generateSignature(executableName)).append("\n");
        buffer.append(color(programName, Ansi.Color.CYAN, false, colors)).append("\n\n");
        if(intro != null) {
            buffer.append(intro).append("\n");
        }

        if(!parameterAccessors.isEmpty()) {
            int longestParameterNameLength = 0;
            for(FieldAccessor accessor : parameterAccessors) {
                String name = accessor.names()[0];
                if(name.length() > longestParameterNameLength && name.length() < 40) {
                    longestParameterNameLength = name.length();
                }
            }

            buffer.append(color("\nParameters", Ansi.Color.MAGENTA, true, colors)).append(":\n");
            for(FieldAccessor accessor : parameterAccessors) {
                int lineLength = accessor.names()[0].length() + 4;
                buffer.append("  ").append(color((accessor.required() ? "<" : "[") +
                                accessor.names()[0] + (accessor.required() ? ">" : "]"),
                        Ansi.Color.GREEN, true, colors)).append("  ");

                appendDescription(buffer, longestParameterNameLength, accessor, lineLength);
            }
        }

        if(!flagAccessors.isEmpty()) {
            int longestFlagNamesLength = 0;
            for(FieldAccessor accessor : flagAccessors) {
                int flagNamesLength = 0;
                String[] names = accessor.names();

                for(int i = 0; i < names.length; i++) {
                    String name = names[i];
                    if(i + 1 < names.length) {
                        flagNamesLength += 2;
                    }
                    flagNamesLength += name.length() < 2 ? 2 : name.length() + 2;
                }

                if(flagNamesLength > longestFlagNamesLength && flagNamesLength < 46) {
                    longestFlagNamesLength = flagNamesLength;
                }
            }

            buffer.append(color("\nAvailable flags", Ansi.Color.MAGENTA, true, colors)).append(":\n");
            for(FieldAccessor accessor : flagAccessors) {
                String[] names = accessor.names();
                StringBuilder flagBuffer = new StringBuilder();
                flagBuffer.append("  ");
                int lineLength = 2;

                for(int i = 0; i < names.length; i++) {
                    String name = names[i];
                    if(name.length() < 2) {
                        lineLength += 2;
                        flagBuffer.append(color("-" + name, Ansi.Color.YELLOW, true, colors));
                    } else {
                        lineLength += 2 + name.length();
                        flagBuffer.append(color("--" + name, Ansi.Color.GREEN, true, colors));
                    }

                    if(i + 1 < names.length) {
                        lineLength += 2;
                        flagBuffer.append(", ");
                    }
                }

                lineLength += 2;
                flagBuffer.append("  ");
                buffer.append(flagBuffer);

                appendDescription(buffer, longestFlagNamesLength, accessor, lineLength);
            }
        }

        if(outro != null) {
            buffer.append("\n").append(outro);
        }

        if(parseError != null) {
            buffer.append("\n\n").append("Commandline error: ").append(color(parseError, Ansi.Color.RED, true, colors));
        }

        return buffer.toString().trim();
    }

    private void appendDescription(StringBuilder buffer, int longestNameLength, FieldAccessor accessor, int lineLength) {
        String[] descriptionParts = accessor.description().split("\n");

        for(int i = 0; i < descriptionParts.length; i++) {
            String part = descriptionParts[i].trim();

            if(i != 0) {
                buffer.append(repeat(' ', longestNameLength + 6));
            } else if(lineLength >= 46) {
                buffer.append("\n").append(repeat(' ', longestNameLength + 6));
            } else {
                buffer.append(repeat(' ', longestNameLength - (lineLength - 4)));
            }

            buffer.append(part).append("\n");
        }
    }

    public String generateSignature(String executableName) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(executableName);

        if(!flagAccessors.isEmpty()) {
            buffer.append(" [FLAGS]...");
        }

        if(!parameterAccessors.isEmpty()) {
            for(FieldAccessor accessor : parameterAccessors) {
                buffer.append(accessor.required() ? " <" : " [").append(accessor.names()[0]).append(accessor.required() ? ">" : "]");
            }
        }

        return buffer.toString();
    }

    private String color(String str, Ansi.Color color, boolean bright, boolean colors) {
        if(colors) {
            if(bright) {
                return Ansi.ansi().fgBright(color).a(str).reset().toString();
            } else {
                return Ansi.ansi().fg(color).a(str).reset().toString();
            }
        }
        return str;
    }

    private String repeat(char c, int n) {
        return new String(new char[n]).replace('\0', c);
    }
}
