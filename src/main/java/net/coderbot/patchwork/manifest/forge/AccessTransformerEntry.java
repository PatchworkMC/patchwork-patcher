package net.coderbot.patchwork.manifest.forge;

import net.coderbot.patchwork.mapping.TsrgMappings;

import net.fabricmc.mappings.*;
import net.fabricmc.mappings.helpers.asm.AsmRemapperCache;
import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;
import org.objectweb.asm.commons.Remapper;

import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.Map;

public class AccessTransformerEntry {
	private String clazzName;
	private String memberName;
	private boolean memberIsField = false;
	//todo inner class support
	public AccessTransformerEntry(String clazzName,
								  String memberName,
								  Remapper voldeToOfficial, Remapper officialToIntermediary) {
		this.clazzName = clazzName;
		this.memberName = memberName;
		devoldify(voldeToOfficial);

		System.out.println(this.memberName);
	}

	private void devoldify(Remapper remapper /*remapper is easier to type*/) {
		String officialClazzName = remapper.map(clazzName);
		String officialMemberName = "";
		memberIsField = !memberName.contains("(");
		if(memberIsField) {

			//officialMemberName = remapper.mapFieldName(officialClazzName, memberName, "");
		} else {
			int split = memberName.indexOf("(");
			String methodName = memberName.substring(0, split);
			String methodDesc = memberName.substring(split);
			officialMemberName = remapper.mapMethodName(clazzName, methodName, methodDesc);
		}
		this.clazzName = officialClazzName;
		this.memberName = officialMemberName;
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
