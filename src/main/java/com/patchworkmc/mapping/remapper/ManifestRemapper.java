package com.patchworkmc.mapping.remapper;

import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.TinyRemapper;

import com.patchworkmc.manifest.api.Remapper;

public class ManifestRemapper implements Remapper, AutoCloseable {
	private org.objectweb.asm.commons.Remapper asmRemapper;
	private NaiveRemapper naiveRemapper;
	private TinyRemapper tiny;

	public ManifestRemapper(IMappingProvider mappings, NaiveRemapper naiveRemapper) {
		this.tiny = TinyRemapper.newRemapper()
			.withMappings(mappings)
			.build();
		this.asmRemapper = tiny.getRemapper();
		this.naiveRemapper = naiveRemapper;
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
		return naiveRemapper.getField(name);
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
