package net.patchworkmc.patcher.patch;

import org.objectweb.asm.ClassVisitor;

import com.patchworkmc.redirect.MethodRedirectTransformer;
import com.patchworkmc.redirect.Target;

public class BlockSettingsTransformer extends MethodRedirectTransformer {
	// The intermediary name for Block$Settings
	private static final String BLOCK_SETTINGS = "net/minecraft/class_2248$class_2251";

	// Patchwork's shim to call the protected methods using FabricBlockSettings
	private static final String PATCHWORK_BLOCK_SETTINGS = "net/patchworkmc/api/redirects/block/PatchworkBlockSettings";

	public BlockSettingsTransformer(ClassVisitor parent) {
		super(parent);

		addRedirect("method_16229", "dropsNothing");
		addRedirect("method_9618", "breakInstantly");
		addRedirect("method_9624", "hasDynamicBounds");
		addRedirect("method_9626", "sounds");
		addRedirect("method_9631", "lightLevel");
		addRedirect("method_9632", "strength"); // with one argument
		addRedirect("method_9640", "ticksRandomly");
	}

	private void addRedirect(String fromMethod, String toMethod) {
		Target from = new Target(BLOCK_SETTINGS, fromMethod);
		Target to = new Target(PATCHWORK_BLOCK_SETTINGS, toMethod);

		redirectInstanceMethodToStatic(from, to);
	}
}
