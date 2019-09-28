package net.coderbot.patchwork;

import net.coderbot.patchwork.commandline.*;
import net.coderbot.patchwork.logging.LogLevel;
import net.coderbot.patchwork.logging.Logger;
import net.coderbot.patchwork.logging.writer.StreamWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.fusesource.jansi.AnsiConsole;

public class PatchworkApplication {
	private static class Commandline {
		@Flag(names = { "h", "help" }, description = "Display this message")
		boolean help;

		@Flag(names = { "no-colors" }, description = "Disable colorful output")
		boolean noColors;

		@Parameter(name = "file one", description = "Test file 1", position = 0)
		String fileOne;

		@Parameter(name = "another file",
				description = "The other file\nIts a cooler one!",
				position = 1)
		String fileTwo;

		@Parameter(name = "optional file",
				description = "Nobody needs this file, you can still supply it",
				position = 2,
				required = false)
		String optionalFile;
	}

	public static void main(String[] args) {
		AnsiConsole.systemInstall();
		Logger logger = Logger.getInstance();

		CommandlineParser<Commandline> parser =
				new CommandlineParser<>(new Commandline(), requestArgs());
		Commandline commandline = null;
		try {
			commandline = parser.parse();
			logger.setWriter(new StreamWriter(!commandline.noColors, System.out, System.err),
					LogLevel.TRACE);
		} catch(CommandlineException e) {
			logger.setWriter(new StreamWriter(false, System.out, System.err), LogLevel.TRACE);
			logger.fatal("Error while parsing commandline!", e);
			logger.thrown(LogLevel.FATAL, e);
			System.exit(1);
		}

		if(!parser.parseSucceeded() || commandline.help) {
			System.out.println(parser.generateHelpMessage(getExecutableName(),
					"Patchwork-Patcher v0.1.0",
					"Patchwork Patcher is a set of tools for transforming and patching Forge mod\n"
							+ "jars into jars that are directly loadable by Fabric Loader",
					"WARNING: Early alpha!",
					!commandline.noColors));
			System.exit(commandline.help ? 0 : 1);
		}

		logger.trace("Trace");
		logger.debug("Debug");
		logger.info("Info");
		logger.warn("Warn");
		logger.error("Error");
		logger.fatal("Fatal");
		logger.thrown(LogLevel.ERROR, new NullPointerException("Error"));
	}

	private static String getExecutableName() {
		try {
			URL location =
					PatchworkApplication.class.getProtectionDomain().getCodeSource().getLocation();
			if(location.getProtocol().equals("file")) {
				return location.getPath();
			}
		} catch(Exception e) {
			Logger.getInstance().debug("Failed to get executable name");
			Logger.getInstance().thrown(LogLevel.DEBUG, e);
		}

		return "/path/to/patchwork.jar";
	}

	private static String[] requestArgs() {
		try {
			System.out.print("Enter commandline: ");
			System.out.flush();
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			return ArgumentTokenizer.tokenize(reader.readLine()).toArray(new String[0]);
		} catch(IOException e) {
			Logger.getInstance().debug("Failed to split read commandline froms stdin");
			Logger.getInstance().thrown(LogLevel.DEBUG, e);
			return new String[0];
		}
	}
}
