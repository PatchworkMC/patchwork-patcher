package net.coderbot.patchwork.commandline.types;

import net.coderbot.patchwork.commandline.CommandlineException;

import java.lang.reflect.Field;

public class StringTypeMapper extends BasicTypeMapper<String> {
    public StringTypeMapper(Object target, Field f) throws CommandlineException {
        super(target, f);
        if(f.getType() != String.class) {
            throw new CommandlineException("Tried to apply mapper for String to field of type " + f.getType().getName());
        }
    }

    @Override
    public void apply(String value) throws CommandlineException {
        set(value);
    }

    @Override
    public boolean acceptsValue() {
        return true;
    }
}
