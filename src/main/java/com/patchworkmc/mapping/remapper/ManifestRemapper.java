package com.patchworkmc.mapping.remapper;

import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.TinyRemapper;

import com.patchworkmc.manifest.api.Remapper;

public class ManifestRemapper implements Remapper, AutoCloseable {
	private org.objectweb.asm.commons.Remapper asmRemapper;
	private TinyRemapper tiny;
	private NaiveRemapper naiveRemapper;
	
	public ManifestRemapper(IMappingProvider mappings, NaiveRemapper remapper) {
		this.tiny = TinyRemapper.newRemapper()
			.withMappings(mappings)
			.build();
		this.asmRemapper = tiny.getRemapper();
		this.naiveRemapper = remapper;
	}

	@Override
	public String remapMemberDescription(String descriptor) {
		return asmRemapper.mapDesc(descriptor);
	}

	@Override
	public String remapFieldName(String owner, String name, String descriptor) {
		if (descriptor.equals("")) {
			// You would think ignoreFieldDesc would work instead, but it doesn't. No idea why.
			return naiveRemapper.getField(name);
		} else {
			return asmRemapper.mapFieldName(owner.replace('.', '/'), name, descriptor);
		}
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

	public NaiveRemapper getNaiveRemapper() {
		return naiveRemapper;
	}
}
