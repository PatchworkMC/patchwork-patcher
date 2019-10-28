package net.coderbot.patchwork.at;

import net.coderbot.patchwork.manifest.forge.AccessTransformerEntry;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ModGutter extends ClassVisitor {
	private List<AccessTransformerEntry> accessTransformerEntries;
	public List<Meta> metas = new ArrayList<>();
	public ModGutter(List<AccessTransformerEntry> list, ClassVisitor parent) {
		super(Opcodes.ASM7, parent);
		this.accessTransformerEntries = list;
	}
	public static class Meta {
		private String owner;
		private String name;
		private String descriptor;
		private int opcode;
		private AccessTransformerEntry accessTransformerEntry;
		private Meta(String owner,
				String name,
				String descriptor,
				int opcode,
				AccessTransformerEntry entry) {
			this.name = name;
			this.owner = owner;
			this.descriptor = descriptor;
			this.opcode = opcode;
			this.accessTransformerEntry = entry;
		}

		public String getName() {
			return name;
		}

		public int getOpcode() {
			return opcode;
		}

		public String getOwner() {
			return owner;
		}

		public String getDescriptor() {
			return descriptor;
		}

		public AccessTransformerEntry getAccessTransformerEntry() {
			return accessTransformerEntry;
		}
	}
	@Override
	public void visit(int version,
			int access,
			String name,
			String signature,
			String superName,
			String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access,
			String name,
			String descriptor,
			String signature,
			String[] exceptions) {

		return new ModGutterMethodVisitor(
				super.visitMethod(access, name, descriptor, signature, exceptions));
	}

	private class ModGutterMethodVisitor extends MethodVisitor {

		public ModGutterMethodVisitor(MethodVisitor parent) {
			super(Opcodes.ASM7, parent);
		}
		boolean callsTarget = false;
		@Override
		public void visitMethodInsn(int opcode,
				String owner,
				String name,
				String descriptor,
				boolean isInterface) {
			visitMemberInsn(opcode, owner, name, descriptor);
			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		}
		private void visitMemberInsn(int opcode, String owner, String name, String descriptor) {
			for(AccessTransformerEntry accessTransformerEntry : accessTransformerEntries) {
				if(name.equals(accessTransformerEntry.getMemberName())) {
					callsTarget = true;
					System.out.println("Found " + owner + " " + name + " " + descriptor +
									   " with opcode " + opcode);
					metas.add(new Meta(owner, name, descriptor, opcode, accessTransformerEntry));
					return;
				} else {
					callsTarget = false;
				}
			}
		}
		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
			visitMemberInsn(opcode, owner, name, descriptor);
			if(!callsTarget) {
				super.visitFieldInsn(opcode, owner, name, descriptor);
				return;
			}
			String mixinName = "patchwork_generated/" + name + "AccessorMixin";
			if(opcode == Opcodes.PUTSTATIC) {
				String methodName = "set" + name;
				String methodDescriptor = "(" + descriptor + ")V";
				super.visitMethodInsn(
						Opcodes.INVOKESTATIC, mixinName, methodName, methodDescriptor, false);
			} else if(opcode == Opcodes.PUTFIELD) {
				String methodName = "set" + name;
				String methodDescriptor = "(" + descriptor + ")V";
				super.visitMethodInsn(
						Opcodes.INVOKEINTERFACE, mixinName, methodName, methodDescriptor, true);
			} else if(opcode == Opcodes.GETSTATIC) {
				String methodName = "get" + name;
				String methodDescriptor = "()" + descriptor;
				super.visitMethodInsn(
						Opcodes.INVOKESTATIC, mixinName, methodName, methodDescriptor, false);
			} else if(opcode == Opcodes.GETFIELD) {
				String methodName = "get" + name;
				String methodDescriptor = "()" + descriptor;
				super.visitMethodInsn(
						Opcodes.INVOKEINTERFACE, mixinName, methodName, methodDescriptor, true);
			}
		}

		@Override
		public void visitEnd() {

			super.visitEnd();
		}
	}
}
