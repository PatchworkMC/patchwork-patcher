package net.coderbot.patchwork.voldemap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TsrgClass {
	private final String official;
	private final String mcp;

	private List<Entry> fields;
	private List<DescribedEntry> methods;

	public TsrgClass(String official, String mcp) {
		this.official = official;
		this.mcp = mcp;

		this.fields = new ArrayList<>();
		this.methods = new ArrayList<>();
	}

	void addField(Entry field) {
		this.fields.add(field);
	}

	void addMethod(DescribedEntry method) {
		this.methods.add(method);
	}

	public String getOfficial() {
		return official;
	}

	public String getMcp() {
		return mcp;
	}

	public List<Entry> getFields() {
		return Collections.unmodifiableList(fields);
	}

	public List<DescribedEntry> getMethods() {
		return Collections.unmodifiableList(methods);
	}

	public static class Entry {
		private String official;
		private String mcp;

		Entry(String official, String mcp) {
			this.official = official;
			this.mcp = mcp;
		}

		public String getOfficial() {
			return official;
		}

		public String getMcp() {
			return mcp;
		}
	}

	public static class DescribedEntry extends Entry {
		private String description;

		DescribedEntry(String official, String mcp, String description) {
			super(official, mcp);

			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}
}
