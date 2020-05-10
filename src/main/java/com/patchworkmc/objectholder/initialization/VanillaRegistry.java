package com.patchworkmc.objectholder.initialization;

import java.util.HashMap;

public final class VanillaRegistry {
	private static final HashMap<String, VanillaRegistry> REGISTRIES = new HashMap<>();

	/**
	 * Intermediary for {@code net.minecraft.util.Registry}.
	 */
	private static final String REGISTRY_DESCRIPTOR = "Lnet/minecraft/class_2378;";

	/**
	 * Intermediary for {@code net.minecraft.util.registry.DefaultedRegistry}.
	 */
	private static final String DEFAULTED_REGISTRY_DESCRIPTOR = "Lnet/minecraft/class_2348;";

	private String field;
	private boolean defaulted;

	private VanillaRegistry(String field, boolean defaulted) {
		this.field = field;
		this.defaulted = defaulted;
	}

	static {
		addMapping("class_2248", "field_11146", true); // Block -> BLOCK
		addMapping("class_1792", "field_11142", true); // Item -> ITEM
		addMapping("class_4168", "field_18796", false); // Activity -> ACTIVITY
		addMapping("class_1959", "field_11153", false); // Biome -> BIOME
		addMapping("class_1969", "field_11151", false); // BiomeSourceType -> BIOME_SOURCE_TYPE
		addMapping("class_2591", "field_11137", false); // BlockEntityType -> BLOCK_ENTITY
		addMapping("class_2939", "field_11157", false); // Carver -> CARVER
		addMapping("class_2798", "field_11149", false); // ChunkGeneratorType -> CHUNK_GENERATOR_TYPE
		addMapping("class_2806", "field_16643", true); // ChunkStatus -> CHUNK_STATUS
		addMapping("class_2960", "field_11158", false); // Identifier -> CUSTOM_STAT
		addMapping("class_3284", "field_11148", false); // Decorator -> DECORATOR
		addMapping("class_2874", "field_11155", false); // DimensionType -> DIMENSION
		addMapping("class_1887", "field_11160", false); // Enchantment -> ENCHANTMENT
		addMapping("class_1299", "field_11145", true); // EntityType -> ENTITY_TYPE
		addMapping("class_3031", "field_11138", false); // Feature -> FEATURE
		addMapping("class_3611", "field_11154", true); // Fluid -> FLUID
		addMapping("class_4140", "field_18793", true); // MemoryModuleType -> MEMORY_MODULE_TYPE
		addMapping("class_3917", "field_17429", false); // ContainerType -> CONTAINER
		addMapping("class_1291", "field_11159", false); // StatusEffect -> STATUS_EFFECT
		addMapping("class_1535", "field_11150", true); // PaintingMotive -> MOTIVE
		addMapping("class_2396", "field_11141", false); // ParticleType -> PARTICLE_TYPE
		addMapping("class_4158", "field_18792", true); // PointOfInterestType
		addMapping("class_1842", "field_11143", true); // Potion -> POTION
		addMapping("class_1865", "field_17598", false); // RecipeSerializer -> RECIPE_SERIALIZER
		addMapping("class_3956", "field_17597", false); // RecipeType -> RECIPE_TYPE
		addMapping("class_3827", "field_16792", false); // RuleTestType -> RULE_TEST
		addMapping("class_4170", "field_18795", false); // Schedule -> SCHEDULE
		addMapping("class_4149", "field_18794", true); // SensorType -> SENSOR_TYPE
		addMapping("class_3414", "field_11156", false); // SoundEvent -> SOUND_EVENT
		addMapping("class_3448", "field_11152", false); // StatType -> STAT_TYPE
		addMapping("class_3195", "field_16644", false); // StructureFeature -> STRUCTURE_FEATURE
		addMapping("class_3773", "field_16645", false); // StructurePieceType -> STRUCTURE_PIECE
		addMapping("class_3816", "field_16793", false); // StructurePoolElementType -> STRUCTURE_POOL_ELEMENT
		addMapping("class_3828", "field_16794", false); // StructureProcessorType -> STRUCTURE_PROCESSOR
		addMapping("class_3523", "field_11147", false); // SurfaceBuilder -> SURFACE_BUILDER
		addMapping("class_3852", "field_17167", true); // VillagerProfession
		addMapping("class_3854", "field_17166", true); // VillagerType
	}

	private static void addMapping(String clazz, String registry, boolean defaulted) {
		REGISTRIES.put("Lnet/minecraft/" + clazz + ";", new VanillaRegistry(registry, defaulted));
	}

	static VanillaRegistry get(String descriptor) {
		return REGISTRIES.get(descriptor);
	}

	String getField() {
		return field;
	}

	String getFieldDescriptor() {
		return defaulted ? DEFAULTED_REGISTRY_DESCRIPTOR : REGISTRY_DESCRIPTOR;
	}
}
