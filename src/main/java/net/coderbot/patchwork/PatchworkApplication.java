package net.coderbot.patchwork;

import net.coderbot.patchwork.logging.LogLevel;
import net.coderbot.patchwork.logging.Logger;
import net.coderbot.patchwork.logging.writer.StreamWriter;
import org.fusesource.jansi.AnsiConsole;

public class PatchworkApplication {
    public static void main(String[] args) {
        AnsiConsole.systemInstall();

        Logger logger = Logger.getInstance();
        logger.setWriter(new StreamWriter(true, System.out, System.err), LogLevel.TRACE);
        logger.trace("Trace");
        logger.debug("Debug");
        logger.info("Info");
        logger.warn("Warn");
        logger.error("Error");
        logger.fatal("Fatal");
        logger.thrown(LogLevel.ERROR, new NullPointerException("Error"));
    }
}
