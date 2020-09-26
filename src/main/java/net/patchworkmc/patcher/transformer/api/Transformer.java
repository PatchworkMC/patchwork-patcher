package net.patchworkmc.patcher.transformer.api;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import net.patchworkmc.patcher.ForgeModJar;
import net.patchworkmc.patcher.util.MinecraftVersion;
import net.patchworkmc.patcher.util.VersionRange;

public abstract class Transformer extends ClassVisitor {
	protected final MinecraftVersion minecraftVersion;
	protected final ForgeModJar forgeModJar;
	public Transformer(MinecraftVersion version, ForgeModJar jar, ClassVisitor parent) {
		super(Opcodes.ASM7, parent);
		this.minecraftVersion = version;
		this.forgeModJar = jar;
	}
}
