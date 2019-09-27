package net.coderbot.patchwork.commandline.types;

import net.coderbot.patchwork.commandline.CommandlineException;
import net.coderbot.patchwork.commandline.CommandlineParseException;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public class IntegerTypeMapper extends BasicTypeMapper<Integer> {
    private static final List<Class<?>> SUPPORTED_TYPES = Arrays.asList(Integer.class, int.class, Integer.TYPE);

    public IntegerTypeMapper(Object target, Field f) throws CommandlineException {
        super(target, f);
        if(!SUPPORTED_TYPES.contains(f.getType())) {
            throw new CommandlineException("Tried to apply mapper for type Integer to field of type " + f.getType().getName());
        }
    }

    @Override
    public void apply(String value) throws CommandlineException {
        try {
            set(Integer.parseInt(value));
        } catch(NumberFormatException e) {
            throw new CommandlineParseException("Failed to parser " + value + " as integer", e);
        }
    }

    @Override
    public boolean acceptsValue() {
        return true;
    }
}
