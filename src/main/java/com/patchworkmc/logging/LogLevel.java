package com.patchworkmc.logging;

/**
 * Represents a log level.
 */
public enum LogLevel {
	TRACE(5000),
	DEBUG(10000),
	INFO(20000),
	WARN(30000),
	ERROR(40000),
	FATAL(50000);

	private final int numericalLevel;

	LogLevel(int numericalLevel) {
		this.numericalLevel = numericalLevel;
	}

	/**
	 * Numerical representation of the log level for comparison with other levels.
	 *
	 * @return Numerical representation of the log level
	 */
	public int numerical() {
		return numericalLevel;
	}

	/**
	 * Determines wether this log level includes another one. For example, INFO includes WARN, but
	 * WARN does not include INFO.
	 *
	 * @param other The log level to check if its included
	 * @return {@code true} if this level includes (enables) the other level, {@code false}
	 * otherwise. Checking if a level includes will always be {@code true}.
	 */
	public boolean includes(LogLevel other) {
		return this.numericalLevel <= other.numericalLevel;
	}
}
