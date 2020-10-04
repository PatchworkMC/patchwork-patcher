package net.patchworkmc.patcher.transformer.api;

import java.util.Arrays;
import java.util.HashSet;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import net.patchworkmc.patcher.util.MemberIdentifier;

/**
 * A limited set of transformations an {@link Transformer} can that will be applied after visiting.
 * This is intended to reduce the need of having multiple transformation passes on a class.
 */
public class ClassPostTransformer {
	private boolean makeClassPublic = false;
	private final HashSet<MemberIdentifier> definalizedFields = new HashSet<>();
	private final HashSet<String> addedInterfaces = new HashSet<>();
	public void makeClassPublic() {
		this.makeClassPublic = true;
	}

	public void definalizeField(String name, String descriptor) {
		definalizedFields.add(new MemberIdentifier(name, descriptor));
	}

	public void addInterface(String... names) {
		this.addedInterfaces.addAll(Arrays.asList(names));
	}

	void apply(ClassNode clazz) {
		if (makeClassPublic) {
			clazz.access &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED);
			clazz.access |= Opcodes.ACC_PUBLIC;
		}

		for (FieldNode field : clazz.fields) {
			if (definalizedFields.contains(new MemberIdentifier(field.name, field.desc))) {
				field.access &= ~Opcodes.ACC_FINAL;
			}
		}

		clazz.interfaces.addAll(addedInterfaces);
	}
}
