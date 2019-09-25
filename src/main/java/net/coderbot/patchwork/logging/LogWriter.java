package net.coderbot.patchwork.logging;

public interface LogWriter {
    void log(LogLevel level, String message);
}
