package net.coderbot.patchwork.annotation;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Rewrites @OnlyIn(Dist) annotations to use @Environment(EnvType)
 */
public class OnlyInRewriter extends AnnotationVisitor {
	public static final String TARGET_DESCRIPTOR = "Lnet/fabricmc/api/Environment;";
	private static final String ENVTYPE_DESCRIPTOR = "Lnet/fabricmc/api/EnvType;";

	public OnlyInRewriter(AnnotationVisitor parent) {
		super(Opcodes.ASM7, parent);
	}

	@Override
	public void visitEnum(final String name, final String descriptor, String value) {
		if(!name.equals("value")) {
			System.err.println(
					"Unexpected OnlyIn enum property: " + name + "->" + descriptor + "::" + value);

			return;
		}

		if(!descriptor.equals("Lnet/minecraftforge/api/distmarker/Dist;")) {
			System.out.println(
					"Unexpected descriptor for OnlyIn dist property, continuing anyways: " +
					descriptor);
		}

		// Fabric uses SERVER in their EnvType.

		if(value.equals("DEDICATED_SERVER")) {
			value = "SERVER";
		}

		super.visitEnum(name, ENVTYPE_DESCRIPTOR, value);
	}
}
