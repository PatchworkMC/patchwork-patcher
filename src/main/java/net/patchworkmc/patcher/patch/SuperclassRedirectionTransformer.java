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
	private static final Map<String, Redirection> redirects = new HashMap<>();

	static {
		// redirects should be added in the form
		// redirects.put("net/minecraft/intermediary", new Redirection("net/patchworkmc/WrapperClass","(Forge's <init> descriptor)V"))
	}

	@Override
	protected void transform(ClassNode node) {
		if (redirects.containsKey(node.superName)) {
			node.superName = redirects.get(node.superName).name;
		}

		node.methods.forEach(this::transformMethod);
	}

	private void transformMethod(MethodNode method) {
		for (AbstractInsnNode in : method.instructions) {
			if (!(in instanceof MethodInsnNode)) {
				continue;
			}

			MethodInsnNode min = (MethodInsnNode) in;

			if (min.getOpcode() == Opcodes.INVOKESPECIAL && min.name.equals("<init>") && redirects.containsKey(min.owner)) {
				Redirection redirect = redirects.get(min.owner);

				if (min.desc.equals(redirect.initializerDescriptor)) {
					AbstractInsnNode prev = min;

					while (prev != null) {
						prev = prev.getPrevious();

						if (prev instanceof TypeInsnNode && prev.getOpcode() == Opcodes.NEW && ((TypeInsnNode) prev).desc.equals(min.owner)) {
							((TypeInsnNode) prev).desc = redirect.name;
							break;
						}
					}

					min.owner = redirect.name;
				}
			}
		}
	}

	private static class Redirection {
		public final String name;
		public final String initializerDescriptor;

		Redirection(String name, String initializerDescriptor) {
			this.name = name;
			this.initializerDescriptor = initializerDescriptor;
		}
	}
}
