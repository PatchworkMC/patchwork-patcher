package net.coderbot.patchwork.annotation;

import org.objectweb.asm.AnnotationVisitor;
import org.spongepowered.asm.lib.Opcodes;

import java.util.Optional;
import java.util.function.Consumer;

public class ObjectHolder {
	private int access;
	private String descriptor;
	private String signature;
	private String identifier;

	private ObjectHolder() {
		this.identifier = null;
	}

	public ObjectHolder(int access, String descriptor, String signature, String identifier) {
		this.access = access;
		this.descriptor = descriptor;
		this.signature = signature;
		this.identifier = identifier;
	}

	public Optional<String> getIdentifier() {
		return Optional.ofNullable(identifier);
	}

	public int getAccess() {
		return access;
	}

	public String getDescriptor() {
		return descriptor;
	}

	public String getSignature() {
		return signature;
	}

	@Override
	public String toString() {
		return "ObjectHolder{" +
				"access=" + access +
				", descriptor='" + descriptor + '\'' +
				", signature='" + signature + '\'' +
				", identifier='" + identifier + '\'' +
				'}';
	}

	public static class Handler extends AnnotationVisitor {
		Consumer<String> valueConsumer;

		public Handler(Consumer<String> value) {
			super(Opcodes.ASM7);

			this.valueConsumer = value;
		}

		@Override
		public void visit(String name, Object value) {
			super.visit(name, value);

			if (name.equals("value")) {
				valueConsumer.accept(value.toString());
			} else {
				System.err.println("Unexpected ObjectHolder property: " + name + "->" + value);
			}
		}
	}
}
