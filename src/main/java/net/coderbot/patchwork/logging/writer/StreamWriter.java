package net.coderbot.patchwork.logging.writer;

import net.coderbot.patchwork.logging.LogLevel;
import net.coderbot.patchwork.logging.LogWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;

import org.fusesource.jansi.Ansi;

/**
 * Logger backend writing messages to {@link OutputStream}s
 */
public class StreamWriter implements LogWriter {
	private static final DateTimeFormatter DATE_TIME_FORMATTER =
			DateTimeFormatter.ofPattern("HH:mm:ss");

	private final boolean color;
	private final OutputStream out;
	private final OutputStream err;

	/**
	 * Constructs a new StreamWriter
	 *
	 * @param color Determines if colors should be enabled
	 * @param out The stream to treat as the standard output (for example {@link System#out})
	 * @param err The stream to treat as the error output (for example {@link System#err})
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
		for(String part : message.split("\n")) {
			messages.add((prefix + part + "\n").getBytes());
		}

		if(!LogLevel.WARN.includes(level)) {
			try {
				for(byte[] msg : messages) {
					out.write(msg);
				}
			} catch(IOException e) {
				// Simply terminate, logging failed, we can't really "log" the exception
				throw new RuntimeException(e);
			}
		} else {
			try {
				for(byte[] msg : messages) {
					err.write(msg);
				}
			} catch(IOException e) {
				// Simply terminate, logging failed, we can't really "log" the exception
				throw new RuntimeException(e);
			}
		}
	}

	// Helper for generating a prefix for a specific log level
	private String prefix(LogLevel level) {
		String logPrefix;

		switch(level) {
			case TRACE:
				logPrefix = color ? Ansi.ansi().fgBrightMagenta().a("T").toString() : "T";
				break;

			case DEBUG:
				logPrefix = color ? Ansi.ansi().fgYellow().a("D").toString() : "D";
				break;

			case INFO:
				logPrefix = color ? Ansi.ansi().fgBrightBlue().a("I").toString() : "I";
				break;

			case WARN:
				logPrefix = color ? Ansi.ansi().fgBrightYellow().a("W").toString() : "W";
				break;

			case ERROR:
				logPrefix = color ? Ansi.ansi().fgBrightRed().a("E").toString() : "E";
				break;

			case FATAL:
				logPrefix = color ? Ansi.ansi().fgRed().a("F").toString() : "F";
				break;

			default:
				throw new AssertionError("UNREACHABLE");
		}

		if(color) {
			return Ansi.ansi()
					.reset()
					.fgBrightBlack()
					.a("[")
					.reset()
					.a(logPrefix)
					.reset()
					.fgBrightBlack()
					.a("] ")
					.fgBrightYellow()
					.a(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
									.format(LocalDateTime.now()))
					.fgBrightBlack()
					.a(": ")
					.reset()
					.toString();
		} else {
			return "[" + logPrefix + "] " + DATE_TIME_FORMATTER.format(LocalDateTime.now()) + ": ";
		}
	}
}
