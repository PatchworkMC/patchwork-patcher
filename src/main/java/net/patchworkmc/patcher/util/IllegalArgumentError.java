package net.patchworkmc.patcher.util;

import net.patchworkmc.patcher.patch.util.ClassRedirection;

/**
 * Thrown to indicate that a programmer has made a mistake that should not be recoverable.
 * <p>
 * For example, an IllegalArugmentError is thrown when a {@link ClassRedirection}
 * used in a Redirector is not properly configured, because this kind of mistake should be as visible as possible
 * during testing and never happen in production.
 * </p>
 */
public class IllegalArgumentError extends Error {
	public IllegalArgumentError() {
	}

	public IllegalArgumentError(String message) {
		super(message);
	}

	public IllegalArgumentError(String message, Throwable cause) {
		super(message, cause);
	}

	public IllegalArgumentError(Throwable cause) {
		super(cause);
	}
}
