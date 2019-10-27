package net.coderbot.patchwork.at;

import net.coderbot.patchwork.manifest.forge.AccessTransformerEntry;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.*;

public class ModScanner extends ClassVisitor {
	private List<AccessTransformerEntry> accessTransformerEntries;
	public List<Meta> metas = new ArrayList<>();
	public ModScanner(List<AccessTransformerEntry> list, ClassVisitor parent) {
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

		return new ModScannerMethodVisitor(
				super.visitMethod(access, name, descriptor, signature, exceptions));
	}

	private class ModScannerMethodVisitor extends MethodVisitor {

		public ModScannerMethodVisitor(MethodVisitor parent) {
			super(Opcodes.ASM7, parent);
		}
		boolean callsTarget = true;
		@Override
		public void visitMethodInsn(int opcode,
				String owner,
				String name,
				String descriptor,
				boolean isInterface) {
			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

			for(AccessTransformerEntry accessTransformerEntry : accessTransformerEntries) {
				if(name.equals(accessTransformerEntry.getMemberName()) &&
						descriptor.equals(
								accessTransformerEntry.getMemberDescription())) { // fixme see below
					callsTarget = true;
					System.out.println("Found " + owner + " " + name + descriptor);
					metas.add(new Meta(owner, name, descriptor, opcode, accessTransformerEntry));
					return;
				}
			}
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
			super.visitFieldInsn(opcode, owner, name, descriptor);
			for(AccessTransformerEntry accessTransformerEntry : accessTransformerEntries) {
				if(name.equals(accessTransformerEntry
									   .getMemberName())) { // fixme properly determine inheritance
					callsTarget = true;
					System.out.println("Found " + owner + " " + name + " " + descriptor);
					metas.add(new Meta(owner, name, descriptor, opcode, accessTransformerEntry));
					return;
				}
			}
		}

		@Override
		public void visitEnd() {

			super.visitEnd();
		}
	}
}
