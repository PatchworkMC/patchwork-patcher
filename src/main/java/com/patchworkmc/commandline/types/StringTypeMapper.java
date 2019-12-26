package com.patchworkmc.commandline.types;

import java.lang.reflect.Field;

import com.patchworkmc.commandline.CommandlineException;

/**
 * Type mapper for identity mapping String values.
 *
 * @see BasicTypeMapper BasicTypeMapper for the base implementation
 */
public class StringTypeMapper extends BasicTypeMapper<String> {
	public StringTypeMapper(Object target, Field f) throws CommandlineException {
		super(target, f);

		if (f.getType() != String.class) {
			throw new CommandlineException("Tried to apply mapper for String to field of type " + f.getType().getName());
		}
	}

	/**
	 * Sets the underlying field to the value specified.
	 *
	 * @param value The value to set the field to
	 * @throws CommandlineException If an error occurs setting the underlying field
	 */
	@Override
	public void apply(String value) throws CommandlineException {
		set(value);
	}

	/**
	 * Always returns {@code true} since we need a String to set the field to.
	 *
	 * @return Always true
	 */
	@Override
	public boolean acceptsValue() {
		return true;
	}
}
