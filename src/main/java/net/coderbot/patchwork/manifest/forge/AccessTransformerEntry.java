package net.coderbot.patchwork.manifest.forge;

import net.fabricmc.tinyremapper.MemberInstance;
import org.objectweb.asm.commons.Remapper;

public class AccessTransformerEntry {
	private String clazzName;
	private String memberName;
	private String memberDescription = "";
	private boolean memberIsField;
	// todo inner class support
	public AccessTransformerEntry(String clazzName, String memberName) {
		this.clazzName = clazzName;
		this.memberName = memberName;
		memberIsField = !memberName.contains("(");
		if(!memberIsField) {
			int split = memberName.indexOf("(");
			memberName = memberName.substring(0, split);
			this.memberDescription = this.memberName.substring(split);
		}

		this.memberName = memberName;
		System.out.println(this.memberName);
	}

	public AccessTransformerEntry remap(Remapper remapper) {
		String mappedMemberName;

		if(memberIsField) {
			mappedMemberName = remapper.mapFieldName(clazzName, memberName, "");
		} else {
			mappedMemberName = remapper.mapMethodName(clazzName, memberName, memberDescription);
			this.memberDescription = remapper.mapDesc(memberDescription);
		}
		this.clazzName = remapper.map(clazzName);
		this.memberName = mappedMemberName;
		return this;
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

	public String getMemberDescription() {
		return memberDescription;
	}
}
