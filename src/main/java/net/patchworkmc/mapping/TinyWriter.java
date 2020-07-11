package net.patchworkmc.mapping;

import net.fabricmc.tinyremapper.IMappingProvider;

public class TinyWriter implements IMappingProvider.MappingAcceptor {
	private StringBuilder tiny;

	public TinyWriter(String srcNamespace, String dstNamespace) {
		tiny = new StringBuilder();

		tiny.append("v1\t");
		tiny.append(srcNamespace);
		tiny.append('\t');
		tiny.append(dstNamespace);
		tiny.append('\n');
	}

	public void acceptClass(String srcName, String dstName) {
		tiny.append("CLASS\t");
		tiny.append(srcName);
		tiny.append('\t');
		tiny.append(dstName);
		tiny.append('\n');
	}

	public void acceptMethod(IMappingProvider.Member method, String dstName) {
		tiny.append("METHOD\t");
		tiny.append(method.owner);
		tiny.append('\t');
		tiny.append(method.desc);
		tiny.append('\t');
		tiny.append(method.name);
		tiny.append('\t');
		tiny.append(dstName);
		tiny.append('\n');
	}

	public void acceptMethodArg(IMappingProvider.Member method, int lvIndex, String dstName) {
		// tiny v1 does not support method args
	}

	public void acceptMethodVar(IMappingProvider.Member method, int lvIndex, int startOpIdx, int asmIndex, String dstName) {
		// tiny v1 does not support method vars
	}

	public void acceptField(IMappingProvider.Member field, String dstName) {
		tiny.append("FIELD\t");
		tiny.append(field.owner);
		tiny.append('\t');
		tiny.append(field.desc);
		tiny.append('\t');
		tiny.append(field.name);
		tiny.append('\t');
		tiny.append(dstName);
		tiny.append('\n');
	}

	public String toString() {
		return tiny.toString();
	}
}
