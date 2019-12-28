package com.patchworkmc.event.generator;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.patchworkmc.event.SubscribeEvent;
import com.patchworkmc.generator.ConsumerGenerator;

public class SubscribeEventGenerator {
	public static String generate(String targetClass, SubscribeEvent entry, ClassVisitor visitor) {
		boolean instance = (entry.getAccess() & Opcodes.ACC_STATIC) == 0;

		String targetDescriptor = "L" + targetClass + ";";
		String eventDescriptor = "L" + entry.getEventClass() + ";";

		String signature = entry.getGenericClass().map(genericClass -> "L" + entry.getEventClass() + "<L" + genericClass + ";>;").orElse(null);

		String shimName = "patchwork_generated/" + targetClass + "_SubscribeEvent_" + entry.getMethod();

		ConsumerGenerator generator = new ConsumerGenerator(visitor, shimName, eventDescriptor, signature);

		// Add a default constructor

		if (instance) {
			// TODO: Generic parents? They should have a signature here

			generator.getVisitor().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "instance", targetDescriptor, null, null);

			MethodVisitor constructor = generator.visitConstructor("(" + targetDescriptor + ")V", null);

			constructor.visitVarInsn(Opcodes.ALOAD, 0);
			constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

			constructor.visitVarInsn(Opcodes.ALOAD, 0);
			constructor.visitVarInsn(Opcodes.ALOAD, 1);
			constructor.visitFieldInsn(Opcodes.PUTFIELD, shimName, "instance", targetDescriptor);

			constructor.visitInsn(Opcodes.RETURN);

			constructor.visitMaxs(2, 2);
			constructor.visitEnd();
		} else {
			generator.visitDefaultConstructor();
		}

		// Add the accept implementation
		MethodVisitor method = generator.visitAccept();

		if (instance) {
			// load "this.instance" onto the stack

			method.visitVarInsn(Opcodes.ALOAD, 0);
			method.visitFieldInsn(Opcodes.GETFIELD, shimName, "instance", targetDescriptor);
		}

		method.visitVarInsn(Opcodes.ALOAD, 1);

		method.visitMethodInsn(instance ? Opcodes.INVOKEVIRTUAL : Opcodes.INVOKESTATIC, targetClass, entry.getMethod(), entry.getMethodDescriptor(), false);

		method.visitInsn(Opcodes.RETURN);

		method.visitMaxs(instance ? 2 : 1, 2);
		method.visitEnd();

		// Add the bridge method and finish the visitor
		generator.visitEnd();

		return shimName;
	}
}
