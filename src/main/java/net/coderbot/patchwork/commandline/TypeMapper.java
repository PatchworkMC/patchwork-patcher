package net.coderbot.patchwork.commandline;

import java.lang.reflect.Field;

/**
 * Interface representing a type which can map strings (or null) to other types.
 */
public interface TypeMapper {
	/**
	 * Tries to apply the given value to the target. What exactly this does, is implementation
	 * defined.
	 *
	 * @param value The value which should be applied, will be null if {@link
	 *         TypeMapper#acceptsValue()} is false
	 * @throws CommandlineParseException If value could not be converted into the target type
	 * @throws CommandlineException If something goes wrong while setting the field
	 */
	void apply(String value) throws CommandlineException;

	/**
	 * Determines wether a value is accepted and required.
	 *
	 * @return {@code true} if a value is accepted and required, {@code false} otherwise.
	 */
	boolean acceptsValue();

	/**
	 * Determines wether this type mapper accepts more {@link TypeMapper#apply(String)} calls
	 *
	 * @return Wether more apply calls are accepted
	 */
	boolean filled();

	// Fake "null" class since annotations can't return null be default
	abstract class NullTypeMapper implements TypeMapper {}

	// Helper interface since BiFunction's can't throw
	@FunctionalInterface
	interface Constructor {
		TypeMapper create(Object target, Field f) throws Throwable;
	}
}
