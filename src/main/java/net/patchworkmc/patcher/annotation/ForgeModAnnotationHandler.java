package net.patchworkmc.patcher.annotation;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

import net.patchworkmc.patcher.ForgeModJar;
import net.patchworkmc.patcher.transformer.api.ClassPostTransformer;

public class ForgeModAnnotationHandler extends AnnotationVisitor {
	private final ForgeModJar jar;
	private final String className;
	private final ClassPostTransformer transformer;
	private boolean visited;

	public ForgeModAnnotationHandler(ForgeModJar jar, String className, ClassPostTransformer transformer) {
		super(Opcodes.ASM9);
		this.jar = jar;
		this.className = className;
		this.transformer = transformer;
	}

	@Override
	public void visit(String name, Object value) {
		super.visit(name, value);

		if (name.equals("value")) {
			jar.addEntrypoint("patchwork:modInstance:" + value, className);
			transformer.addInterface("net/patchworkmc/api/ModInstance");
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
