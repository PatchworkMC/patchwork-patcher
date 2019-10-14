package net.coderbot.patchwork.access;

import org.objectweb.asm.Opcodes;

public class ModAccessTransformation {
	public static final ModAccessTransformation NONE = new ModAccessTransformation(0, 0);
	public static final ModAccessTransformation DEFINALIZE =
			new ModAccessTransformation(Opcodes.ACC_FINAL, 0);
	public static final ModAccessTransformation MAKE_PUBLIC =
			new ModAccessTransformation(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED,
					Opcodes.ACC_PUBLIC);

	private int removed;
	private int added;

	public ModAccessTransformation(int removed, int added) {
		this.removed = removed;
		this.added = added;
	}

	public int getRemoved() {
		return removed;
	}

	public int getAdded() {
		return added;
	}

	public int apply(int access) {
		access &= (~getRemoved());
		access |= getAdded();

		return access;
	}

	@Override
	public boolean equals(Object o) {
		if(o == this) {
			return true;
		}

		if(o instanceof ModAccessTransformation) {
			ModAccessTransformation other = (ModAccessTransformation) o;

			return other.removed == this.removed && other.added == this.added;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return removed * 31 + added;
	}

	@Override
	public String toString() {
		return "AccessTransformation{"
				+ "removed=" + removed + ", added=" + added + '}';
	}
}
