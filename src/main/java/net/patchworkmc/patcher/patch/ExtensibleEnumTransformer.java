package net.patchworkmc.patcher.patch;

import org.objectweb.asm.ClassVisitor;

import com.patchworkmc.redirect.MethodRedirectTransformer;
import com.patchworkmc.redirect.Target;

public class ExtensibleEnumTransformer extends MethodRedirectTransformer {
	private static final String PATCHWORK_ENUM_HACKS = "net/patchworkmc/api/enumhacks/EnumHacks";

	public ExtensibleEnumTransformer(ClassVisitor classVisitor) {
		super(classVisitor);

		addRedirect("net/minecraft/class_1814", "createRarity"); // Rarity
		addRedirect("net/minecraft/class_1311", "createEntityCategory"); // EntityCategory
		addRedirect("net/minecraft/class_3785$class_3786", "createStructurePoolProjection"); // StructurePool.Projection
		addRedirect("net/minecraft/class_3124$class_3125", "createOreFeatureConfigTarget"); // OreFeatureConfig.Target
		addRedirect("net/minecraft/class_2582", "createBannerPattern"); // BannerPattern
		addRedirect("net/minecraft/class_1317$class_1319", "createSpawnRestrictionLocation"); // SpawnRestriction.Location
		addRedirect("net/minecraft/class_1886", "createEnchantmentTarget"); // EnchantmentTarget
	}

	private void addRedirect(String enumClazz, String methodName) {
		Target from = new Target(enumClazz, "create");
		Target to = new Target(PATCHWORK_ENUM_HACKS, methodName);

		redirectStaticMethod(from, to);
	}
}
