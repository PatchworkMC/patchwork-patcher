package com.patchworkmc.manifest.converter.com.patchworkmc.manifest;

import java.util.ArrayList;

import com.patchworkmc.manifest.accesstransformer.AccessTransformerEntry;
import com.patchworkmc.manifest.accesstransformer.AccessTransformerList;

public class AccessTransformerListMerger {
	private AccessTransformerListMerger() {
		// NO-OP
	}

	/**
	 * Creates a new AccessTransformerList that contains both the target and dependency's access transformers.
	 * @param target
	 * @param dependency
	 * @return
	 */
	public static AccessTransformerList createMergedList(AccessTransformerList target, AccessTransformerList dependency) {
		// We're making a new list, independent of the old one.
		AccessTransformerList result = new AccessTransformerList(new ArrayList<>(target.getEntries()));

		for (AccessTransformerEntry entry : dependency.getEntries()) {
			if (!contains(entry, result)) {
				result.getEntries().add(cloneEntry(entry));
			}
		}

		return result;
	}

	private static boolean contains(AccessTransformerEntry entry, AccessTransformerList list) {
		for (AccessTransformerEntry targetEntry : list.getEntries()) {
			if (targetEntry.getClassName().equals(entry.getClassName()) && targetEntry.getDescriptor().equals(entry.getMemberName())
							&& targetEntry.getMemberName().equals(entry.getMemberName())) {
				return true;
			}
		}

		return false;
	}

	// TODO this should probably be in parser instead
	private static AccessTransformerEntry cloneEntry(AccessTransformerEntry entry) {
		if (entry.isField()) {
			return new AccessTransformerEntry(entry.getClassName(), entry.getMemberName());
		} else {
			// TODO make sure this works
			return new AccessTransformerEntry(entry.getClassName(), entry.getMemberName() + entry.getDescriptor());
		}
	}
}
