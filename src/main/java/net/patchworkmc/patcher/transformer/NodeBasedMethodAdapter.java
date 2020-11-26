package net.patchworkmc.patcher.transformer;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

public abstract class NodeBasedMethodAdapter extends MethodVisitor {
	MethodVisitor next;

	public NodeBasedMethodAdapter(int access, String name, String desc, String signature, String[] exceptions, MethodVisitor methodVisitor) {
		super(Opcodes.ASM9, new MethodNode(access, name, desc, signature, exceptions));
		next = methodVisitor;
	}

	public abstract void transform(MethodNode mn);

	@Override
	public void visitEnd() {
		MethodNode mn = (MethodNode) mv;
		this.transform(mn);
		mn.accept(next);
	}
}
