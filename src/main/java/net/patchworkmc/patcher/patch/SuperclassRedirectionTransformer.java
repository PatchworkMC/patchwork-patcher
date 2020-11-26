package net.patchworkmc.patcher.patch;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import net.patchworkmc.patcher.ForgeModJar;
import net.patchworkmc.patcher.transformer.NodeBasedMethodAdapter;
import net.patchworkmc.patcher.transformer.api.ClassPostTransformer;
import net.patchworkmc.patcher.transformer.api.Transformer;
import net.patchworkmc.patcher.util.MinecraftVersion;

public class SuperclassRedirectionTransformer extends Transformer {
	private static final Map<String, Redirection> redirects = new HashMap<>();

	static {
		// redirects should be added in the form
		// redirects.put("net/minecraft/intermediary", new Redirection("net/patchworkmc/WrapperClass","(Forge's <init> descriptor)V"))
	}

	public SuperclassRedirectionTransformer(MinecraftVersion version, ForgeModJar jar, ClassVisitor parent, ClassPostTransformer postTransformer) {
		super(version, jar, parent, postTransformer);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if (redirects.containsKey(superName)) {
			superName = redirects.get(superName).name;
		}

		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
		return new MethodTransformer(access, name, descriptor, signature, exceptions, mv);
	}

	private static class MethodTransformer extends NodeBasedMethodAdapter {
		MethodTransformer(int access, String name, String desc, String signature, String[] exceptions, MethodVisitor methodVisitor) {
			super(access, name, desc, signature, exceptions, methodVisitor);
		}

		@Override
		public void transform(MethodNode mn) {
			for (AbstractInsnNode in : mn.instructions) {
				if (in instanceof MethodInsnNode) {
					MethodInsnNode min = (MethodInsnNode) in;

					if (min.getOpcode() == Opcodes.INVOKESPECIAL && min.name.equals("<init>")) {
						if (redirects.containsKey(min.owner)) {
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
