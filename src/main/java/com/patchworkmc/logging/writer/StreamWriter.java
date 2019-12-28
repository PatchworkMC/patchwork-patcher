package com.patchworkmc.logging.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;

import org.fusesource.jansi.Ansi;

import com.patchworkmc.logging.LogLevel;
import com.patchworkmc.logging.LogWriter;

/**
 * Logger backend writing messages to {@link OutputStream}s.
 */
public class StreamWriter implements LogWriter {
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

	private final boolean color;
	private final OutputStream out;
	private final OutputStream err;
	private OutputStream last;

	/**
	 * Constructs a new StreamWriter.
	 *
	 * @param color Determines if colors should be enabled
	 * @param out   The stream to treat as the standard output (for example {@link System#out})
	 * @param err   The stream to treat as the error output (for example {@link System#err})
	 */
	public StreamWriter(boolean color, OutputStream out, OutputStream err) {
		this.color = color;
		this.out = out;
		this.err = err;
	}

	@Override
	public void log(LogLevel level, String message) {
		List<byte[]> messages = new ArrayList<>();
		String prefix = prefix(level);

		for (String part : message.split("\n")) {
			messages.add((prefix + part + "\n").getBytes());
		}

		if (!LogLevel.WARN.includes(level)) {
			try {
				for (byte[] msg : messages) {
					if (last == err && last != null) {
						err.flush();;
					}

					out.write(msg);
					last = out;
				}
			} catch (IOException e) {
				// Simply terminate, logging failed, we can't really "log" the exception
				throw new RuntimeException(e);
			}
		} else {
			try {
				for (byte[] msg : messages) {
					if (last == out && last != null) {
						out.flush();;
					}

					err.write(msg);
					last = err;
				}
			} catch (IOException e) {
				// Simply terminate, logging failed, we can't really "log" the exception
				throw new RuntimeException(e);
			}
		}
	}

	// Helper for generating a prefix for a specific log level
	private String prefix(LogLevel level) {
		String logPrefix;

		switch (level) {
		case TRACE:
			logPrefix = color ? Ansi.ansi().fgBrightMagenta().a("TRACE").toString() : "TRACE";
			break;

		case DEBUG:
			logPrefix = color ? Ansi.ansi().fgYellow().a("DEBUG").toString() : "DEBUG";
			break;

		case INFO:
			logPrefix = color ? Ansi.ansi().fgBrightBlue().a("INFO").toString() : "INFO";
			break;

		case WARN:
			logPrefix = color ? Ansi.ansi().fgBrightYellow().a("WARN").toString() : "WARN";
			break;

		case ERROR:
			logPrefix = color ? Ansi.ansi().fgBrightRed().a("ERROR").toString() : "ERROR";
			break;

		case FATAL:
			logPrefix = color ? Ansi.ansi().fgRed().a("FATAL").toString() : "FATAL";
			break;

		default:
			throw new AssertionError("UNREACHABLE");
		}

		if (color) {
			return Ansi.ansi().reset().fgBrightBlack().a("[").reset().a(logPrefix).reset().fgBrightBlack().a("] ").fgBrightYellow().a(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).format(LocalDateTime.now())).fgBrightBlack().a(": ").reset().toString();
		} else {
			return "[" + logPrefix + "] " + DATE_TIME_FORMATTER.format(LocalDateTime.now()) + ": ";
		}
	}
}
