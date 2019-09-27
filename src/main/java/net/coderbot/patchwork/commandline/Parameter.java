package net.coderbot.patchwork.commandline;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Parameter {
    String name();
    String description();
    int position();
    boolean required() default true;

    Class<? extends TypeMapper> typeMapper() default TypeMapper.NullTypeMapper.class;

}
