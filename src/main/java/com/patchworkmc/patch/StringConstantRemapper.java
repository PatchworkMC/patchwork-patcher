package com.patchworkmc.patch;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.patchworkmc.Patchwork;
import com.patchworkmc.mapping.remapper.AmbiguousMappingException;
import com.patchworkmc.mapping.remapper.PatchworkRemapper;

/**
 * Remaps Strings into Intermediary. Necessary for things like reflection and ObfuscationRemapperHelper,
 * unless we put SRG and tiny-remapper on the classpath at runtime.
 */
public class StringConstantRemapper extends ClassVisitor {
	private PatchworkRemapper.Naive remapper;

	public StringConstantRemapper(ClassVisitor classVisitor, PatchworkRemapper.Naive remapper) {
		super(Opcodes.ASM7, classVisitor);
		this.remapper = remapper;
	}

	/**
	 * Remaps strings in fields.
	 */
	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		if (value instanceof String) {
			value = remap((String) value);
		}

		return super.visitField(access, name, descriptor, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		return new MethodTransformer(super.visitMethod(access, name, descriptor, signature, exceptions));
	}

	private String remap(String name) {
		// if unable to remap these methods return the names they received.
		if (name.startsWith("field_")) {
			name = remapper.getField(name);
		} else if (name.startsWith("func_")) {
			try {
				name = remapper.getMethod(name);
			} catch (AmbiguousMappingException e) {
				Patchwork.LOGGER.warn("Failed to remap string constant: %s", e.getMessage());

				return name;
			}
		} else {
			name = remapper.getClass(name);
		}

		return name;
	}

	/**
	 * Remaps LDC calls (string construction) in method bytecode.
	 */
	private class MethodTransformer extends MethodVisitor {
		private MethodTransformer(MethodVisitor methodVisitor) {
			super(Opcodes.ASM7, methodVisitor);
		}

		@Override
		public void visitLdcInsn(Object value) {
			if (value instanceof String) {
				value = remap((String) value);
			}

			super.visitLdcInsn(value);
		}
	}
}
