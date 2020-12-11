package net.patchworkmc.patcher.transformer;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import net.patchworkmc.patcher.ForgeModJar;
import net.patchworkmc.patcher.annotation.AnnotationProcessor;
import net.patchworkmc.patcher.capabilityinject.CapabilityInjectRewriter;
import net.patchworkmc.patcher.event.EventHandlerRewriter;
import net.patchworkmc.patcher.event.EventSubclassTransformer;
import net.patchworkmc.patcher.event.EventSubscriptionChecker;
import net.patchworkmc.patcher.objectholder.ObjectHolderRewriter;
import net.patchworkmc.patcher.patch.BiomeLayersTransformer;
import net.patchworkmc.patcher.patch.BlockSettingsTransformer;
import net.patchworkmc.patcher.patch.ExtensibleEnumTransformer;
import net.patchworkmc.patcher.patch.ItemGroupTransformer;
import net.patchworkmc.patcher.patch.KeyBindingsTransformer;
import net.patchworkmc.patcher.patch.LevelGeneratorTypeTransformer;
import net.patchworkmc.patcher.patch.SuperclassRedirectionTransformer;
import net.patchworkmc.patcher.util.MinecraftVersion;
import net.patchworkmc.patcher.util.VersionRange;

public final class Transformers {
	private static final LinkedHashMap<VisitorConstructor, VersionRange> visitorTransformers = new LinkedHashMap<>();
	private static final LinkedHashMap<NodeConstructor, VersionRange> nodeTransformers = new LinkedHashMap<>();

	public static byte[] apply(MinecraftVersion version, ForgeModJar jar, byte[] input, @Nullable EventSubscriptionChecker checker) {
		ClassReader reader = new ClassReader(input);
		ClassNode node = new ClassNode();
		ClassPostTransformer postTransformer = new ClassPostTransformer(checker);
		ClassVisitor parent = node;

		for (Map.Entry<VisitorConstructor, VersionRange> entry : visitorTransformers.entrySet()) {
			if (entry.getValue().isCompatible(version)) {
				parent = entry.getKey().apply(version, jar, parent, postTransformer);
			}
		}

		reader.accept(parent, ClassReader.EXPAND_FRAMES);

		nodeTransformers.forEach((constructor, range) -> {
			if (range.isCompatible(version)) {
				constructor.apply().transform(node);
			}
		});

		postTransformer.transform(node);
		ClassWriter writer = new ClassWriter(reader, 0);
		node.accept(writer);
		return writer.toByteArray();
	}

	static {
		// Magic Annotation rewriters
		addTransformer(AnnotationProcessor::new);
		addTransformer(EventHandlerRewriter::new);
		addTransformer(EventSubclassTransformer::new);
		addTransformer(ObjectHolderRewriter::new);
		addTransformer(CapabilityInjectRewriter::new);

		// Redirects

		addTransformer(ItemGroupTransformer::new);
		addTransformer(BlockSettingsTransformer::new);
		addTransformer(BiomeLayersTransformer::new);
		addTransformer(ExtensibleEnumTransformer::new);
		addTransformer(LevelGeneratorTypeTransformer::new);
		addTransformer(KeyBindingsTransformer::new);
		addTransformer(SuperclassRedirectionTransformer::new);
	}

	private static void addTransformer(MinecraftVersion start, MinecraftVersion end, VisitorConstructor constructor) {
		visitorTransformers.put(constructor, VersionRange.ofRange(start, end));
	}

	private static void addTransformer(MinecraftVersion version, VisitorConstructor constructor) {
		visitorTransformers.put(constructor, VersionRange.of(version));
	}

	private static void addTransformer(VisitorConstructor constructor) {
		visitorTransformers.put(constructor, VersionRange.ofAll());
	}

	private static void addTransformer(NodeConstructor nodeTransformer) {
		nodeTransformers.put(nodeTransformer, VersionRange.ofAll());
	}

	private interface VisitorConstructor {
		VisitorTransformer apply(MinecraftVersion version, ForgeModJar jar, ClassVisitor parent, ClassPostTransformer postTransformer);
	}

	private interface NodeConstructor {
		NodeTransformer apply();
	}
}
