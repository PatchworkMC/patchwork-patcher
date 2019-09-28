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
import java.util.stream.Collectors;

/**
 * Class for processing the commandline, parsing it and then converting it into an Object type requested by the user.
 *
 * @param <T> Type the {@link CommandlineParser#parse()} method uses for parsing and evaluating the commandline
 */
public class CommandlineParser<T> {
    /**
     * Lookup used for faster reflective like access.
     * <b>THIS IS ONLY PUBLIC FOR INTERNAL USAGE</b>
     */
    public static final MethodHandles.Lookup METHOD_LOOKUP = MethodHandles.lookup();

    // Map of built-in type mappers
    private static final Map<Class<?>, TypeMapper.Constructor> TYPE_MAPPERS = new HashMap<>();
    static {
        TYPE_MAPPERS.put(Boolean.class, BooleanTypeMapper::new);
        TYPE_MAPPERS.put(boolean.class, BooleanTypeMapper::new);

        TYPE_MAPPERS.put(String.class, StringTypeMapper::new);

        TYPE_MAPPERS.put(Integer.class, IntegerTypeMapper::new);
        TYPE_MAPPERS.put(int.class, IntegerTypeMapper::new);
    }

    // To minimize the cost of looking up constructors of user supplied type mappers every time cache them
    private static final Map<Class<?>, MethodHandle> CACHED_TYPE_MAPPER_CONSTRUCTORS = new HashMap<>();

    // Target object to translate the parsed commandline to
    private final T argumentHolder;
    private final String[] arguments;

    // All field accessors needed
    private final List<FieldAccessor> parameterAccessors;
    private final List<FieldAccessor> flagAccessors;
    private FieldAccessor remainingAccessor;

    /*
     * State storage of the commandline parser, the following rules apply:
     * - If parseSucceeded == false && parseError == null => parse() has not been called
     * - If parseSucceeded == true => parse() has been called and the commandline
     *                                could be fully parsed and parseError = null
     * - If parseSucceeded == false && parseError != null => parse() has been called and the given commandline was bad
     * - parseSucceeded should never be true if parseError != null
     */
    private boolean parseSucceeded;
    private String parseError;

    /**
     * Creates a new CommandlineParser targeting an argument holder and for parsing the commandline given.
     *
      * @param argumentHolder The target object to apply the parsed commandline to
     * @param arguments The commandline itself, already split into sensible parts
     *                  (usually the parameter of the main method)
     */
    public CommandlineParser(T argumentHolder, String[] arguments) {
        this.argumentHolder = argumentHolder;
        this.arguments = arguments;

        this.parameterAccessors = new ArrayList<>();
        this.flagAccessors = new ArrayList<>();
    }

    /**
     * Parses the commandline and returns the object the options have been applied to.
     * An exception is only thrown if a fatal error occurred while parsing. This means, if reflective access
     * fails or any other unexpected exception happens.
     *
     * This method does not throw if parsing itself fails. To check for such use
     * {@link CommandlineParser#parseSucceeded()} and {@link CommandlineParser#getParseError()}. To generate a full help
     * message use {@link CommandlineParser#generateHelpMessage(String, String, String, String, boolean)} which will
     * also include the reason parsing failed.
     *
     * @return The object the options have been applied to, which is the object passed in the constructor
     * @throws CommandlineException If a fatal error occurs while parsing
     */
    public T parse() throws CommandlineException {
        buildAccessors();
        processArgs();
        return argumentHolder;
    }

