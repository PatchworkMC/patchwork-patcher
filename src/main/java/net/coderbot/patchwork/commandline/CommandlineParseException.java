package net.coderbot.patchwork.commandline;

public class CommandlineParseException extends CommandlineException {
    public CommandlineParseException(String message) {
        super(message);
    }

    public CommandlineParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
