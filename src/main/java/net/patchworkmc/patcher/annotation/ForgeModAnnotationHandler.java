package net.patchworkmc.patcher.annotation;

import java.util.function.Consumer;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

import net.patchworkmc.patcher.ForgeModJar;

public class ForgeModAnnotationHandler extends AnnotationVisitor {
	private final ForgeModJar jar;
	private final String className;
	private final Consumer<String> modidConsumer;
	private boolean visited;

	public ForgeModAnnotationHandler(ForgeModJar jar, String className, Consumer<String> modidConsumer) {
		super(Opcodes.ASM9);
		this.jar = jar;
		this.className = className;
		this.modidConsumer = modidConsumer;
	}

	@Override
	public void visit(String name, Object value) {
		super.visit(name, value);

		if (name.equals("value")) {
			jar.addEntrypoint("patchwork:mod_instance", className);
			this.modidConsumer.accept((String) value);
			visited = true;
		} else {
			throw new IllegalArgumentException("Unexpected mod annotation property: " + name + " (expected " + name + ") ->" + value);
		}
	}

	@Override
	public void visitEnd() {
		super.visitEnd();

		if (!visited) {
			throw new IllegalStateException("Mod annotation is missing a value!");
		}
	}
}
