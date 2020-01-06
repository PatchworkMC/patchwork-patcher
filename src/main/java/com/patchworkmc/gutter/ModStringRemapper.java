package com.patchworkmc.gutter;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.patchworkmc.mapping.SimpleVoldeToIntermediaryRemapper;

public class ModStringRemapper extends ClassVisitor {
	SimpleVoldeToIntermediaryRemapper remapper;
	public ModStringRemapper(ClassVisitor classVisitor, SimpleVoldeToIntermediaryRemapper remapper) {
		super(Opcodes.ASM7, classVisitor);
		this.remapper = remapper;
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		if(value instanceof String) {
			value = remap((String) value);
		}
		return super.visitField(access, name, descriptor, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		return new MethodTransformer(super.visitMethod(access, name, descriptor, signature, exceptions));
	}

	private class MethodTransformer extends MethodVisitor {
		public MethodTransformer(MethodVisitor methodVisitor) {
			super(Opcodes.ASM7, methodVisitor);
		}

		@Override
		public void visitLdcInsn(Object value) {
			if(value instanceof String) {
				value = remap((String) value);
			}
			super.visitLdcInsn(value);
		}
	}

	private String remap(String name) {
		if(name.startsWith("field_")) {
			name = remapper.getField(name);
		} else if (name.startsWith("func_")){
			name = remapper.getMethod(name);
		} else {
			// Loader uses '.' in remapping but the IMappingsProvider uses "/"
			name = remapper.getClass(name).replace('/', '.');
		}
		return name;
	}
}
