package com.patchworkmc.commandline.types;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import com.patchworkmc.commandline.CommandlineException;

/**
 * Type mapper for converting <b>a flag with no value</b> into a boolean.
 *
 * @see BasicTypeMapper BasicTypeMapper for the base implementation
 */
public class BooleanTypeMapper extends BasicTypeMapper<Boolean> {
	private static final List<Class<?>> SUPPORTED_TYPES = Arrays.asList(Boolean.class, boolean.class, Boolean.TYPE);

	public BooleanTypeMapper(Object target, Field f) throws CommandlineException {
		super(target, f);

		if (!SUPPORTED_TYPES.contains(f.getType())) {
			throw new CommandlineException("Tried to apply mapper for type Boolean to field of type " + f.getType().getName());
		}
	}

	/**
	 * Sets the field targeted by this mapper to {@code true}.
	 * As specified by {@link BooleanTypeMapper#acceptsValue()} this mapper does not accept a value
	 * and thus expects the parameter to this method to be null!
	 *
	 * @param value Ignored, if asserts enabled asserted to be null
	 * @throws CommandlineException If an error occurs while setting the underlying field to {@code
	 *                              true}
	 */
	@Override
	public void apply(String value) throws CommandlineException {
		assert value == null;
		set(true);
	}

	/**
	 * Always returns {@code false} as calling {@link BooleanTypeMapper#apply(String)} expects the
	 * value to be
	 * {@code null}.
	 *
	 * @return Always {@code false}
	 */
	@Override
	public boolean acceptsValue() {
		return false;
	}
}
