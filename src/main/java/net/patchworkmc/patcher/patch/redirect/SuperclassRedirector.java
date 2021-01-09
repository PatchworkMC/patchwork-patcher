package net.patchworkmc.patcher.patch.redirect;

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
import net.patchworkmc.patcher.util.IllegalArgumentError;

/**
 * Redirects {@code public class Bar extends Foo} to {@code public class Bar extends Different},
 * and can also rename inherited methods.
 */
public class SuperclassRedirector extends NodeTransformer {
	/**
	 * The format for ClassRedirection entries is {@code method name + desc -> method name}.
	 * <br>If a value is not provided an {@link net.patchworkmc.patcher.util.IllegalArgumentError} will be thrown.
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
			redirection.assertNoSetEntries();

			node.superName = redirects.get(node.superName).newOwner;

			for (MethodNode method : node.methods) {
				if (((method.access & Opcodes.ACC_STATIC) != 0)) {
					throw new IllegalArgumentError(String.format("ClassRedirection for class %s->%s targeting method %s "
									+ "seems to target a static method (found in class %s)!", node.superName, redirection.newOwner,
							method.name + method.desc, node.name));
				}

				String rename = redirection.mapEntries.get(method.name + method.desc);

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
					((TypeInsnNode) prev).desc = classRedirection.newOwner;
				} else {
					if (!method.name.equals("<init>")) {
						throw new UnsupportedOperationException("Could not locate NEW instruction for method via rewindMethod");
					}
				}

				min.owner = classRedirection.newOwner;
			}
		}
	}
}
