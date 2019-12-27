package com.patchworkmc.manifest.forge;

public class ManifestParseException extends Exception {

	// Prevent serializable warning.
	private static final long serialVersionUID = 1634635202713032248L;

	public ManifestParseException(String message) {
		super(message);
	}

	public ManifestParseException(String message, Exception exception) {
		super(message, exception);
	}
}
