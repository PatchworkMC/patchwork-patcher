package com.patchworkmc.event;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.patchworkmc.Patchwork;

/**
 * Processes Cancelable and HasResult annotations, strips getListenerList and getParentListenerList
  */
public class EventSubclassTransformer extends ClassVisitor {
	private static final String CANCELABLE = "Lnet/minecraftforge/eventbus/api/Cancelable;";
	private static final String IS_CANCELABLE = "isCancelable";
	private static final String IS_CANCELABLE_DESCRIPTOR = "()Z";
	private static final String GET_LISTENER_LIST = "getListenerList";
	private static final String GET_PARENT_LISTENER_LIST = "getParentListenerList";
	private static final String GET_LISTENER_LIST_DESCRIPTOR = "()Lnet/minecraftforge/eventbus/ListenerList;";

	private boolean hasCancelable;
	private boolean cancelable;
	private String className;

	public EventSubclassTransformer(ClassVisitor parent) {
		super(Opcodes.ASM7, parent);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);

		this.className = name;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (descriptor.equals(CANCELABLE)) {
			cancelable = true;

			return null;
		}

		return super.visitAnnotation(descriptor, visible);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (name.equals(IS_CANCELABLE) && descriptor.equals(IS_CANCELABLE_DESCRIPTOR)) {
			hasCancelable = true;
		}

		// Strip getListenerList and getParentListenerList to significantly simplify the logic in Patchwork EventBus.
		if (name.equals(GET_LISTENER_LIST) && descriptor.equals(GET_LISTENER_LIST_DESCRIPTOR)) {
			Patchwork.LOGGER.warn("Stripping %s from %s (an assumed Event class)", GET_LISTENER_LIST, className);

			return null;
		}

		if (name.equals(GET_PARENT_LISTENER_LIST) && descriptor.equals(GET_LISTENER_LIST_DESCRIPTOR)) {
			Patchwork.LOGGER.warn("Stripping %s from %s (an assumed Event class)", GET_PARENT_LISTENER_LIST, className);

			return null;
		}

		return super.visitMethod(access, name, descriptor, signature, exceptions);
	}

	@Override
	public void visitEnd() {
		if (cancelable && !hasCancelable) {
			visitCancelable();
		}

		super.visitEnd();
	}

	/**
	 * Adds the following code:
	 * <pre>
	 * public boolean isCancelable() {
	 *     return true;
	 * }
	 * </pre>
	 */
	private void visitCancelable() {
		MethodVisitor isCancelable = super.visitMethod(Opcodes.ACC_PUBLIC, IS_CANCELABLE, IS_CANCELABLE_DESCRIPTOR, null, null);

		if (isCancelable != null) {
			AnnotationVisitor override = isCancelable.visitAnnotation("Ljava/lang/Override;", true);

			if (override != null) {
				override.visitEnd();
			}

			isCancelable.visitInsn(Opcodes.ICONST_1);
			isCancelable.visitInsn(Opcodes.IRETURN);
			isCancelable.visitMaxs(2, 0);
			isCancelable.visitEnd();
		}
	}
}
