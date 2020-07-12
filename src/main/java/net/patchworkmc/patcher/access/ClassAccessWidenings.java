package net.patchworkmc.patcher.access;

import java.util.HashSet;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import net.patchworkmc.patcher.util.MemberIdentifier;

public class ClassAccessWidenings {
	private boolean makeClassPublic;
	private final HashSet<MemberIdentifier> definalizedFields;

	public ClassAccessWidenings() {
		makeClassPublic = false;
		definalizedFields = new HashSet<>();
	}

	public void makeClassPublic() {
		this.makeClassPublic = true;
	}

	public void definalizeField(String name, String descriptor) {
		definalizedFields.add(new MemberIdentifier(name, descriptor));
	}

	public void apply(ClassNode clazz) {
		if (makeClassPublic) {
			clazz.access &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED);
			clazz.access |= Opcodes.ACC_PUBLIC;
		}

		if (definalizedFields.isEmpty()) {
			return;
		}

		for (FieldNode field: clazz.fields) {
			if (definalizedFields.contains(new MemberIdentifier(field.name, field.desc))) {
				field.access &= ~Opcodes.ACC_FINAL;
			}
		}
	}
}
