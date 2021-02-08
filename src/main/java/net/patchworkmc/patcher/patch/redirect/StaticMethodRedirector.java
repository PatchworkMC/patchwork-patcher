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

public class StaticMethodRedirector extends NodeTransformer {
	/**
	 * ClassRedirection format for {@code with} is {@code method name + desc -> method name}.
	 * If a value is not provided it is assumed the method name is the same.
	 */
	private static final Map<String, ClassRedirection> redirects = new HashMap<>();

	static {
		redirects.put("net/minecraft/class_1743", new ClassRedirection("net/patchwork/api/block/PatchworkAxeItem")
				.with("getAxeStrippingState(Lnet/minecraft/class_2680;)Lnet/minecraft/class_2680;"));
		redirects.put("net/minecraft/class_1794", new ClassRedirection("net/patchworkmc/api/block/PatchworkHoeItem")
				.with("getHoeTillingState(Lnet/minecraft/class_2680;)Lnet/minecraft/class_2680;"));
		redirects.put("net/minecraft/class_1821", new ClassRedirection("net/minecraft/api/block/PatchworkShovelItem")
				.with("getShovelPathingState(Lnet/minecraft/class_2680;)Lnet/minecraft/class_2680;"));
	}

	@Override
	protected void transform(ClassNode node) {
		for (MethodNode method : node.methods) {
			forEachMethodInsn(method, this::redirect);
		}
	}

	private void redirect(MethodNode node, MethodInsnNode insn) {
		ClassRedirection classRedirection = redirects.get(insn.owner);

		if (classRedirection != null && classRedirection.contains(insn.name + insn.desc)) {
			if (insn.getOpcode() != Opcodes.INVOKESTATIC) {
				throw new IllegalArgumentError(String.format("ClassRedirection for class %s->%s targeting method %s "
						+ "expected opcode INVOKESTATIC, got %s", insn.owner, classRedirection.newOwner,
						insn.name + insn.desc, Integer.toBinaryString(insn.getOpcode())));
			}

			String newMethodName = classRedirection.mapEntries.get(insn.name + insn.desc);

			if (newMethodName != null) {
				newMethodName = insn.name;
			}

			insn.owner = classRedirection.newOwner;
			insn.name = newMethodName;
		}
	}
}
