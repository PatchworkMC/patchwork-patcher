package net.coderbot.patchwork.commandline;

import java.lang.reflect.Field;

public interface TypeMapper {
    void apply(String value) throws CommandlineException;
    boolean acceptsValue();
    boolean filled();

    abstract class NullTypeMapper implements TypeMapper {}

    interface Constructor {
        TypeMapper create(Object target, Field f) throws Throwable;
    }
}
