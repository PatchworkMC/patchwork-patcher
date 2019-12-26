package com.patchworkmc.commandline;

import java.lang.invoke.WrongMethodTypeException;

/**
 * Utility class for generalizing access to fields. Can be seen like a wrapper around a TypeMapper
 * for a specific parameter or flag.
 */
class FieldAccessor {
	private final TypeMapper typeMapper;
	private final String[] names;
	private final String description;
	private final boolean required;

	/**
	 * Constructs a new FieldAccessor with a specified type mapper and some utility parameters for a
	 * specified parameter or flag.
	 *
	 * @param typeMapper  The type mapper to use
	 * @param names       Names of the parameter or flag
	 * @param description Description of the parameter or flag
	 * @param required    Wether this parameter or flag is required
	 */
	FieldAccessor(TypeMapper typeMapper, String[] names, String description, boolean required) {
		this.typeMapper = typeMapper;
		this.names = names;
		this.description = description;
		this.required = required;
	}

	/**
	 * Retrieves the names of the flag or parameter.
	 *
	 * @return An array containing the names of the flag or parameter
	 */
	String[] names() {
		return names;
	}

	/**
	 * Retrieves the description of the flag or parameter.
	 *
	 * @return The description
	 */
	public String description() {
		return description;
	}

	/**
	 * Determines wether the underlying type mapper accepts a value.
	 *
	 * @return {@code true} if the underlying type mapper accepts a value, {@code false} otherwise
	 */
	boolean acceptsValue() {
		return typeMapper.acceptsValue();
	}

	/**
	 * Applies the given string using the type mapper.
	 *
	 * @param value The value to apply
	 * @throws CommandlineException If an error occurs applying the value
	 */
	public void set(String value) throws CommandlineException {
		try {
			typeMapper.apply(value);
		} catch (WrongMethodTypeException e) {
			throw new CommandlineException("Badly bound method handle", e);
		} catch (Throwable t) {
			throw new CommandlineException("Setter threw " + ((t instanceof Exception) ? "exception" : "throwable"), t);
		}
	}

	/**
	 * Determines wether this parameter is required. Always returns false for flags.
	 *
	 * @return {@code true} if this parameter is required, {@code false} if not or if we are dealing
	 * with a flag
	 */
	boolean required() {
		return required;
	}

	/**
	 * Determines wether this parameter or flag has been filled. Might depend on previous used
	 * values.
	 *
	 * @return {@code true} if the parameter or flag has been fully filled already, {@code false}
	 * otherwise
	 */
	boolean filled() {
		return typeMapper.filled();
	}
}
