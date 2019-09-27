package net.coderbot.patchwork.commandline;

public class CommandlineException extends Exception {
    public CommandlineException(String message) {
        super(message);
    }

    public CommandlineException(String message, Throwable cause) {
        super(message, cause);
    }
}
