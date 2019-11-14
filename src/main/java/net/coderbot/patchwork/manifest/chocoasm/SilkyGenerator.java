package net.coderbot.patchwork.manifest.chocoasm;

import net.coderbot.patchwork.manifest.forge.AccessTransformerEntry;
import net.coderbot.patchwork.manifest.forge.AccessTransformerList;

public class SilkyGenerator {

	/**
	 * Parses an AccessTransformerList as a silky.at for ChocoASM
	 */
	public static String generate(AccessTransformerList accessTransformers) {
		StringBuilder sb = new StringBuilder();
		for(AccessTransformerEntry entry : accessTransformers.getEntries()) {
			if(!entry.isMemberIsField())
				sb.append(entry.getClazzName())
						.append(" ")
						.append(entry.getMemberName())
						.append(entry.getMemberDescription())
						.append("\n");
		}
		return sb.toString();
	}
}
