package net.patchworkmc.patcher.patch.redirect;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import net.patchworkmc.patcher.patch.util.ClassRedirection;
import net.patchworkmc.patcher.transformer.NodeTransformer;

/**
 * Redirects all NEW opcodes to a factory method, like PatchworkItemStack.of(foo).
 * Note that to simplify the implementation, all constructors must be included in the factory class.
 */
public class NewToFactoryRedirector extends NodeTransformer {
	/**
	 * The ClassRedirection format is simply oldOwner -> newOwner.
	 */
	private static final Map<String, ClassRedirection> redirects = new HashMap<>();

	static {
		redirects.put("net/minecraft/class_1799", new ClassRedirection("net/patchworkmc/api/capability/PatchworkItemStack"));
	}

	@Override
	protected void transform(ClassNode node) {
		for (MethodNode method : node.methods) {
			this.forEachInsn(method, TypeInsnNode.class, this::removeNew);
			this.forEachMethodInsn(method, this::redirectInit);
		}
	}

	private void removeNew(MethodNode node, TypeInsnNode insn) {
		if (insn.getOpcode() == Opcodes.NEW && redirects.get(insn.desc.substring(1, insn.desc.length()-1)) != null) {
			node.instructions.remove(insn.getNext());
			node.instructions.remove(insn);
		}
	}

	private void redirectInit(MethodNode node, MethodInsnNode insn) {
		ClassRedirection classRedirection = redirects.get(insn.owner);

		if (classRedirection != null && classRedirection.contains(insn.desc) && insn.name.equals("<init>")) {
			insn.desc = insn.desc.replace(")V", ")" + insn.owner);
			insn.owner = classRedirection.newOwner;
			insn.name = "of";
		}
	}
}
