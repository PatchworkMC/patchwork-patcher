package net.patchworkmc.patcher.transformer;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import net.patchworkmc.patcher.ForgeModJar;
import net.patchworkmc.patcher.util.MinecraftVersion;

public abstract class VisitorTransformer extends ClassVisitor {
	protected final ClassPostTransformer postTransformer;
	protected final MinecraftVersion minecraftVersion;
	protected final ForgeModJar forgeModJar;

	protected VisitorTransformer(MinecraftVersion version, ForgeModJar jar, ClassVisitor parent, ClassPostTransformer postTransformer) {
		super(Opcodes.ASM9, parent);
		this.minecraftVersion = version;
		this.forgeModJar = jar;
		this.postTransformer = postTransformer;
	}
}
