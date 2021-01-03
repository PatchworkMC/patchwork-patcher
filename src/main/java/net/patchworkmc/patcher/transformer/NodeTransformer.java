package net.patchworkmc.patcher.transformer;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;

public abstract class NodeTransformer {
	protected abstract void transform(ClassNode node);

	/**
	 * <a href="https://youtu.be/YbJOTdZBX1g?t=9">It's rewind time.</a>
	 * @return the instruction immediately before the first
	 */
	@Nullable
	protected final AbstractInsnNode rewindMethod(@NotNull MethodInsnNode node) {
		Type[] types = Type.getMethodType(node.desc).getArgumentTypes();
		AbstractInsnNode previous = node.getPrevious();
		boolean countInstance = node.getOpcode() != Opcodes.INVOKESTATIC;

		// sorry for such messy code, but every time i try to refactor it to be cleaner it breaks
		for (int i = countInstance ? -1 : 0; i < types.length; i++) {
			AbstractInsnNode ret = previous.getPrevious();
			Objects.requireNonNull(ret, "reached top of method early?");

			if (previous instanceof LabelNode || previous instanceof LineNumberNode || previous instanceof FrameNode
					|| (previous instanceof JumpInsnNode && previous.getOpcode() != Opcodes.GOTO) /*hack for the top of ternaries*/) {
				// process an extra "argument"
				i--;
			} else if (previous.getOpcode() == Opcodes.ANEWARRAY || previous.getOpcode() == Opcodes.NEWARRAY) {
				// need to get around the ANEWARRAY and the array size
				i = i-2;
			} else if (previous.getOpcode() == Opcodes.DUP && previous.getPrevious().getOpcode() == Opcodes.AASTORE) {
				// need to get around the DUP, AASTORE, the value being stored, and the index
				i = i-4;
			} else if (previous.getOpcode() == Opcodes.AASTORE && previous.getNext().getOpcode() != Opcodes.DUP) {
				// need to get around the AASTORE, the value being stored, and the index
				i = i-3;
			} else if (previous instanceof MethodInsnNode) {
				MethodInsnNode old = (MethodInsnNode) previous;
				ret = rewindMethod(old);

				if (old.name.equals("<init>")) {
					// init is an invokestatic, so we need to not count it as an argument
					i--;
				}
			} else if (previous.getOpcode() == Opcodes.GETFIELD) {
				// we need to skip over the pushing of the instance
				// skipUselessInstructions will make sure that we actually skip the instance pushing, and not a linenumber or something
				ret = skipUselessInstructionsBackwards(previous.getPrevious()).getPrevious();
			} else if (previous instanceof InvokeDynamicInsnNode) {
				throw new UnsupportedOperationException("invokedynamic not yet implemented");
			} else if (previous.getOpcode() == Opcodes.MULTIANEWARRAY) {
				throw new UnsupportedOperationException("Multi-dimensional arrays not yet implemented");
			} else {
				AbstractInsnNode nextToProcess = skipUselessInstructionsBackwards(previous.getPrevious());

				if (nextToProcess != null && nextToProcess.getOpcode() == Opcodes.GOTO) {
					// we're about to enter a ternary operator. Ew!
					// TODO: will this work if they're nested?
					//    I hope we never find out.
					// this is enough extra "arguments" to get us through the ternary
					i = i-3;
				}

				ret = previous.getPrevious();
			}

			previous = ret;
		}

		return previous;
	}

	protected final AbstractInsnNode skipUselessInstructionsBackwards(AbstractInsnNode node) {
		do {
			if (!(node instanceof LabelNode || node instanceof LineNumberNode || node instanceof FrameNode)) {
				return node;
			}
		} while ((node = node.getPrevious()) != null);

		return null;
	}

	protected final AbstractInsnNode skipUselessInstructionsForwards(AbstractInsnNode node) {
		do {
			if (!(node instanceof LabelNode || node instanceof LineNumberNode || node instanceof FrameNode)) {
				return node;
			}
		} while ((node = node.getNext()) != null);

		return null;
	}
}
