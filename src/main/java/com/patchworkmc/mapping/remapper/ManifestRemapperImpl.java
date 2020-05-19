package com.patchworkmc.mapping.remapper;

import net.fabricmc.tinyremapper.IMappingProvider;

import net.patchworkmc.manifest.accesstransformer.v2.exception.FatalRemappingException;
import net.patchworkmc.manifest.accesstransformer.v2.exception.MissingMappingException;
import net.patchworkmc.manifest.api.Remapper;

/**
 * Provides an {@link Remapper} for remapping access transformers. This class extends ASM's {@link org.objectweb.asm.commons.Remapper} in
 * order to provide method descriptor remapping functionality. Please do not reference this class directly,
 * but use the {@link Remapper} interface where one is needed.
 *
 * <p>Names are returned with slash seperators ({@code com/foo/example/BarClass}), but both dot and slash formatted names are accepted.</p>
 */
public class ManifestRemapperImpl extends org.objectweb.asm.commons.Remapper implements Remapper {
	private final PatchworkRemapper patchworkRemapper;
	public ManifestRemapperImpl(IMappingProvider mappings, PatchworkRemapper remapper) {
		this.patchworkRemapper = remapper;
	}

	// ASM overrides

	/**
	 * Surround with try catch on any ASM remapping method that will call this!
	 */
	@Override
	public String map(String name) {
		try {
			return patchworkRemapper.getClass(name.replace('.', '/'));
		} catch (MissingMappingException ex) {
			throw new FatalRemappingException(ex);
		}
	}

	// Patchwork Manifest's Remapper
	@Override
	public String remapMemberDescription(String descriptor) throws MissingMappingException {
		try {
			return mapDesc(descriptor);
		} catch (FatalRemappingException ex) {
			throw (MissingMappingException) ex.getCause();
		}
	}

	@Override
	public String remapFieldName(String owner, String name, String descriptor) throws MissingMappingException {
		owner = owner.replace('.', '/');

		if (!name.startsWith("field_")) {
			return name;
		}

		return patchworkRemapper.getField(owner, name);
	}

	@Override
	public String remapMethodName(String owner, String name, String descriptor) throws MissingMappingException {
		return patchworkRemapper.getMethod(owner, name, descriptor);
	}

	@Override
	public String remapClassName(String name) throws MissingMappingException {
		try {
			return map(name);
		} catch (FatalRemappingException ex) {
			throw (MissingMappingException) ex.getCause();
		}
	}
}
