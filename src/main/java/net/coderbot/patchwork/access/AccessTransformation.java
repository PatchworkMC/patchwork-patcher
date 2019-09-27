package net.coderbot.patchwork.access;

public class AccessTransformation {
	private String target;
	private int removed;
	private int added;

	public AccessTransformation(String target, int removed, int added) {
		this.target = target;
		this.removed = removed;
		this.added = added;
	}

	public int getRemoved() {
		return removed;
	}

	public int getAdded() {
		return added;
	}

	public String getTarget() {
		return target;
	}

	@Override
	public String toString() {
		return "AccessTransformation{" +
				"removed=" + removed +
				", added=" + added +
				", target='" + target + '\'' +
				'}';
	}
}
