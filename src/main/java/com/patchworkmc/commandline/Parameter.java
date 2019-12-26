package com.patchworkmc.commandline;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking a field as a parameter in the type of an object passed to a {@link
 * CommandlineParser}.
 * <p>The field may be private since access will be acquired via reflection.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Parameter {
	/**
	 * The name of the parameter. This will later only be used to display it to the user and has no
	 * further effect on the control flow itself.
	 *
	 * @return The name of the parameter
	 */
	String name();

	/**
	 * A description of what this parameter is used for. Newlines can be freely used, should even be
	 * to avoid lines getting too long. Too long means being wrapped by the terminal and thus it
	 * can't be really defined what the limit is.
	 *
	 * @return A description of what this parameter is used for
	 */
	String description();

	/**
	 * Zero based index of the position of the parameter. So parameter with the position 0 will be
	 * the first, 1 will be the next and so on. <b>Use -1 to indicate a parameter collecting the
	 * remaining arguments.</b>
	 *
	 * @return The index of the position of this parameter
	 */
	int position();

	/**
	 * <p>
	 * Wether this parameter is required or not. <b>Note that an optional parameter cannot be
	 * followed by a required parameter.</b> Applied to {@link Parameter#position()} this means that
	 * the position of a optional parameter must always be greater than the position of all required
	 * parameters. Optional parameters also can't be used when a parameter collecting the remaining
	 * arguments is used.
	 * </p>
	 *
	 * <p>
	 * Defaults to true.
	 * </p>
	 *
	 * @return {@code true} if this parameter is required, {@code false} otherwise
	 */
	boolean required() default true;

	/**
	 * The type mapper to use to map this value to. Defaults to a "null" type in which case the
	 * {@link CommandlineParser} will search for the correct one.
	 *
	 * @return The type mapper to use or {@link TypeMapper.NullTypeMapper} if the default one should
	 * be used
	 */
	Class<? extends TypeMapper> typeMapper() default TypeMapper.NullTypeMapper.class;
}
