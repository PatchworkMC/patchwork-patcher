package net.coderbot.patchwork.commandline;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Flag {
    String[] names();
    String description();

    Class<? extends TypeMapper> typeMapper() default TypeMapper.NullTypeMapper.class;
}
