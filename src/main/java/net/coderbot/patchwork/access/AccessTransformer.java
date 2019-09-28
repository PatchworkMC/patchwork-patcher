package net.coderbot.patchwork.access;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class AccessTransformer extends ClassVisitor {

	private AccessTransformations transformations;

	public AccessTransformer(ClassVisitor parent, AccessTransformations transformations) {
		super(Opcodes.ASM7, parent);

		this.transformations = transformations;
	}

	@Override
	public FieldVisitor visitField(int access,
			String name,
			String descriptor,
			String signature,
			Object value) {

		AccessTransformation transformation = transformations.getFieldTransformation(name);

		access &= (~transformation.getRemoved());
		access |= transformation.getAdded();

		return super.visitField(access, name, descriptor, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access,
			String name,
			String descriptor,
			String signature,
			String[] exceptions) {

		AccessTransformation transformation =
				transformations.getMethodTransformation(name, descriptor);

		access &= (~transformation.getRemoved());
		access |= transformation.getAdded();

		return super.visitMethod(access, name, descriptor, signature, exceptions);
	}
}
