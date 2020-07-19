package net.patchworkmc.patcher.access;

import java.util.HashSet;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import net.patchworkmc.patcher.util.MemberIdentifier;

public class ClassAccessWidenings {
	private boolean makeClassPublic = false;
	private final HashSet<MemberIdentifier> definalizedFields = new HashSet<>();
	private final HashSet<MemberIdentifier> publicMethods = new HashSet<>();

	public void makeClassPublic() {
		this.makeClassPublic = true;
	}

	public void definalizeField(String name, String descriptor) {
		definalizedFields.add(new MemberIdentifier(name, descriptor));
	}

	public void makeMethodPublic(String name, String descriptor) {
		publicMethods.add(new MemberIdentifier(name, descriptor));
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

		for (MethodNode method: clazz.methods) {
			if (publicMethods.contains(new MemberIdentifier(method.name, method.desc))) {
				method.access &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED);
				method.access |= Opcodes.ACC_PUBLIC;
			}
		}
	}
}
