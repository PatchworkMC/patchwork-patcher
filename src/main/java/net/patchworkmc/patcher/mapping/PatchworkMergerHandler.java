package net.patchworkmc.patcher.mapping;

import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.merge.MappingSetMergerHandler;
import org.cadixdev.lorenz.merge.MergeContext;
import org.cadixdev.lorenz.merge.MergeResult;
import org.cadixdev.lorenz.model.ClassMapping;
import org.cadixdev.lorenz.model.FieldMapping;
import org.cadixdev.lorenz.model.InnerClassMapping;
import org.cadixdev.lorenz.model.MethodMapping;
import org.cadixdev.lorenz.model.TopLevelClassMapping;

public class PatchworkMergerHandler implements MappingSetMergerHandler {
	// This is a hack solution for https://github.com/CadixDev/Lorenz/issues/41
	@Override
	public FieldMapping mergeDuplicateFieldMappings(FieldMapping left, FieldMapping strictRightDuplicate, FieldMapping looseRightDuplicate, FieldMapping strictRightContinuation, FieldMapping looseRightContinuation, ClassMapping<?, ?> target, MergeContext context) {
		if (strictRightDuplicate != null) {
			return MappingSetMergerHandler.super.mergeDuplicateFieldMappings(left, strictRightDuplicate, looseRightDuplicate, strictRightContinuation, looseRightContinuation, target, context);
		} else {
			return MappingSetMergerHandler.super.mergeFieldMappings(left, null, looseRightDuplicate, target, context);
		}
	}

	// MCP 1.16.3 has a mapping that intermediary doesn't. This nukes all common and missing mappings to prevent that
	@Override
	public MergeResult<TopLevelClassMapping> addRightTopLevelClassMapping(TopLevelClassMapping left, MappingSet target, MergeContext context) {
		return new MergeResult<>(null);
	}

	@Override
	public MergeResult<InnerClassMapping> addRightInnerClassMapping(InnerClassMapping left, ClassMapping<?, ?> target, MergeContext context) {
		return new MergeResult<>(null);
	}

	@Override
	public FieldMapping addRightFieldMapping(FieldMapping left, ClassMapping<?, ?> target, MergeContext context) {
		return null;
	}

	@Override
	public MergeResult<MethodMapping> addRightMethodMapping(MethodMapping right, ClassMapping<?, ?> target, MergeContext context) {
		return new MergeResult<>(null);
	}


}
