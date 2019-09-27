package net.coderbot.patchwork.commandline.types;

import net.coderbot.patchwork.commandline.CommandlineException;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public class BooleanTypeMapper extends BasicTypeMapper<Boolean> {
    private static final List<Class<?>> SUPPORTED_TYPES = Arrays.asList(Boolean.class, boolean.class, Boolean.TYPE);

    public BooleanTypeMapper(Object target, Field f) throws CommandlineException {
        super(target, f);
        if(!SUPPORTED_TYPES.contains(f.getType())) {
            throw new CommandlineException("Tried to apply mapper for type Boolean to field of type " + f.getType().getName());
        }
    }

    @Override
    public void apply(String value) throws CommandlineException {
        set(true);
    }

    @Override
    public boolean acceptsValue() {
        return false;
    }
}
