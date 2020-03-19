package com.patchworkmc.mapping.remapper;

import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.TinyRemapper;

import com.patchworkmc.manifest.api.Remapper;
import com.patchworkmc.mapping.MemberMap;

public class ManifestRemapper implements Remapper, AutoCloseable {
	private org.objectweb.asm.commons.Remapper asmRemapper;
	private TinyRemapper tiny;
	private MemberMap fields;

	public ManifestRemapper(IMappingProvider mappings) {
		this.tiny = TinyRemapper.newRemapper()
			.withMappings(mappings)
			.build();
		this.asmRemapper = tiny.getRemapper();
		this.fields = new MemberMap();

		mappings.load(new IMappingProvider.MappingAcceptor() {
			@Override
			public void acceptClass(String srcName, String dstName) {
				// NO-OP
			}

			@Override
			public void acceptMethod(IMappingProvider.Member method, String dstName) {
				// NO-OP
			}

			@Override
			public void acceptMethodArg(IMappingProvider.Member method, int lvIndex, String dstName) {
				// NO-OP
			}

			@Override
			public void acceptMethodVar(IMappingProvider.Member method, int lvIndex, int startOpIdx, int asmIndex, String dstName) {
				// NO-OP
			}

			@Override
			public void acceptField(IMappingProvider.Member field, String dstName) {
				fields.put(field.owner, field.name, dstName);
			}
		});
	}

	@Override
	public String remapMemberDescription(String descriptor) {
		return asmRemapper.mapDesc(descriptor);
	}

	@Override
	public String remapFieldName(String owner, String name, String descriptor) {
		owner = owner.replace('.', '/');

		if (descriptor.equals("")) {
			// You would think ignoreFieldDesc would work instead, but it doesn't. No idea why.
			String remapped = fields.get(owner, name);

			if (remapped == null) {
				throw new IllegalStateException("Missing field mapping for " + owner + "." + name);
			}

			return remapped;
		} else {
			return asmRemapper.mapFieldName(owner, name, descriptor);
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
}
