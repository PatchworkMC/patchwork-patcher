package net.patchworkmc.patcher.patch;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import net.patchworkmc.patcher.patch.util.ClassRedirection;
import net.patchworkmc.patcher.transformer.NodeTransformer;

public class SuperclassRedirectionTransformer extends NodeTransformer {
	/**
	 * The format for ClassRedirection entries is method name + desc -> method name.
	 */
	private static final Map<String, ClassRedirection> redirects = new HashMap<>();

	static {
		redirects.put("net/minecraft/class_1761",
				new ClassRedirection("net/patchworkmc/api/registries/PatchworkItemGroup")
						.with("method_7750()Lnet/minecraft/class_1799;", "patchwork$createIconHack"));
		redirects.put("net/minecraft/class_304", new ClassRedirection("net/patchworkmc/api/keybindings/PatchworkKeyBinding"));
	}

	@Override
	protected void transform(ClassNode node) {
		ClassRedirection redirection = redirects.get(node.superName);

		if (redirection != null) {
			node.superName = redirects.get(node.superName).newName;

			for (MethodNode method : node.methods) {
				String rename = redirection.entries.get(method.name + method.desc);

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
				AbstractInsnNode prev = rewindMethod(min);

				if (prev instanceof TypeInsnNode && prev.getOpcode() == Opcodes.NEW && ((TypeInsnNode) prev).desc.equals(min.owner)) {
					((TypeInsnNode) prev).desc = classRedirection.newName;
				} else {
					if (!method.name.equals("<init>")) {
						throw new UnsupportedOperationException("Could not locate NEW instruction for method via rewindMethod");
					}
				}

				min.owner = classRedirection.newName;
			}
		}
	}
}
