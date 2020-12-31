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

public class NewToFactoryTransformer extends NodeTransformer {
	/**
	 * ClassRedirection format for values are descriptor of initializer -> factory method name
	 */
	private static final Map<String, ClassRedirection> redirects = new HashMap<>();

	static {
		redirects.put("net/minecraft/class_1799", new ClassRedirection("net/patchworkmc/api/capability/PatchworkItemStack")
				.with("(Lnet/minecraft/class_1935;ILnet/minecraft/class_2487;)V", "of"));
	}

	@Override
	protected void transform(ClassNode node) {
		for (MethodNode method : node.methods) {
			transformMethod(method);
		}
	}

	private void transformMethod(MethodNode node) {
		for (AbstractInsnNode instruction : node.instructions) {
			if (!(instruction instanceof MethodInsnNode)) {
				continue;
			}

			MethodInsnNode insn = (MethodInsnNode) instruction;
			ClassRedirection classRedirection = redirects.get(insn.owner);

			if (classRedirection != null && insn.name.equals("<init>")) {
				String methodRedirect = classRedirection.entries.get(insn.desc);

				if (methodRedirect != null) {
					insn.owner = classRedirection.newName;
					insn.name = methodRedirect;
				}

				// Rewind time
				AbstractInsnNode previous = insn.getPrevious();

				if (previous.getOpcode() == Opcodes.DUP) {
					node.instructions.remove(previous);
					previous = previous.getPrevious();
				}

				if (previous.getOpcode() == Opcodes.NEW) {
					TypeInsnNode check = (TypeInsnNode) previous;

					if (!check.desc.equals(insn.owner)) {
						throw new AssertionError(String.format("Expected %s for NEW opcode, got %s.", insn.owner, check.desc));
					}

					node.instructions.remove(previous);
				} else {
					throw new AssertionError(String.format("Could not find NEW opcode for %s::<init>%s", insn.owner, insn.desc));
				}
			}
		}
	}
}
