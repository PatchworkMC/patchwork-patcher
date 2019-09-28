package net.coderbot.patchwork.access;

public class AccessTransformation {
	public static final AccessTransformation NONE = new AccessTransformation(0, 0);

	private int removed;
	private int added;

	public AccessTransformation(int removed, int added) {
		this.removed = removed;
		this.added = added;
	}

	public int getRemoved() {
		return removed;
	}

	public int getAdded() {
		return added;
	}

	@Override
	public String toString() {
		return "AccessTransformation{"
				+ "removed=" + removed + ", added=" + added + '}';
	}
}
