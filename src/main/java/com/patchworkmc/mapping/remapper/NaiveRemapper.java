package com.patchworkmc.mapping.remapper;

import java.util.HashMap;

import net.fabricmc.tinyremapper.IMappingProvider;

import com.patchworkmc.Patchwork;

/**
 * Remaps classes, methods, and fields based on the assumption that names are never repeated in the source mappings,
 * or that they duplicate directly in the target mappings. (i.e. 'valueOf' -> 'valueOf')
 *
 * <p>Mappings that are duplicated will be ignored after their first entry.</p>
 */
public class NaiveRemapper {
	private final HashMap<String, String> classes = new HashMap<>();
	private final HashMap<String, String> methods = new HashMap<>();
	private final HashMap<String, String> fields = new HashMap<>();
	private final HashMap<String, String> fieldDesc = new HashMap<>();

	public NaiveRemapper(IMappingProvider mappings) {
		mappings.load(new IMappingProvider.MappingAcceptor() {
			@Override
			public void acceptClass(String srcName, String dstName) {
				if (classes.get(srcName) == null) {
					classes.put(srcName, dstName);
				} else {
					throw new IllegalArgumentException("Duplicated class name " + srcName);
				}
			}

			@Override
			public void acceptMethod(IMappingProvider.Member method, String dstName) {
				// Have to include some exceptions
				String presentName = methods.get(method.name);

				if (presentName == null || presentName.equals(dstName)) {
					methods.put(method.name, dstName);
				} else {
					Patchwork.LOGGER.debug("Duplicated method name " + method.name);
				}
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
				String presentName = fields.get(field.name);

				if (presentName == null || presentName.equals(dstName)) {
					fields.put(field.name, dstName);
					fieldDesc.put(dstName, field.desc);
				} else {
					Patchwork.LOGGER.debug("Duplicated method name " + field.name);
				}
			}
		});
	}

	public String getClass(String volde) {
		return classes.getOrDefault(volde, volde);
	}

	public String getMethod(String volde) {
		return methods.getOrDefault(volde, volde);
	}

	public String getField(String volde) {
		return fields.getOrDefault(volde, volde);
	}

	/**
	 * Suffixed with 'special' to try and get people to read the doc before calling.
	 * @return the VOLDE descriptor for the given INTERMEDIARY field, or an empty string.
	 */
	public String getFieldDescSpecial(String intermediary) {
		return fieldDesc.getOrDefault(intermediary, "");
	}
}