    private void buildAccessors() throws CommandlineException {
        Class<?> targetClass = argumentHolder.getClass();

        // We don't know in which order Reflection gives the fields back to use
        Map<Integer, FieldAccessor> unorderedParameters = new HashMap<>();

        for(Field field : targetClass.getDeclaredFields()) {
            // Static fields are not supported since we are operating on an instance
            if(Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            Parameter parameterAnnotation;
            Flag flagAnnotation;

            if((parameterAnnotation = field.getAnnotation(Parameter.class)) != null) {
                if(unorderedParameters.containsKey(parameterAnnotation.position())) {
                    throw new CommandlineException("Duplicated parameter position " + parameterAnnotation.position());
                }

                // -1 means collect remaining
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
                } else if(parameterAnnotation.position() < -1) {
                    throw new IllegalArgumentException("@Parameter{position} cannot be less than -1");
                }

                // Collect all parameters into our HashMap and associate them with a FieldAccessor
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
                // Collect all flags into our List and associate them with a FieldAccessor
                flagAccessors.add(new FieldAccessor(
                        getTypeMapper(flagAnnotation.typeMapper(), field),
                        flagAnnotation.names(),
                        flagAnnotation.description(),
                        false
                ));
            }
        }

        // Iterate over a sorted List of entries
        for(Map.Entry<Integer, FieldAccessor> entry : unorderedParameters
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList())
        ) {
            int index = parameterAccessors.size();
            if(entry.getKey() != index) {
                // This can only happen if the user left out a position for parameters
                // for example, if position = 1 and position = 3 is specified, we can't know what 2 would be.
                // For the sake of simplicity don't allow this.
                throw new CommandlineException(
                        "Parameter index error, position gap? Expected " + entry.getKey() + " for parameter " +
                                Arrays.toString(entry.getValue().names()) + " but got " + index);
            }

            if(index != 0 && !parameterAccessors.get(index - 1).required() && entry.getValue().required()) {
                // This might happen if a parameter which is required is specified to follow a parameter which is
                // optional. Since we can't know which parameter to fit the value in, we don't allow this.
                throw new CommandlineException("Parameter " + Arrays.toString(entry.getValue().names()) + " which is " +
                        "required follows optional parameter " +
                        Arrays.toString(parameterAccessors.get(index - 1).names()));
            } else if(!entry.getValue().required() && remainingAccessor != null) {
                // Optional parameters with a remaining collector are also not allowed for the same reason as above
                throw new CommandlineException("Optional parameter and remaining accessor found");
            }

            parameterAccessors.add(entry.getValue());
        }
    }

    // This creates a type mapper for specified field.
    // annotatedClass indicates if the user has overridden the mapper type
    private TypeMapper getTypeMapper(Class<? extends TypeMapper> annotatedClass, Field f) throws CommandlineException {
        if(annotatedClass != TypeMapper.NullTypeMapper.class) {
            // The user has specified its own mapper type
            try {
                if(CACHED_TYPE_MAPPER_CONSTRUCTORS.containsKey(annotatedClass)) {
                    // We already have a constructor handle for that type, avoid the reflective lookup
                    return (TypeMapper) CACHED_TYPE_MAPPER_CONSTRUCTORS.get(annotatedClass).invoke(argumentHolder, f);
                }

                // Search for a constructor matching
                MethodHandle constructor =
                        METHOD_LOOKUP.findConstructor(annotatedClass, MethodType.methodType(void.class, Object.class, Field.class));

                // Cache it for later use
                CACHED_TYPE_MAPPER_CONSTRUCTORS.put(annotatedClass, constructor);

                // Construct the mapper using the found method handle
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
                // The user has not overridden the class to use and there is no default
                throw new CommandlineException("Failed to find type mapper for type " + annotatedType.getName());
            }

            try {
                return TYPE_MAPPERS.get(annotatedType).create(argumentHolder, f);
            } catch(Throwable t) {
                if(t instanceof CommandlineException) {
                    throw ((CommandlineException) t);
                }

                // This usually indicates a bug
                throw new CommandlineException("Error while creating default type mapper for type " +
                        annotatedType.getName(), t);
            }
        }
    }

    // This is the real magic which iterates over the supplied string array, parses and finally applies it
    private void processArgs() throws CommandlineException {
        // Keep track of which parameter we accessed last since flags might be offsetting the position relative
        // to the index of the argument or a parameter might accept multiple values
        int parameterIndex = 0;

        for(int i = 0; i < arguments.length; i++) {
            final String currentArg = arguments[i];

            String name;
            String value = null;

            // Check if we deal with a flag
            if(currentArg.startsWith("--") || currentArg.startsWith("-")) {
                if(currentArg.startsWith("--")) {
                    // A long flag supports passing a value by using "="
                    if(currentArg.contains("=")) {
                        String[] keyValue = currentArg.substring(2).split("=", 2);
                        name = keyValue[0];
                        value = keyValue[1];
                    } else {
                        name = currentArg.substring(2);
                    }
                } else {
                    /*
                     * Remove the leading "-" and check if the flag is still longer than 1 char
                     * because short flags support passing the value by appending it directly.
                     *
                     * For example "-fhello" would be {name = "f", value = "hello"}
                     */
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
                        // Search the flag by comparing the name
                        if(accessorName.equals(name)) {
                            // This does a bunch of checks to make a sure a flag requiring a value gets one
                            // and a flag not requiring one does not get one passed
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
                // Since we do not have the "-" prefix we deal with a parameter
                if(i >= parameterAccessors.size()) {
                    // Check wether we collect remaining arguments and error out else
                    if(remainingAccessor != null && !remainingAccessor.filled()) {
                        try {
                            remainingAccessor.set(currentArg);
                        } catch(CommandlineParseException e) {
                            // The user supplied a value not applicable to the parameter
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
                        // The user supplied a value not applicable to the parameter
                        parseError = e.getMessage();
                        return;
                    }

                    if(accessor.filled()) {
                        // If the parameter is filled now, move on to the next one
                        parameterIndex++;
                    }
                }
            }
        }

        // Check for missing arguments before completing
        if(parameterIndex < parameterAccessors.size() && parameterAccessors.get(parameterIndex).required()) {
            FieldAccessor accessor = parameterAccessors.get(parameterIndex);
            parseError = "Missing required argument " + accessor.names()[0];
            return;
        }

        parseSucceeded = true;
    }

    /**
     * Checks wether the parse succeeded by now. Must be called after {@link CommandlineParser#parse()} has been called.
     *
     * @return {@code true} if parsing succeeded, {@code false} otherwise
     * @throws IllegalStateException if called before {@link CommandlineParser#parse()} has been called.
     */
    public boolean parseSucceeded() {
        if(parseError == null && !parseSucceeded) {
            throw new IllegalStateException("Called parseSucceeded() before calling parse()");
        } else {
            return parseSucceeded;
        }
    }

    /**
     * Retrieves a possible error description if an error occurred. Must be called after
     * {@link CommandlineParser#parse()} has been called and only if {@link CommandlineParser#parseSucceeded()} returned
     * {@code false}.
     *
     * @return The parse error which occurred
     */
    public String getParseError() {
        if(parseSucceeded) {
            throw new IllegalStateException("Called getParseError() after parsing succeeded");
        } else if(parseError == null) {
            throw new IllegalStateException("Called getParseError() before calling parse()");
        } else {
            return parseError;
        }
    }

    /**
     * Generates a nice help message which can be displayed to the user, optionally including colors.
     *
     * @param executableName The executable name, a bit hard to get right with Java, see
     *                       https://stackoverflow.com/questions/11158235/get-name-of-executable-jar-from-within-main-method
     *                       for more details
     * @param programName The name of the program displayed to the user
     * @param intro A short description of the program, or null if it should be omitted
     * @param outro A short text following the commandline description, put copyright and such here, null if it should
     *              be omitted
     * @param colors Wether to use colors in the output string
     * @return A nice help message based on all parameters and flags
     */
    public String generateHelpMessage(String executableName, String programName,
                                      String intro, String outro, boolean colors) {
        StringBuilder buffer = new StringBuilder();
        // Generate a simple header
        buffer.append("Usage: ").append(generateSignature(executableName)).append("\n");
        buffer.append(color(programName, Ansi.Color.CYAN, false, colors)).append("\n\n");
        if(intro != null) {
            buffer.append(intro).append("\n");
        }

        if(!parameterAccessors.isEmpty()) {
            // Check how much space we require for the names of parameters so we can align their descriptions
            int longestParameterNameLength = 0;
            for(FieldAccessor accessor : parameterAccessors) {
                String name = accessor.names()[0];
                if(name.length() > longestParameterNameLength && name.length() < 40) {
                    longestParameterNameLength = name.length();
                }
            }

            // Form a header
            buffer.append(color("\nParameters", Ansi.Color.MAGENTA, true, colors)).append(":\n");
            for(FieldAccessor accessor : parameterAccessors) {
                int lineLength = accessor.names()[0].length() + 4;
                // Apply the required chars:
                // - "<" & ">" for required parameters
                // - "[" & "]" for optional ones
                buffer.append("  ").append(color((accessor.required() ? "<" : "[") +
                                accessor.names()[0] + (accessor.required() ? ">" : "]"),
                        Ansi.Color.GREEN, true, colors)).append("  ");

                appendDescription(buffer, longestParameterNameLength, accessor, lineLength);
            }
        }

        if(!flagAccessors.isEmpty()) {
            // Same as above for parameters, its just a bit more complex here since we have separating commas
            int longestFlagNamesLength = 0;
            for(FieldAccessor accessor : flagAccessors) {
                int flagNamesLength = 0;
                String[] names = accessor.names();

                for(int i = 0; i < names.length; i++) {
                    // Iterate every name
                    String name = names[i];
                    if(i + 1 < names.length) {
                        // If it is not the last name, " ," will be appended
                        flagNamesLength += 2;
                    }
                    // For a short flag we require just 2 chars, else "--" plus the name
                    flagNamesLength += name.length() < 2 ? 2 : name.length() + 2;
                }

                // If a flag is too long, don't make it the longest one to avoid weird padding,
                // the generator will later deal with this
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

                // Construct a nice list of the flag names
                for(int i = 0; i < names.length; i++) {
                    String name = names[i];
                    if(name.length() < 2) {
                        // Short flag, append "-" + name
                        lineLength += 2;
                        flagBuffer.append(color("-" + name, Ansi.Color.YELLOW, true, colors));
                    } else {
                        // Long flag, append "--" + name
                        lineLength += 2 + name.length();
                        flagBuffer.append(color("--" + name, Ansi.Color.GREEN, true, colors));
                    }

                    // If its not the last flag name, append " ,"
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
            // If we had some parse error, inform the user about what exactly went wrong.
            // Apparently a lot of programs don't do it, and its really annoying!
            buffer.append("\n\n").append("Commandline error: ").append(color(parseError, Ansi.Color.RED, true, colors));
        }

        return buffer.toString().trim();
    }

    // This method generates an align description of what this flag or parameter does
    private void appendDescription(StringBuilder buffer, int longestNameLength, FieldAccessor accessor, int lineLength) {
        String[] descriptionParts = accessor.description().split("\n");

        for(int i = 0; i < descriptionParts.length; i++) {
            String part = descriptionParts[i].trim();

            if(i != 0) {
                // If its not the first line, simply pad it to match
                buffer.append(repeat(' ', longestNameLength + 6));
            } else if(lineLength >= 46) {
                // We have a too long name, jump on the next line and then proceed as if it wouldn't be the first line
                buffer.append("\n").append(repeat(' ', longestNameLength + 6));
            } else {
                // Calculate how much space we need to actually align the description right after the name
                buffer.append(repeat(' ', longestNameLength - (lineLength - 4)));
            }

            // And finally really append the line
            buffer.append(part).append("\n");
        }
    }

    /**
     * Generates a short usage of the file
     *
     * @param executableName The executable name, see
     *                       {@link CommandlineParser#generateHelpMessage(String, String, String, String, boolean)} for
     *                       further information
     * @return A short usage
     */
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

    // Helper for conditionally coloring a string with Jansi
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

    // Helper for repeating a char n times
    private String repeat(char c, int n) {
        return new String(new char[n]).replace('\0', c);
    }
}
