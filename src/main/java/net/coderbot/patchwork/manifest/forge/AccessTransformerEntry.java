package net.coderbot.patchwork.manifest.forge;

import net.coderbot.patchwork.mapping.TsrgMappings;
import net.fabricmc.mappings.*;

public class AccessTransformerEntry {
    private String clazzName;
    private String memberName;
    private boolean memberIsField = false;

    public AccessTransformerEntry(String clazzName, String memberName, Mappings voldeToOfficial,
                                  Mappings officialToIntermediary) {
        this.clazzName = clazzName;
        this.memberName = memberName;
        devoldify(voldeToOfficial);
        fabrify(officialToIntermediary);
    }

    private void devoldify(Mappings voldeToOfficial) {
        System.out.println(clazzName + " " + memberName);
        //boolean memberIsField = false; // for when we go from official -> intermediary

        // loop through every single class mapping until we find the one we want
        for(ClassEntry classEntry : voldeToOfficial.getClassEntries()) {
            if(classEntry.get("srg").equals(clazzName)) {
                // Found it! Set the class name to the official
                clazzName = classEntry.get("official");
                break;
            }
        }

        if(memberName.startsWith("field_")) {
            // it's a field
            this.memberIsField = true;

            for(FieldEntry fieldEntry : voldeToOfficial.getFieldEntries()) {
                if(fieldEntry.get("srg").getName().equals(memberName)) {
                    // Found it! Set the member name to the official
                    memberName = fieldEntry.get("official").getName();
                }
            }

        } else {
            // It's a method

            for(MethodEntry methodEntry : voldeToOfficial.getMethodEntries()) {
                if(methodEntry.get("srg").getName().equals(memberName)) {
                    // Found it! Set the member name to the official
                    memberName = methodEntry.get("official").getName();
                }
            }
        }
        //System.out.println(clazzName + " " + memberName);
    }

    private void fabrify(Mappings officialToIntermediary) {
        String clazzName = this.clazzName;
        String memberName = this.memberName;
        System.out.println(clazzName + " " + memberName);
        //boolean memberIsField = false; // for when we go from official -> intermediary

        // loop through every single class mapping until we find the one we want
        for(ClassEntry classEntry : officialToIntermediary.getClassEntries()) {
            if(classEntry.get("official").equals(clazzName)) {
                // Found it! Set the class name to the intermediary
                clazzName = classEntry.get("intermediary");
               break;
            }
        }

        if(memberIsField) {
            for(FieldEntry fieldEntry : officialToIntermediary.getFieldEntries()) {
                EntryTriple official = fieldEntry.get("official");
                EntryTriple intermediary = fieldEntry.get("intermediary");
                if(official.getOwner().equals(this.clazzName)/*the official one from before*/ && official.getName().equals(memberName)) {
                    memberName = intermediary.getName();
                }
            }

        } else {
            // It's a method
            for(MethodEntry methodEntry : officialToIntermediary.getMethodEntries()) {
                if(methodEntry.get("official").getName().equals(memberName)) {
                    // Found it! Set the member name to the official
                    memberName = methodEntry.get("intermediary").getName();
                    break;
                }
            }
        }
        this.clazzName = clazzName;
        this.memberName = memberName;
        System.out.println(this.clazzName + " " + this.memberName);
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
