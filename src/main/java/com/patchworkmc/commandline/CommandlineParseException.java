package com.patchworkmc.commandline;

/**
 * {@link Exception} representing parse errors for commandline values, for example if the user types
 * in a value which is not a number, but a number was expected. Note that this class is usually not
 * exposed to the user in terms of need of being caught.
 */
public class CommandlineParseException extends CommandlineException {
	public CommandlineParseException(String message) {
		super(message);
	}

	public CommandlineParseException(String message, Throwable cause) {
		super(message, cause);
	}
}
