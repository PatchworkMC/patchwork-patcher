package com.patchworkmc.manifest.converter;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.commons.Remapper;

import net.fabricmc.tinyremapper.IMappingProvider;

public class FieldDescriptorProvider {
	private DescriptorRemapper descriptorRemapper = new DescriptorRemapper();
	private Map<String, String> descriptors = new HashMap<>();

	public FieldDescriptorProvider(IMappingProvider mappings) {
		mappings.load(new IMappingProvider.MappingAcceptor() {
			@Override
			public void acceptClass(String srcName, String dstName) {
				descriptorRemapper.acceptClass(srcName, dstName);
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
				// NO-OP
			}
		});

		// TODO: Try to do this in a way that doesn't load mappings twice.
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
				String dstDesc = descriptorRemapper.mapDesc(field.desc);
				String dstOwner = descriptorRemapper.map(field.owner);

				String id = id(dstOwner, dstName);
				String presentDesc = descriptors.get(id);

				if (presentDesc != null) {
					if (!presentDesc.equals(dstDesc)) {
						String message = String.format("Duplicated field descriptor mapping for %s.%s (proposed %s, but key already mapped to %s!)", dstOwner, dstName, dstDesc, presentDesc);

						throw new IllegalArgumentException(message);
					} else {
						return;
					}
				}

				descriptors.put(id, dstDesc);
			}
		});
	}

	public String getDescriptor(String owner, String field) {
		return descriptors.get(id(owner, field));
	}

	private String id(String owner, String name) {
		return owner + ";;" + name;
	}

	private static class DescriptorRemapper extends Remapper {
		private Map<String, String> owners;

		private DescriptorRemapper() {
			owners = new HashMap<>();
		}

		private void acceptClass(String srcName, String dstName) {
			String existingName = owners.get(srcName);

			if (existingName != null && !existingName.equals(dstName)) {
				String message = String.format("Duplicated class mapping for %s (proposed %s, but key already mapped to %s!)", srcName, dstName, existingName);

				throw new IllegalArgumentException(message);
			}

			owners.put(srcName, dstName);
		}

		@Override
		public String map(String internalName) {
			String mapped = owners.get(internalName);

			if (mapped != null) {
				return mapped;
			} else {
				return internalName;
			}
		}
	}
}
