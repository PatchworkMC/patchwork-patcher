package net.patchworkmc.patcher.patch;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import net.patchworkmc.patcher.redirect.SuperclassRedirectTransformer;

public class ItemGroupTransformer extends SuperclassRedirectTransformer {
	// The intermediary name for ItemGroup
	private static final String ITEM_GROUP = "net/minecraft/class_1761";

	// Patchwork's replacement item group class
	private static final String PATCHWORK_ITEM_GROUP = "net/patchworkmc/api/redirects/itemgroup/PatchworkItemGroup";

	private static final String VANILLA_CREATE_ICON = "method_7750";
	private static final String VANILLA_CREATE_ICON_DESC = "()Lnet/minecraft/item/ItemStack;";

	private static final String PATCHWORK_CREATE_ICON = "patchwork$createIcon";

	public ItemGroupTransformer(ClassVisitor parent) {
		super(parent);

		redirectSuperclass(ITEM_GROUP, PATCHWORK_ITEM_GROUP);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (name.equals(VANILLA_CREATE_ICON) && descriptor.equals(VANILLA_CREATE_ICON_DESC)) {
			name = PATCHWORK_CREATE_ICON;
		}

		return super.visitMethod(access, name, descriptor, signature, exceptions);
	}
}
