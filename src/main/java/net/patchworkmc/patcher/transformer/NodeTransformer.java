package net.patchworkmc.patcher.transformer;

import org.objectweb.asm.tree.ClassNode;

public abstract class NodeTransformer {
	protected abstract void transform(ClassNode node);
}
