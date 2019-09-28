package net.coderbot.patchwork.commandline.types;

import net.coderbot.patchwork.commandline.CommandlineException;
import net.coderbot.patchwork.commandline.CommandlineParseException;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * Type mapper for converting Strings to integer values
 *
 * @see BasicTypeMapper BasicTypeMapper for the base implementation
 */
public class IntegerTypeMapper extends BasicTypeMapper<Integer> {
    // Keep a list of supported types around
    // It is possible that int.class is the same as Integer.TYPE, but for maximum compatibility we keep both tracked
    private static final List<Class<?>> SUPPORTED_TYPES = Arrays.asList(Integer.class, int.class, Integer.TYPE);

    public IntegerTypeMapper(Object target, Field f) throws CommandlineException {
        super(target, f);
        if(!SUPPORTED_TYPES.contains(f.getType())) {
            throw new CommandlineException("Tried to apply mapper for type Integer to field of type " + f.getType().getName());
        }
    }

    /**
     * Sets the underlying field to the value parsed as an {@code int}.
     *
     * @param value The value to parse
     * @throws CommandlineParseException If an error occurs parsing the {@code String} into an {@code int} using
     *                                   {@link Integer#parseInt(String)}
     * @throws CommandlineException      If an error occurs setting the field
     */
    @Override
    public void apply(String value) throws CommandlineException {
        try {
            set(Integer.parseInt(value));
        } catch(NumberFormatException e) {
            throw new CommandlineParseException("Failed to parser " + value + " as integer", e);
        }
    }

    /**
     * Always true since we require an {@code String} to construct the {@code int} from
     *
     * @return Always true
     */
    @Override
    public boolean acceptsValue() {
        return true;
    }
}
