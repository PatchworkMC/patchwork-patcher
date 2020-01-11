package com.patchworkmc.mapping.remapper;

import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.TinyRemapper;

import com.patchworkmc.manifest.api.Remapper;

public class ManifestRemapper implements Remapper, AutoCloseable {
	private org.objectweb.asm.commons.Remapper asmRemapper;
	private TinyRemapper tiny;

	public ManifestRemapper(IMappingProvider mappings) {
		// TODO: this is a copy-paste of the old tinyremapper that was used in my original AT fork.
		// Some of these flags might not be needed.
		this.tiny = TinyRemapper.newRemapper()
			.withMappings(mappings)
			.rebuildSourceFilenames(true)
			.ignoreFieldDesc(true)
			.build();
		this.asmRemapper = tiny.getRemapper();
	}

	@Override
	public String remapMemberDescription(String descriptor) {
		return asmRemapper.mapDesc(descriptor);
	}

	@Override
	public String remapFieldName(String owner, String name, String descriptor) {
		return asmRemapper.mapFieldName(owner, name, descriptor);
	}

	@Override
	public String remapMethodName(String owner, String name, String descriptor) {
		return asmRemapper.mapMethodName(owner, name, descriptor);
	}

	@Override
	public String remapClassName(String name) {
		return asmRemapper.map(name);
	}

	@Override
	public void close() {
		tiny.finish();
	}
}
