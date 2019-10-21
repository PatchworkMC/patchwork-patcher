package net.coderbot.patchwork.manifest.forge;

import net.fabricmc.tinyremapper.MemberInstance;
import org.objectweb.asm.commons.Remapper;

public class AccessTransformerEntry {
	private String clazzName;
	private String memberName;
	private boolean memberIsField = false;
	// todo inner class support
	public AccessTransformerEntry(String clazzName,
			String memberName,
			Remapper voldeToOfficial,
			Remapper officialToIntermediary) {
		this.clazzName = clazzName;
		this.memberName = memberName;
		remap(voldeToOfficial);
		remap(officialToIntermediary);
		System.out.println(this.memberName);
	}

	private void remap(Remapper remapper /*remapper is easier to type*/) {
		String mappedClazzName = remapper.map(clazzName);
		String mappedMemberName;
		memberIsField = !memberName.contains("(");
		if(memberIsField) {
			mappedMemberName = remapper.mapFieldName(clazzName, memberName, "");
		} else {
			int split = memberName.indexOf("(");
			String methodName = memberName.substring(0, split);
			String methodDesc = memberName.substring(split);
			mappedMemberName = remapper.mapMethodName(clazzName, methodName, methodDesc);
		}
		this.clazzName = mappedClazzName;
		this.memberName = mappedMemberName;
	}

	public String getClazzName() {
		return clazzName;
	}

	public String getMemberName() {
		return memberName;
	}

	public boolean isMemberIsField() {
		return memberIsField;
	}
}
