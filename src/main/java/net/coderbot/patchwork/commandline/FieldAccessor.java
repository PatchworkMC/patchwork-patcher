package net.coderbot.patchwork.commandline;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Field;

class FieldAccessor {
    private final TypeMapper typeMapper;
    private final String[] names;
    private final String description;
    private final boolean required;

    FieldAccessor(TypeMapper typeMapper, String[] names, String description, boolean required) {
        this.typeMapper = typeMapper;
        this.names = names;
        this.description = description;
        this.required = required;
    }

    public String[] names() {
        return names;
    }

    public String description() {
        return description;
    }

    public boolean acceptsValue() {
        return typeMapper.acceptsValue();
    }

    public void set(String value) throws CommandlineException {
        try {
            typeMapper.apply(value);
        } catch(WrongMethodTypeException e) {
            throw new CommandlineException("Badly bound method handle", e);
        } catch(Throwable t) {
            throw new CommandlineException("Setter threw " + ((t instanceof Exception) ? "exception" : "throwable"), t);
        }
    }

    public boolean required() {
        return required;
    }

    public boolean filled() {
        return typeMapper.filled();
    }
}
