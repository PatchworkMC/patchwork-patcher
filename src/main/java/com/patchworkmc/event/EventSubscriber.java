package com.patchworkmc.event;

import java.util.Objects;

/**
 * Object to hold information about a class that has at least one event subscriber.
 */
public class EventSubscriber {
	private final String className;
	private final boolean isInterface;
	private final boolean hasInstanceSubscriber;
	private final boolean hasStaticSubscriber;

	public EventSubscriber(String className, boolean isInterface, boolean hasInstanceSubscriber, boolean hasStaticSubscriber) {
		this.className = className;
		this.isInterface = isInterface;
		this.hasInstanceSubscriber = hasInstanceSubscriber;
		this.hasStaticSubscriber = hasStaticSubscriber;
	}

	public String getClassName() {
		return className;
	}

	public boolean isInterface() {
		return isInterface;
	}

	public boolean hasInstanceSubscriber() {
		return hasInstanceSubscriber;
	}

	public boolean hasStaticSubscriber() {
		return hasStaticSubscriber;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		EventSubscriber that = (EventSubscriber) o;

		return isInterface() == that.isInterface()
			&& hasInstanceSubscriber == that.hasInstanceSubscriber
			&& hasStaticSubscriber == that.hasStaticSubscriber
			&& Objects.equals(getClassName(), that.getClassName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getClassName(), isInterface(), hasInstanceSubscriber, hasStaticSubscriber);
	}
}
