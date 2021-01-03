package net.patchworkmc.patcher.transformer;

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
		if (node.desc.contains("IKeyConflict")) {
			System.out.println("handle for debugging");
		}
		Type[] types = Type.getMethodType(node.desc).getArgumentTypes();
		AbstractInsnNode previous = node.getPrevious();
		boolean countInstance = node.getOpcode() != Opcodes.INVOKESTATIC;
		for (int i = countInstance ? -1 : 0; i < types.length; i++) {
			if (previous instanceof LabelNode || previous instanceof LineNumberNode || previous instanceof FrameNode
					|| (previous instanceof JumpInsnNode && previous.getOpcode() != Opcodes.GOTO) /*hack for the top of ternaries*/) {
				previous = previous.getPrevious();
				i--;

			} else if (previous.getOpcode() == Opcodes.ANEWARRAY || previous.getOpcode() == Opcodes.NEWARRAY) {
				previous = previous.getPrevious();
				i = i-2;
			} else if (previous.getOpcode() == Opcodes.DUP && previous.getPrevious().getOpcode() == Opcodes.AASTORE) {
				previous = previous.getPrevious();
				i = i-4;
			} else if (previous.getOpcode() == Opcodes.AASTORE && previous.getNext().getOpcode() != Opcodes.DUP) {
				previous = previous.getPrevious();
				i = i-3;
			} else if (previous instanceof MethodInsnNode) {
				MethodInsnNode old = (MethodInsnNode) previous;
				previous = rewindMethod(old);

				if (old.name.equals("<init>")) {
					// this needs an extra skip
					i--;
				}
			} else if (previous.getOpcode() == Opcodes.GETFIELD) {
				// we need to skip over the pushing of the instance
				// skipUselessInstructions will make sure that we actually skip the instance pushing, and not a linenumber or something
				previous = skipUselessInstructionsBackwards(previous.getPrevious()).getPrevious();
			} else if (previous.getOpcode() == Opcodes.GOTO) {
				// we're in a ternary operator. Ew!
				// TODO: will this work if they're nested?
				//    I hope we never find out.
				previous = previous.getPrevious();
				// this is enough extra "arguments" to get us through the ternary
				i = i-3;
			} else if (previous instanceof InvokeDynamicInsnNode) {
				throw new UnsupportedOperationException("invokedynamic not yet implemented");
			} else if (previous.getOpcode() == Opcodes.MULTIANEWARRAY) {
				throw new UnsupportedOperationException("Multi-dimensional arrays not yet implemented");
			} else {
				previous = previous.getPrevious();
			}
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
