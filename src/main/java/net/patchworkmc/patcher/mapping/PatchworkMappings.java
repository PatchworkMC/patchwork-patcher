package net.patchworkmc.patcher.mapping;

import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.model.ClassMapping;

import net.fabricmc.tinyremapper.IMappingProvider;

public class PatchworkMappings implements IMappingProvider {
	private final MappingSet lorenz;

	public PatchworkMappings(IMappingProvider provider) {
		MappingSet set = MappingSet.create();
		provider.load(new IMappingProvider.MappingAcceptor() {
			@Override
			public void acceptClass(String srcName, String dstName) {
				set.getOrCreateClassMapping(srcName).setDeobfuscatedName(dstName);
			}

			@Override
			public void acceptMethod(IMappingProvider.Member method, String dstName) {
				set.getOrCreateClassMapping(method.owner).getOrCreateMethodMapping(method.name, method.desc).setDeobfuscatedName(dstName);
			}

			@Override
			public void acceptMethodArg(IMappingProvider.Member method, int lvIndex, String dstName) {
				// not needed for Patcher
			}

			@Override
			public void acceptMethodVar(IMappingProvider.Member method, int lvIndex, int startOpIdx, int asmIndex, String dstName) {
				// not needed for Patcher
			}

			@Override
			public void acceptField(IMappingProvider.Member field, String dstName) {
				set.getOrCreateClassMapping(field.owner).getOrCreateFieldMapping(field.name, field.desc).setDeobfuscatedName(dstName);
			}
		});

		this.lorenz = set;
	}

	public PatchworkMappings(MappingSet lorenz) {
		this.lorenz = lorenz;
	}

	/**
	 * @return a copy of the internal {@link MappingSet}
	 */
	public MappingSet getLorenzMappings() {
		return lorenz.copy();
	}

	@Override
	public void load(MappingAcceptor out) {
		lorenz.getTopLevelClassMappings().forEach(clazz -> loadClass(clazz, out));
	}

	private void loadClass(ClassMapping<?, ?> clazz, MappingAcceptor out) {
		out.acceptClass(clazz.getFullObfuscatedName(), clazz.getFullDeobfuscatedName());
		clazz.getFieldMappings().forEach(f -> out.acceptField(new Member(clazz.getObfuscatedName(), f.getObfuscatedName(), f.getSignature().toJvmsIdentifier()), f.getDeobfuscatedName()));
		clazz.getMethodMappings().forEach(m -> out.acceptMethod(new Member(clazz.getDeobfuscatedName(), m.getObfuscatedName(), m.getSignature().toJvmsIdentifier()), m.getDeobfuscatedName()));
		clazz.getInnerClassMappings().forEach(innerclass -> loadClass(innerclass, out));
	}
}
