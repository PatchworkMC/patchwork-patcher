package com.patchworkmc.mapping.remapper.exception;

public class FatalRemappingException extends RuntimeException {
	public FatalRemappingException(String message, Exception exception) {
		super(message, exception);
	}
}
