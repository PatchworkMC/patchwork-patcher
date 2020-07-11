package net.patchworkmc.mapping;

public class RawMapping {
	private String official;
	private String mapped;

	public RawMapping(String official, String mapped) {
		this.official = official;
		this.mapped = mapped;
	}

	public String getOfficial() {
		return official;
	}

	public String getMapped() {
		return mapped;
	}
}
