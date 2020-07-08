package com.patchworkmc.patch;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.patchworkmc.redirect.SuperclassRedirectTransformer;

public class LevelGeneratorTypeTransformer extends SuperclassRedirectTransformer {
	private static final String LEVEL_GENERATOR_TYPE_NAME = "net/minecraft/class_1942";
	private static final String PATCHWORK_LEVEL_GENERATOR_TYPE_NAME = "net/patchworkmc/api/levelgenerators/PatchworkLevelGeneratorType";

	public LevelGeneratorTypeTransformer(ClassVisitor classVisitor) {
		super(classVisitor);

		redirectSuperclass(LEVEL_GENERATOR_TYPE_NAME, PATCHWORK_LEVEL_GENERATOR_TYPE_NAME);
	}
}
