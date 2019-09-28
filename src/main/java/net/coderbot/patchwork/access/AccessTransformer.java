package net.coderbot.patchwork.access;

import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

public class AccessTransformer extends ClassVisitor {

	private Map<String, AccessTransformation> fieldTransformers;

	public AccessTransformer(ClassVisitor parent,
			Map<String, AccessTransformation> fieldTransformers) {
		super(Opcodes.ASM7, parent);

		this.fieldTransformers = fieldTransformers;
	}

	@Override
	public FieldVisitor visitField(int access,
			String name,
			String descriptor,
			String signature,
			Object value) {
		AccessTransformation transformation = fieldTransformers.get(name);

		if(transformation != null) {
			access &= (~transformation.getRemoved());
			access |= transformation.getAdded();
		}

		return super.visitField(access, name, descriptor, signature, value);
	}
}
