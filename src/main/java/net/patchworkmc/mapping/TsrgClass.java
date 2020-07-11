package net.patchworkmc.mapping;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class TsrgClass<F extends RawMapping> {
	private final String official;
	private final String mapped;

	private Map<String, F> fields;
	private Map<String, Mapping> methods;

	public TsrgClass(String official, String mapped) {
		this.official = official;
		this.mapped = mapped;

		// Maintain ordering
		this.fields = new LinkedHashMap<>();
		this.methods = new LinkedHashMap<>();
	}

	public void addField(F field) {
		this.fields.put(field.getOfficial(), field);
	}

	public void addMethod(Mapping method) {
		this.methods.put(method.getOfficial() + method.getDescription(), method);
	}

	public String getOfficial() {
		return official;
	}

	public String getMapped() {
		return mapped;
	}

	public F getField(String official) {
		return fields.get(official);
	}

	public Mapping getMethod(String official, String description) {
		return methods.get(official + description);
	}

	public Collection<F> getFields() {
		return Collections.unmodifiableCollection(fields.values());
	}

	public Collection<Mapping> getMethods() {
		return Collections.unmodifiableCollection(methods.values());
	}
}
