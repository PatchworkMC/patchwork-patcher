package net.patchworkmc.patcher.patch;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import net.patchworkmc.patcher.transformer.NodeTransformer;

public class SuperclassRedirectionTransformer extends NodeTransformer {
	private static final Map<String, ClassRedirection> redirects = new HashMap<>();

	static {
		redirects.put("net/minecraft/class_1761",
				new ClassRedirection("net/patchworkmc/api/registries/PatchworkItemGroup")
						.withMethod("method_7750()Lnet/minecraft/class_1799;", "patchwork$createIconHack"));
	}

	@Override
	protected void transform(ClassNode node) {
		ClassRedirection redirection = redirects.get(node.superName);

		if (redirection != null) {
			node.superName = redirects.get(node.superName).newName;

			for (MethodNode method : node.methods) {
				String rename = redirection.methods.get(method.name + method.desc);

				if (rename != null) {
					method.name = rename;
				}
			}
		}

		node.methods.forEach(this::redirectInstanceCreation);
	}

	private void redirectInstanceCreation(MethodNode method) {
		for (AbstractInsnNode in : method.instructions) {
			if (!(in instanceof MethodInsnNode)) {
				continue;
			}

			MethodInsnNode min = (MethodInsnNode) in;

			ClassRedirection classRedirection = redirects.get(min.owner);

			if (classRedirection == null) {
				continue;
			}

			if (min.getOpcode() == Opcodes.INVOKESPECIAL && min.name.equals("<init>")) {
				AbstractInsnNode prev = min;

				while (prev != null) {
					prev = prev.getPrevious();

					if (prev instanceof TypeInsnNode && prev.getOpcode() == Opcodes.NEW && ((TypeInsnNode) prev).desc.equals(min.owner)) {
						((TypeInsnNode) prev).desc = classRedirection.newName;
						break;
					}
				}

				min.owner = classRedirection.newName;
			}
		}
	}

	private static class ClassRedirection {
		final String newName;

		final Map<String, String> methods = new HashMap<>();

		ClassRedirection(String newName) {
			this.newName = newName;
		}

		final ClassRedirection withMethod(String nameAndDesc, String newName) {
			this.methods.put(nameAndDesc, newName);
			return this;
		}
	}
}
