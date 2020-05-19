package com.patchworkmc.mapping.remapper;

import net.fabricmc.tinyremapper.IMappingProvider;

import net.patchworkmc.manifest.accesstransformer.v2.exception.FatalMissingMappingException;
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
			throw new FatalMissingMappingException(ex);
		}
	}

	// Patchwork Manifest's Remapper
	@Override
	public String remapMemberDescription(String descriptor) throws MissingMappingException {
		try {
			return mapDesc(descriptor);
		} catch (FatalMissingMappingException ex) {
			throw ex.getCause();
		}
	}

	@Override
	public String remapFieldName(String owner, String name, String descriptor) throws MissingMappingException {
		try {
			return patchworkRemapper.getField(owner.replace('.', '/'), name);
		} catch (FatalMissingMappingException ex) {
			throw ex.getCause();
		}
	}

	@Override
	public String remapMethodName(String owner, String name, String descriptor) throws MissingMappingException {
		try {
			return patchworkRemapper.getMethod(owner.replace('.', '/'), name, descriptor);
		} catch (FatalMissingMappingException ex) {
			throw ex.getCause();
		}
	}

	@Override
	public String remapClassName(String name) throws MissingMappingException {
		try {
			return map(name);
		} catch (FatalMissingMappingException ex) {
			throw ex.getCause();
		}
	}
}
