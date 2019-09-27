package net.coderbot.patchwork.commandline.types;

import net.coderbot.patchwork.commandline.CommandlineException;
import net.coderbot.patchwork.commandline.CommandlineParser;
import net.coderbot.patchwork.commandline.TypeMapper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Field;

public abstract class BasicTypeMapper<T> implements TypeMapper {
    private final MethodHandle setter;

    private boolean filled;

    protected BasicTypeMapper(Object target, Field f) throws CommandlineException {
        f.setAccessible(true);
        try {
            setter = CommandlineParser.METHOD_LOOKUP.unreflectSetter(f).bindTo(target);
        } catch(IllegalAccessException e) {
            throw new CommandlineException("Insufficient access rights to unreflect setter", e);
        }
    }

    protected void set(T value) throws CommandlineException {
        try {
            setter.invoke(value);
            filled = true;
        } catch(WrongMethodTypeException | ClassCastException e) {
            throw new CommandlineException("Tried to invoke setter with invalid value type", e);
        } catch(Throwable t) {
            throw new CommandlineException("Error occurred invoking setter", t);
        }
    }

    @Override
    public boolean filled() {
        return filled;
    }
}
