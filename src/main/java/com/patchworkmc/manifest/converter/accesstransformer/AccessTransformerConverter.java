package com.patchworkmc.manifest.converter.accesstransformer;

import java.util.Map;

import net.patchworkmc.manifest.accesstransformer.v2.ForgeAccessTransformer;
import net.patchworkmc.manifest.accesstransformer.v2.Transformed;
import net.patchworkmc.manifest.accesstransformer.v2.TransformedClass;
import net.patchworkmc.manifest.accesstransformer.v2.TransformedField;
import net.patchworkmc.manifest.accesstransformer.v2.TransformedMethod;
import net.patchworkmc.manifest.accesstransformer.v2.TransformedWildcardMember;
import net.patchworkmc.manifest.accesstransformer.v2.flags.AccessLevel;
import net.patchworkmc.manifest.accesstransformer.v2.flags.Finalization;
import com.patchworkmc.mapping.IntermediaryHolder;

/**
 * Takes a {@link ForgeAccessTransformer} and spits out an AccessWidener/v1 file
 */
public class AccessTransformerConverter {
	private AccessTransformerConverter() {
		// NO-OP
	}

	public static byte[] convertToWidener(ForgeAccessTransformer accessTransformer, IntermediaryHolder holder, String namespace) {
		StringBuilder sb = new StringBuilder();
		sb.append("accessWidener\tv1\t").append(namespace).append('\n');
		sb.append("#\tAutomatically generated by Patchwork Patcher.\n");
		sb.append("#\tThe original (but remapped) AT entry is roughly preserved here for debugging.\n");
		sb.append("#\tThey may be stripped in future releases.\n\n");

		for (TransformedClass targetClass : accessTransformer.getClasses()) {
			if (targetClass.getAccessLevel() != AccessLevel.KEEP || targetClass.getFinalization() != Finalization.KEEP) {
				writeClass(sb, targetClass);
			}
			writeWildcards(sb, targetClass.getName(), (TransformedWildcardMember) targetClass.getDefaultForFields(),
				(TransformedWildcardMember) targetClass.getDefaultForMethods(), holder);

			targetClass.getFields().forEach(field -> writeField(sb, field, holder));
			targetClass.getMethods().forEach(method -> writeMethod(sb, method));
		}

		return sb.toString().getBytes();
	}


	private static void writeClass(StringBuilder sb, TransformedClass targetClass) {
		//// Debug
		// Modifer word
		writeDebugModifier(sb, targetClass);
		// Target
		sb.append('\t').append(targetClass.getName()).append('\n');

		//// Widener
		// Modifier word
		sb.append(targetClass.getFinalization() == Finalization.REMOVE ? "extendable" : "accessible");
		// Target
		sb.append("\tclass\t").append(targetClass.getName()).append('\n');
	}

	private static void writeWildcards(StringBuilder sb, String owner, TransformedWildcardMember fields, TransformedWildcardMember methods, IntermediaryHolder holder) {
		//// Debug
		if (fields != null) {
			writeDebugModifier(sb, fields);
			sb.append('\t').append(owner).append('\t').append("*\n");
		}
		if (methods != null) {
			writeDebugModifier(sb, methods);
			sb.append('\t').append(owner).append('\t').append("*\n");
		}


		//// Widener
		for (Map.Entry<String, IntermediaryHolder.Member> entry : holder.getMappings().get(owner).entrySet()) {
			IntermediaryHolder.Member member = entry.getValue();

			if (member.isField && fields != null) {
				// We don't write the debug because it's already been done
				writeField(sb, owner, member.name, member.descriptor, fields, false);
			} else if (methods != null) {
				writeMethod(sb, owner, member.name, member.descriptor, methods, false);
			}
		}
	}

	private static void writeField(StringBuilder sb, TransformedField transformed, IntermediaryHolder holder) {
		String owner = transformed.getOwner();
		String name = transformed.getName();
		String descriptor = holder.getMappings().get(owner).get(name).descriptor;
		AccessTransformerConverter.writeField(sb, owner, name, descriptor, transformed, true);
	}

	private static void writeField(StringBuilder sb, String owner, String name, String descriptor, Transformed transformed, boolean writeDebug) {
		if (writeDebug) {
			writeDebugModifier(sb, transformed);
			sb.append('\t').append(owner).append('\t').append(name).append('\n');
		}

		sb.append(transformed.getFinalization() == Finalization.REMOVE ? "mutable\t" : "accessible\t");

		sb.append("field\t").append(owner).append('\t');
		sb.append(name).append('\t').append(descriptor).append('\n');

	}

	private static void writeMethod(StringBuilder sb, TransformedMethod method) {
		writeMethod(sb, method.getOwner(), method.getName(), method.getDescriptor(), method, true);
	}

	// TODO support final(?)
	private static void writeMethod(StringBuilder sb, String owner, String name, String descriptor, Transformed transformed, boolean writeDebug) {
		if (writeDebug) {
			writeDebugModifier(sb, transformed);
			sb.append('\t').append(owner).append('\t').append(name).append('\n');
		}

		// We go ahead and remove final unless it's explictly added because accessible on a method adds it.
		// This might cause a "desync" between the two in finalization status,
		// but it shouldn't cause any issues.
		//// Widener
		sb.append("extendable\t");
		sb.append("method\t").append(owner).append('\t');
		sb.append(name).append('\t').append(descriptor).append('\n');
		if (transformed.getAccessLevel().equals(AccessLevel.PUBLIC)) {
			sb.append("accessible\t");
			sb.append("method\t").append(owner).append('\t');
			sb.append(name).append('\t').append(descriptor).append('\n');
		}
	}

	private static void writeDebugModifier(StringBuilder sb, Transformed transformed) {
		sb.append("#").append(transformed.getAccessLevel()).append('/').append(transformed.getFinalization());
	}
}
