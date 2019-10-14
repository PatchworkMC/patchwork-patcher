package net.coderbot.patchwork.access;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ModAccessTransformer extends ClassVisitor {

	private ModAccessTransformations transformations;

	public ModAccessTransformer(ClassVisitor parent, ModAccessTransformations transformations) {
		super(Opcodes.ASM7, parent);

		this.transformations = transformations;
	}

	@Override
	public void visit(int version,
			int access,
			String name,
			String signature,
			String superName,
			String[] interfaces) {
		access = transformations.getClassTransformation().apply(access);

		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public FieldVisitor visitField(int access,
			String name,
			String descriptor,
			String signature,
			Object value) {

		access = transformations.getFieldTransformation(name).apply(access);

		return super.visitField(access, name, descriptor, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access,
			String name,
			String descriptor,
			String signature,
			String[] exceptions) {

		access = transformations.getMethodTransformation(name, descriptor).apply(access);

		return super.visitMethod(access, name, descriptor, signature, exceptions);
	}
}
