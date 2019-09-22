package net.coderbot.patchwork.annotation;

import org.objectweb.asm.AnnotationVisitor;
import org.spongepowered.asm.lib.Opcodes;

import java.util.Optional;

public class Mod {
	private String modId;

	private Mod() {
		this.modId = null;
	}

	public Mod(String modId) {
		this.modId = modId;
	}

	public Optional<String> getModId() {
		return Optional.ofNullable(modId);
	}

	@Override
	public String toString() {
		return "Mod{" +
				"modId='" + modId + '\'' +
				'}';
	}

	public static class Handler extends AnnotationVisitor {
		Mod instance;

		public Handler(ForgeAnnotations target) {
			super(Opcodes.ASM7);

			instance = new Mod();

			target.mod = instance;
		}

		@Override
		public void visit(String name, Object value) {
			super.visit(name, value);

			System.out.println(name + "->" + value);

			if (name.equals("value")) {
				instance.modId = value.toString();
			}
		}
	}
}
