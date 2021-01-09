package net.patchworkmc.patcher.patch.redirect;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import net.patchworkmc.patcher.patch.util.ClassRedirection;
import net.patchworkmc.patcher.transformer.NodeTransformer;
import net.patchworkmc.patcher.util.IllegalArgumentError;

/**
 * Redirects {@code INVOKEVIRTUAL com/example/Foo bar(IIJ)V} to {@code INVOKESTATIC net/example/FooUtils utilBar(Lcom/example/Foo;IIJ)V}.
 */
public final class VirtualToStaticRedirector extends NodeTransformer {
	/**
	 * The ClassRedirection format is {@code method+descriptor -> method}.
	 * <br>
	 * If a value is not provided it is assumed the method name is the same.
	 */
	private static final Map<String, ClassRedirection> redirects = new HashMap<>();

	static {
		//redirects.put("com/example/Foo", new ClassRedirection("net/example/FooUtils").with("bar(IIJ)V");
	}

	@Override
	protected void transform(ClassNode node) {
		for (MethodNode method : node.methods) {
			forEachMethodInsn(method, this::redirectMethodInsn);
		}
	}

	protected void redirectMethodInsn(MethodNode node, MethodInsnNode insn) {
		ClassRedirection redirection = redirects.get(insn.owner);

		if (redirection != null && redirection.contains(insn.name + insn.desc)) {
			if (insn.getOpcode() == org.objectweb.asm.Opcodes.INVOKESTATIC) {
				throw new IllegalArgumentError(String.format("ClassRedirection for class %s->%s targeting method %s "
								+ "seems to target a static method!", insn.owner, redirection.newOwner,
						insn.name + insn.desc));
			}

			insn.desc = generateDescriptor(insn.owner, insn.desc);
			insn.name = redirection.mapEntries.getOrDefault(insn.name + insn.desc, insn.name);
			insn.owner = redirection.newOwner;
			insn.setOpcode(Opcodes.INVOKESTATIC);
		}
	}

	private String generateDescriptor(String owner, String desc) {
		return String.format("(L%s;%s", owner, desc.substring(1));
	}
}
