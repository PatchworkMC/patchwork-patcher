package net.coderbot.patchwork.logging;

import java.util.HashMap;
import java.util.Map;

public class Logger {
    private static Logger instance;

    public static Logger getInstance() {
        return instance == null ? instance = new Logger() : instance;
    }

    private final Map<LogWriter, LogLevel> writers;

    private Logger() {
        writers = new HashMap<>();
    }

    public void setWriter(LogWriter writer, LogLevel level) {
        this.writers.put(writer, level);
    }

    public void log(LogLevel level, String format, Object ...args) {
        String formattedLog = String.format(format, args);
        writers.forEach((writer, writerLevel) -> {
            if(writerLevel.includes(level)) {
                writer.log(level, formattedLog);
            }
        });
    }

    public void trace(String format, Object ...args) {
        log(LogLevel.TRACE, format, args);
    }

    public void debug(String format, Object ...args) {
        log(LogLevel.DEBUG, format, args);
    }

    public void info(String format, Object ...args) {
        log(LogLevel.INFO, format, args);
    }

    public void warn(String format, Object ...args) {
        log(LogLevel.WARN, format, args);
    }

    public void error(String format, Object ...args) {
        log(LogLevel.ERROR, format, args);
    }

    public void fatal(String format, Object ...args) {
        log(LogLevel.FATAL, format, args);
    }

    public void thrown(LogLevel level, Throwable cause) {
        boolean first = true;
        StringBuilder messageBuffer = new StringBuilder();
        while(cause != null) {
            if(first) {
                messageBuffer
                        .append((cause instanceof Exception) ? "Exception" : "Throwable")
                        .append(" thrown in thread ")
                        .append(Thread.currentThread().getName());
            } else {
                messageBuffer
                        .append("Caused by ")
                        .append((cause instanceof Exception) ? "Exception" : "Throwable");
            }
            messageBuffer
                    .append(" ")
                    .append(cause.getClass().getName())
                    .append(": ")
                    .append(cause.getLocalizedMessage());

            for(StackTraceElement element : cause.getStackTrace()) {
                messageBuffer
                        .append("\n    at ")
                        .append(element.getClassName())
                        .append(".")
                        .append(element.getMethodName())
                        .append("(")
                        .append(element.getFileName())
                        .append(":")
                        .append(element.getLineNumber())
                        .append(")");
            }
            cause = cause.getCause();
            first = false;
        }

        String message = messageBuffer.toString();
        log(level, message);
    }
}
