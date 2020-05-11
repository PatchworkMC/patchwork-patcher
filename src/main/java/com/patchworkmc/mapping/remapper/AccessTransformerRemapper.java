package com.patchworkmc.mapping.remapper;

import org.objectweb.asm.Type;

import net.fabricmc.tinyremapper.IMappingProvider;

import net.patchworkmc.manifest.api.Remapper;
import com.patchworkmc.mapping.remapper.exception.FatalRemappingException;
import com.patchworkmc.mapping.remapper.exception.MissingMappingException;

/**
 * Provides an {@link Remapper} for remapping access transformers. This class extends ASM's {@link org.objectweb.asm.commons.Remapper} in
 * order to provide method descriptor remapping functionality.1
 *
 * <p>Names are returned with slash seperators ({@code com/foo/example/BarClass}), but both dot and slash formatted names are accepted.</p>
 */
public class AccessTransformerRemapper extends org.objectweb.asm.commons.Remapper implements Remapper {
	private final PatchworkRemapper patchworkRemapper;
	public AccessTransformerRemapper(IMappingProvider mappings, PatchworkRemapper remapper) {
		this.patchworkRemapper = remapper;
	}

	// ASM overrides
	@Override
	public String map(String name) {
		return patchworkRemapper.getClass(name.replace('.', '/'));
	}

	@Override
	public String mapFieldName(String owner, String name, String descriptor) {
		owner = owner.replace('.', '/');

		if (!name.startsWith("field_")) {
			return name;
		}
		// You would think ignoreFieldDesc would work instead, but it doesn't. No idea why.
		try {
			return patchworkRemapper.getField(owner, name);
		} catch (MissingMappingException ex) {
			throw new FatalRemappingException("Unable to remap field for access transformer!", ex);
		}
	}

	@Override
	public String mapMethodName(String owner, String name, String descriptor) {
		try {
			return patchworkRemapper.getMethod(owner, name, descriptor);
		} catch (MissingMappingException ex) {
			throw new FatalRemappingException("Unable to remap method for access transformer!", ex);
		}
	}

	// Patchwork Manifest's Remapper
	// acts as a wrapper around the asm one.
	@Override
	public String remapMemberDescription(String descriptor) {
		return mapDesc(descriptor);
	}

	@Override
	public String remapFieldName(String owner, String name, String descriptor) {
		return mapFieldName(owner, name, descriptor);
	}

	@Override
	public String remapMethodName(String owner, String name, String descriptor) {
		return mapMethodName(owner, name, descriptor);
	}

	@Override
	public String remapClassName(String name) {
		return map(name);
	}
}
