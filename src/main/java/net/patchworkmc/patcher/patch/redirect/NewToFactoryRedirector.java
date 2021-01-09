package net.patchworkmc.patcher.patch.redirect;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import net.patchworkmc.patcher.patch.util.ClassRedirection;
import net.patchworkmc.patcher.transformer.NodeTransformer;

public class NewToFactoryRedirector extends NodeTransformer {
	/**
	 * The ClassRedirection format is {@code initializer descriptor -> factory method name}.
	 * <br>If a value is not provided an {@link net.patchworkmc.patcher.util.IllegalArgumentError} will be thrown.
	 */
	private static final Map<String, ClassRedirection> redirects = new HashMap<>();

	static {
		redirects.put("net/minecraft/class_1799", new ClassRedirection("net/patchworkmc/api/capability/PatchworkItemStack")
				.with("(Lnet/minecraft/class_1935;ILnet/minecraft/class_2487;)V", "of"));
	}

	@Override
	protected void transform(ClassNode node) {
		for (MethodNode method : node.methods) {
			this.forEachMethodInsn(method, this::redirect);
		}
	}

	private void redirect(MethodNode node, MethodInsnNode insn) {
		ClassRedirection classRedirection = redirects.get(insn.owner);

		if (classRedirection != null && classRedirection.contains(insn.desc) && insn.name.equals("<init>")) {
			classRedirection.assertNoSetEntries();

			String methodRedirect = classRedirection.mapEntries.get(insn.desc);

			AbstractInsnNode previous = rewindMethod(insn);
			Objects.requireNonNull(previous, "rewindMethod returned null");

			if (skipUselessInstructionsForwards(previous.getNext()).getOpcode() == Opcodes.DUP) {
				// there might be a case where this causes issues
				node.instructions.remove(previous.getNext());
			}

			if (previous.getOpcode() == Opcodes.NEW) {
				TypeInsnNode check = (TypeInsnNode) previous;

				if (!check.desc.equals(insn.owner)) {
					throw new UnsupportedOperationException(String.format("Expected %s for NEW opcode, got %s.", insn.owner, check.desc));
				}

				node.instructions.remove(previous);
				insn.desc = insn.desc.replace(")V", ")" + insn.owner);
				insn.owner = classRedirection.newOwner;
				insn.name = methodRedirect;
				insn.setOpcode(Opcodes.INVOKESTATIC);
			} else {
				throw new UnsupportedOperationException(String.format("Could not find NEW opcode for %s::<init>%s", insn.owner, insn.desc));
			}
		}
	}
}
