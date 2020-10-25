package net.patchworkmc.patcher.manifest.converter.mod;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.patchworkmc.manifest.mod.ModManifest;
import net.patchworkmc.manifest.mod.ModManifestDependency;
import net.patchworkmc.manifest.mod.ModManifestEntry;

public class ModManifestConverter {
	private static final DateFormat ISO_UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");

	static {
		ISO_UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private ModManifestConverter() {
		// NO-OP
	}

	/**
	 * Generates a list of JsonObjects based on all of the mods in the manifest.
	 * Index 0 will contain all dependencies, etc. The rest should be saved into empty jars to show
	 * all the mods in things like ModMenu.
	 *
	 * @param manifest
	 * @return
	 */
	public static List<JsonObject> convertToFabric(ModManifest manifest) {
		ArrayList<JsonObject> modJsons = new ArrayList<>();

		// Populate mods
		for (ModManifestEntry entry : manifest.getMods()) {
			JsonObject json = convertToFabric(manifest, entry);

			if (manifest.getMods().size() > 1 && modJsons.isEmpty()) {
				JsonArray arr = new JsonArray();

				for (ModManifestEntry mod : manifest.getMods()) {
					if (!mod.getModId().equals(entry.getModId())) {
						arr.add(mod.getModId());
					}
				}

				json.getAsJsonObject("custom").getAsJsonObject("patchwork:patcherMeta").add("children", arr);
			} else if (manifest.getMods().size() > 1 && !modJsons.isEmpty()) {
				json.getAsJsonObject("custom").getAsJsonObject("patchwork:patcherMeta").add("parent", modJsons.get(0).get("id"));
			}

			modJsons.add(json);
		}

		return modJsons;
	}

	/**
	 * Generates a basic fabric.mod.json.
	 * Does not include dependencies, entrypoints--including patchwork's!--, or mixins.
	 * These should all be handled somewhere else.
	 *
	 * @param manifest
	 * @param mod
	 * @return A basic fabric.mod.json
	 */

	public static JsonObject convertToFabric(ModManifest manifest, ModManifestEntry mod) {
		// Build the JSON
		JsonObject json = new JsonObject();

		json.addProperty("schemaVersion", 1);
		json.addProperty("id", mod.getModId());
		json.addProperty("version", mod.getVersion());
		json.addProperty("environment", "*");
		json.addProperty("name", mod.getDisplayName());

		json.add("depends", getDependencies(manifest, mod, true));
		json.add("suggests", getDependencies(manifest, mod, false));

		mod.getDescription().ifPresent(description -> json.addProperty("description", description.trim()));

		json.add("contact", getContactInformation(manifest, mod));

		mod.getAuthors().ifPresent(authors -> json.add("authors", convertAuthorsList(authors)));

		Optional<String> logo = mod.getLogoFile();

		if (!logo.isPresent()) {
			logo = manifest.getLogoFile();
		}

		JsonObject custom = new JsonObject();

		JsonObject patcherMeta = new JsonObject();
		patcherMeta.addProperty("patchedOn", ISO_UTC.format(new Date()));
		// TODO: don't hardcode develop
		patcherMeta.addProperty("patcherVersion", "develop");
		custom.add("patchwork:patcherMeta", patcherMeta);

		// modmenu flag TODO: https://github.com/Prospector/ModMenu/pull/167
		JsonObject modMenuData = new JsonObject();
		modMenuData.addProperty("loader", "forge");
		custom.add("patchwork:source", modMenuData);

		json.add("custom", custom);

		json.addProperty("icon", logo.orElse("assets/patchwork-generated/icon.png"));
		return json;
	}

	private static JsonObject getContactInformation(ModManifest manifest, ModManifestEntry mod) {
		JsonObject contact = new JsonObject();

		manifest.getIssueTrackerUrl().ifPresent(url -> contact.addProperty("issues", url));
		mod.getDisplayUrl().ifPresent(url -> contact.addProperty("homepage", url));

		return contact;
	}

	private static JsonArray convertAuthorsList(String authors) {
		String[] authorsList = authors.split(",");

		JsonArray array = new JsonArray();

		for (String author : authorsList) {
			array.add(author.trim());
		}

		return array;
	}

	private static JsonObject getDependencies(ModManifest manifest, ModManifestEntry mod, boolean mandatory) {
		JsonObject deps = new JsonObject();
		Map<String, List<ModManifestDependency>> dependencyMap = manifest.getDependencyMapping();

		if (dependencyMap.containsKey(mod.getModId())) {
			dependencyMap.get(mod.getModId()).forEach(c -> {
				if (c.isMandatory() == mandatory) {
					if (c.getModId().equals("forge")) {
						deps.addProperty("patchwork", "*");
					} else {
						// TODO convert version range styles
						deps.addProperty(c.getModId(), "*");
					}
				}
			});
		}

		return deps;
	}
}
