package net.patchworkmc.patcher.annotation;

import java.util.function.Consumer;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Handles annotations containing a single required string value.
 */
public class StringAnnotationHandler extends AnnotationVisitor {
	private Consumer<String> valueConsumer;
	private String expected;
	private boolean visited;

	public StringAnnotationHandler(Consumer<String> value) {
		this("value", value);
	}

	public StringAnnotationHandler(AnnotationVisitor parent, Consumer<String> value) {
		this(parent, "value", value);
	}

	public StringAnnotationHandler(String expected, Consumer<String> value) {
		this(null, expected, value);
	}

	public StringAnnotationHandler(AnnotationVisitor parent, String expected, Consumer<String> value) {
		super(Opcodes.ASM9, parent);

		this.valueConsumer = value;
		this.expected = expected;
		this.visited = false;
	}

	@Override
	public void visit(String name, Object value) {
		super.visit(name, value);

		if (name.equals(expected)) {
			valueConsumer.accept(value.toString());
			visited = true;
		} else {
			throw new IllegalArgumentException("Unexpected string annotation property: " + name + " (expected " + expected + ") ->" + value);
		}
	}

	@Override
	public void visitEnd() {
		super.visitEnd();

		if (!visited) {
			throw new IllegalStateException("String annotation is missing a value!");
		}
	}
}
