package net.patchworkmc.patcher.transformer;

import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public abstract class NodeTransformer {
	protected abstract void transform(ClassNode node);

	protected final <T> void forEachInsn(MethodNode node, Class<T> target, Consumer<T> consumer) {
		for (AbstractInsnNode instruction : node.instructions) {
			if (target.isAssignableFrom(instruction.getClass())) {
				//noinspection unchecked
				consumer.accept((T) instruction);
			}
		}
	}

	protected final <T> void forEachInsn(MethodNode node, Class<T> target, BiConsumer<MethodNode, T> biConsumer) {
		for (AbstractInsnNode instruction : node.instructions) {
			if (target.isAssignableFrom(instruction.getClass())) {
				//noinspection unchecked
				biConsumer.accept(node, (T) instruction);
			}
		}
	}

	protected final void forEachMethodInsn(MethodNode node, BiConsumer<MethodNode, MethodInsnNode> consumer) {
		forEachInsn(node, MethodInsnNode.class, consumer);
	}
}
