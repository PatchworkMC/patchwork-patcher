package net.patchworkmc.patcher.transformer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.BiConsumer;

import net.fabricmc.tinyremapper.OutputConsumerPath;

import net.patchworkmc.patcher.ForgeModJar;
import net.patchworkmc.patcher.event.EventSubscriptionChecker;
import net.patchworkmc.patcher.util.MinecraftVersion;

public class PatchworkTransformer implements BiConsumer<String, byte[]> {
	private final MinecraftVersion minecraftVersion;
	private final OutputConsumerPath outputConsumer;
	private final ForgeModJar modJar;
	private final EventSubscriptionChecker checker;

	/**
	 * The main class transformer for Patchwork.
	**/
	public PatchworkTransformer(MinecraftVersion minecraftVersion, OutputConsumerPath outputConsumer, ForgeModJar modJar) {
		this.minecraftVersion = minecraftVersion;
		this.outputConsumer = outputConsumer;
		this.modJar = modJar;
		this.checker = new EventSubscriptionChecker();
	}

	@Override
	public void accept(String name, byte[] content) {
		// Names should match Java internal names, such as "java/lang/Object" or "com/example/Example$1"

		if (name.startsWith("/")) {
			throw new IllegalArgumentException("Name should not start with a /");
		}

		if (name.endsWith(".class")) {
			throw new IllegalArgumentException("Name should not end with .class");
		}

		if (name.startsWith("net/minecraft/")) {
			throw new IllegalArgumentException("Mod jars are not allowed to contain classes in Minecraft's package!");
		}

		if (name.startsWith("java/")) {
			throw new IllegalArgumentException("Mod jars are not allowed to contain classes in Java's package!");
		}

		outputConsumer.accept(name, Transformers.apply(minecraftVersion, modJar, content, checker));
	}

	public void finish() {
		this.checker.check();
	}

	public OutputConsumerPath getOutputConsumer() {
		return outputConsumer;
	}

	public void closeOutputConsumer() {
		try {
			this.outputConsumer.close();
		} catch (IOException ex) {
			throw new UncheckedIOException("Unable to close OutputConsumerPath", ex);
		}
	}
}
