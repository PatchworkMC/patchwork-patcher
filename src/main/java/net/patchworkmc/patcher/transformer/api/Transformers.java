package net.patchworkmc.patcher.transformer.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;

import net.patchworkmc.patcher.ForgeModJar;
import net.patchworkmc.patcher.event.EventSubclassTransformer;
import net.patchworkmc.patcher.patch.BiomeLayersTransformer;
import net.patchworkmc.patcher.patch.BlockSettingsTransformer;
import net.patchworkmc.patcher.patch.ExtensibleEnumTransformer;
import net.patchworkmc.patcher.patch.ItemGroupTransformer;
import net.patchworkmc.patcher.patch.KeyBindingsTransformer;
import net.patchworkmc.patcher.patch.LevelGeneratorTypeTransformer;
import net.patchworkmc.patcher.util.MinecraftVersion;
import net.patchworkmc.patcher.util.VersionRange;

public final class Transformers {
	private static final LinkedHashMap<TransformerConstructor, VersionRange> allTransformers = new LinkedHashMap<>();

	public static ClassVisitor makeVisitor(MinecraftVersion version, ForgeModJar jar, ClassVisitor parent) {
		for (Map.Entry<TransformerConstructor, VersionRange> entry : allTransformers.entrySet()) {
			if (entry.getValue().isCompatible(version)) {
				parent = entry.getKey().apply(version, jar, parent);
			}
		}

		return parent;
	}

	static {
		addTransformer(ItemGroupTransformer::new);
		addTransformer(BlockSettingsTransformer::new);
		addTransformer(BiomeLayersTransformer::new);
		addTransformer(ExtensibleEnumTransformer::new);
		addTransformer(EventSubclassTransformer::new);
		addTransformer(LevelGeneratorTypeTransformer::new);
		addTransformer(KeyBindingsTransformer::new);
	}

	private static void addTransformer(MinecraftVersion start, MinecraftVersion end, TransformerConstructor constructor) {
		allTransformers.put(constructor, VersionRange.ofRange(start, end));
	}

	private static void addTransformer(MinecraftVersion version, TransformerConstructor constructor) {
		allTransformers.put(constructor, VersionRange.of(version));
	}

	private static void addTransformer(TransformerConstructor constructor) {
		allTransformers.put(constructor, VersionRange.ofAll());
	}

	private interface TransformerConstructor {
		Transformer apply(MinecraftVersion version, ForgeModJar jar, ClassVisitor parent);
	}
}
