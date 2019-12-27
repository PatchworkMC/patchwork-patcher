package com.patchworkmc.commandline;

/**
 * Base {@link Exception} for representing errors that can occur while working with the commandline.
 * Note that a {@link CommandlineParseException} is based on this class but will usually not be
 * exposed to the user.
 */
public class CommandlineException extends Exception {
	
	// Prevent serializable warning.
	private static final long serialVersionUID = 44967530028944993L;

	public CommandlineException(String message) {
		super(message);
	}

	public CommandlineException(String message, Throwable cause) {
		super(message, cause);
	}
}
