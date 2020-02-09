package com.patchworkmc.mapping.remapper;

import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.TinyRemapper;

import com.patchworkmc.manifest.api.Remapper;

public class ManifestRemapper implements Remapper, AutoCloseable {
	private org.objectweb.asm.commons.Remapper asmRemapper;
	private TinyRemapper tiny;

	public ManifestRemapper(IMappingProvider mappings) {
		this.tiny = TinyRemapper.newRemapper()
						.ignoreFieldDesc(true)
			.withMappings(mappings)
			.build();
		this.asmRemapper = tiny.getRemapper();
	}

	@Override
	public String remapMemberDescription(String descriptor) {
		return asmRemapper.mapDesc(descriptor);
	}

	/**
	 * AccessTransformers lack descriptors for fields, so we have to use a NaiveRemapper instead.
	 */
	@Override
	public String remapFieldName(String owner, String name, String descriptor) {
		return asmRemapper.mapFieldName(owner.replace('.', '/'), name, descriptor);
	}

	@Override
	public String remapMethodName(String owner, String name, String descriptor) {
		return asmRemapper.mapMethodName(owner.replace('.', '/'), name, descriptor);
	}

	@Override
	public String remapClassName(String name) {
		return asmRemapper.map(name.replace('.', '/')).replace('/', '.');
	}

	@Override
	public void close() {
		tiny.finish();
	}
}
