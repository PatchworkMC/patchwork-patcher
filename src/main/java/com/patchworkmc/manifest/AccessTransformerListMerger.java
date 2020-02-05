package com.patchworkmc.manifest;

import java.util.ArrayList;
import java.util.HashSet;

import com.patchworkmc.manifest.accesstransformer.AccessTransformerEntry;
import com.patchworkmc.manifest.accesstransformer.AccessTransformerList;

// TODO: During the manifest AT rewrite, maybe make AccessTransformerList a Set to make this easier.
public class AccessTransformerListMerger {
	private AccessTransformerListMerger() {
		// NO-OP
	}

	/**
	 * Creates a new AccessTransformerList that contains both the target and dependency's access transformers.
	 * @param left The AccessTransformerList
	 * @param right
	 * @return the merged AccessTransformerList
	 */
	public static AccessTransformerList createMergedList(AccessTransformerList left, AccessTransformerList right) {
		HashSet<AccessTransformerEntry> entrySet = new HashSet<>();
		// We clone entries to avoid accidentally remapping/otherwise mutating an entry in another list
		left.getEntries().forEach(entry -> entrySet.add(cloneEntry(entry)));
		right.getEntries().forEach(entry -> entrySet.add(cloneEntry(entry)));

		return new AccessTransformerList(new ArrayList<>(entrySet));
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
