package net.coderbot.patchwork.event;

public class SubscribeEvent {
	private int access;
	private String method;
	private String descriptor;
	private String signature;
	String priority;
	boolean receiveCancelled;

	public SubscribeEvent(int access, String method, String descriptor, String signature) {
		this.access = access;
		this.method = method;
		this.descriptor = descriptor;
		this.signature = signature;
		priority = "NORMAL";
		receiveCancelled = false;
	}

	public int getAccess() {
		return access;
	}

	public String getMethod() {
		return method;
	}

	public String getDescriptor() {
		return descriptor;
	}

	public String getSignature() {
		return signature;
	}

	public String getPriority() {
		return priority;
	}

	public boolean receiveCancelled() {
		return receiveCancelled;
	}

	@Override
	public String toString() {
		return "SubscribeEvent{" +
				"access=" + access +
				", method='" + method + '\'' +
				", descriptor='" + descriptor + '\'' +
				", signature='" + signature + '\'' +
				", priority='" + priority + '\'' +
				", receiveCancelled=" + receiveCancelled +
				'}';
	}
}
