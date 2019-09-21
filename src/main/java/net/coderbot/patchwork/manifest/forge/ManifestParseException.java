package net.coderbot.patchwork.manifest.forge;

public class ManifestParseException extends Exception {
	public ManifestParseException(String message) {
		super(message);
	}

	public ManifestParseException(String message, Exception exception) {
		super(message, exception);
	}
}
