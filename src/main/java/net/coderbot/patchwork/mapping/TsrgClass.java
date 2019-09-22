package net.coderbot.patchwork.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TsrgClass<F extends RawMapping> {
	private final String official;
	private final String mapped;

	private List<F> fields;
	private List<Mapping> methods;

	public TsrgClass(String official, String mapped) {
		this.official = official;
		this.mapped = mapped;

		this.fields = new ArrayList<>();
		this.methods = new ArrayList<>();
	}

	public void addField(F field) {
		this.fields.add(field);
	}

	public void addMethod(Mapping method) {
		this.methods.add(method);
	}

	public String getOfficial() {
		return official;
	}

	public String getMapped() {
		return mapped;
	}

	public List<F> getFields() {
		return Collections.unmodifiableList(fields);
	}

	public List<Mapping> getMethods() {
		return Collections.unmodifiableList(methods);
	}
}
