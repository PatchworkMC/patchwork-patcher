package net.patchworkmc.patcher.event;

import java.util.Optional;

/**
 * A representation of the SubscribeEvent annotation, along with some metadata about the method it was attached to.
 */
public final class SubscribeEvent {
	protected String priority;
	protected boolean receiveCancelled;
	private final int access;
	private final String method;
	private final String eventClass;
	private final String genericClass;
	private final boolean hasReturnValue;

	public SubscribeEvent(int access, String method, String eventClass, String genericClass, boolean hasReturnValue) {
		this.access = access;
		this.method = method;
		this.eventClass = eventClass;
		this.genericClass = genericClass;
		this.hasReturnValue = hasReturnValue;
		this.priority = "NORMAL";
		this.receiveCancelled = false;
	}

	public int getAccess() {
		return access;
	}

	/**
	 * @return the name of the method, i.e. "handleBlockPlaceEvent"
	 */
	public String getMethod() {
		return method;
	}

	public String getEventClass() {
		return eventClass;
	}

	/**
	 * @return the bytecode descriptor of the method, i.e. "(Lcom/example/MyEvent;)V"
	 */
	public String getMethodDescriptor() {
		return "(L" + getEventClass() + ";)V";
	}

	/**
	 * @return the class in the generic signature of the method (TODO: what is the format?)
	 */
	public Optional<String> getGenericClass() {
		return Optional.ofNullable(genericClass);
	}

	/**
	 * @return the priority of the subscription. Can be LOWEST, LOW, NORMAL, HIGH, or HIGHEST
	 */
	public String getPriority() {
		return priority;
	}

	public boolean receiveCancelled() {
		return receiveCancelled;
	}

	/**
	 * @return if the method returns something besides void. TOOD: why is this here? Why is it ignored?
	 */
	public boolean hasReturnValue() {
		return hasReturnValue;
	}

	@Override
	public String toString() {
		return "SubscribeEvent{"
			+ "priority='" + priority + '\''
			+ ", receiveCancelled=" + receiveCancelled
			+ ", access=" + access
			+ ", method='" + method + '\''
			+ ", eventClass='" + eventClass + '\''
			+ ", genericClass='" + genericClass + '\''
			+ ", hasReturnValue=" + hasReturnValue
			+ '}';
	}
}
