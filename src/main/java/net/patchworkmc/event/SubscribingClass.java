package net.patchworkmc.event;

import java.util.Objects;

/**
 * Object to hold information about a class that has at least one event subscriber.
 */
public class SubscribingClass {
	private final String className;
	private final boolean isInterface;
	private final boolean hasInstanceSubscribers;
	private final boolean hasStaticSubscribers;

	public SubscribingClass(String className, boolean isInterface, boolean hasInstanceSubscribers, boolean hasStaticSubscribers) {
		this.className = className;
		this.isInterface = isInterface;
		this.hasInstanceSubscribers = hasInstanceSubscribers;
		this.hasStaticSubscribers = hasStaticSubscribers;
	}

	public String getClassName() {
		return className;
	}

	public boolean isInterface() {
		return isInterface;
	}

	public boolean hasInstanceSubscribers() {
		return hasInstanceSubscribers;
	}

	public boolean hasStaticSubscribers() {
		return hasStaticSubscribers;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		SubscribingClass that = (SubscribingClass) o;

		return isInterface() == that.isInterface()
			&& hasInstanceSubscribers == that.hasInstanceSubscribers
			&& hasStaticSubscribers == that.hasStaticSubscribers
			&& Objects.equals(getClassName(), that.getClassName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getClassName(), isInterface(), hasInstanceSubscribers, hasStaticSubscribers);
	}
}
