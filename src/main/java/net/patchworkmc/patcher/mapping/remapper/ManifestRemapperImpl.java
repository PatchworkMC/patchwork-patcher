package net.patchworkmc.patcher.mapping.remapper;

import org.cadixdev.lorenz.MappingSet;

import net.fabricmc.tinyremapper.IMappingProvider;

import net.patchworkmc.manifest.accesstransformer.v2.exception.FatalMissingMappingException;
import net.patchworkmc.manifest.accesstransformer.v2.exception.MissingMappingException;
import net.patchworkmc.manifest.api.Remapper;
import net.patchworkmc.patcher.mapping.PatchworkMappings;

/**
 * Provides an {@link Remapper} for remapping access transformers. This class extends ASM's {@link org.objectweb.asm.commons.Remapper} in
 * order to provide method descriptor remapping functionality. Please do not reference this class directly,
 * but use the {@link Remapper} interface where one is needed.
 *
 * <p>Names are returned with slash seperators ({@code com/foo/example/BarClass}), but both dot and slash formatted names are accepted.</p>
 */
public class ManifestRemapperImpl extends org.objectweb.asm.commons.Remapper implements Remapper {
	private final MappingSet mappings;

	public ManifestRemapperImpl(MappingSet mappings) {
		this.mappings = mappings;
	}

	// ASM overrides

	/**
	 * Surround with try catch on any ASM remapping method that will call this!
	 */
	@Override
	public String map(String name) {
		return mappings.getClassMapping(name).orElseThrow(() -> new FatalMissingMappingException(new MissingMappingException("cannot find mapping for " + name)))
			.getDeobfuscatedName().replace('.', '/');
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
		return mappings.getClassMapping(owner).orElseThrow(() -> new MissingMappingException("cannot find class " + owner))
			.getFieldMapping(name).orElseThrow(() -> new MissingMappingException("cannot find field " + name))
			.getDeobfuscatedName().replace('.', '/');
	}

	@Override
	public String remapMethodName(String owner, String name, String descriptor) throws MissingMappingException {
		return mappings.getClassMapping(owner).orElseThrow(() -> new MissingMappingException("cannot find class " + owner))
			.getMethodMapping(name, descriptor).orElseThrow(() -> new MissingMappingException("cannot find method" + name + descriptor))
			.getDeobfuscatedName().replace('.', '/');
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
