package net.patchworkmc.patcher.patch;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import net.patchworkmc.patcher.patch.util.ClassRedirection;
import net.patchworkmc.patcher.transformer.NodeTransformer;

public class StaticMethodRedirectionTransformer extends NodeTransformer {
	/**
	 * The format for the ClassRedirection is method name + desc -> method name.
	 */
	private static final Map<String, ClassRedirection> redirects = new HashMap<>();

	static {
		// redirects.put("net/minecraft/class_123", new ClassRedirection("net/patchwork/Foo").with("asd(I)Ljava/lang/String;", "efg");
	}

	@Override
	protected void transform(ClassNode node) {
		for (MethodNode method : node.methods) {
			transformMethod(method);
		}
	}

	private void transformMethod(MethodNode node) {
		for (AbstractInsnNode instruction : node.instructions) {
			if (instruction.getOpcode() == Opcodes.INVOKESTATIC) {
				MethodInsnNode insn = (MethodInsnNode) instruction;
				ClassRedirection classRedirection = redirects.get(insn.owner);

				if (classRedirection != null) {
					String newMethodName = classRedirection.entries.get(insn.name + insn.desc);

					if (newMethodName != null) {
						insn.owner = classRedirection.newName;
						insn.name = newMethodName;
					}
				}
			}
		}
	}
}
