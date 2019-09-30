package net.coderbot.patchwork.manifest.converter;

import net.coderbot.patchwork.manifest.forge.ModManifest;
import net.coderbot.patchwork.manifest.forge.ModManifestDependency;
import net.coderbot.patchwork.manifest.forge.ModManifestEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ModManifestConverter {
	public static JsonObject convertToFabric(ModManifest manifest) {
		if(manifest.getMods().size() != 1) {
			// TODO: multiple mod manifests
			throw new UnsupportedOperationException(
					"Cannot process Forge mod manifests with multiple mods yet");
		}

		ModManifestEntry mod = manifest.getMods().get(0);
		List<ModManifestDependency> dependencies =
				manifest.getDependencyMapping().get(mod.getModId());

		if(dependencies == null) {
			dependencies = new ArrayList<>();
		}

		return convertToFabric(manifest, mod, dependencies);
	}

	public static JsonObject convertToFabric(ModManifest manifest,
			ModManifestEntry mod,
			List<ModManifestDependency> dependencies) {
		// Build the JSON

		// TODO: Dependencies, mixins, entrypoints

		JsonObject json = new JsonObject();

		json.addProperty("schemaVersion", 1);
		json.addProperty("id", mod.getModId());
		json.addProperty("version", mod.getVersion());
		json.addProperty("environment", "*");
		json.addProperty("name", mod.getDisplayName());

		mod.getDescription().ifPresent(
				description -> json.addProperty("description", description.trim()));

		json.add("contact", getContactInformation(manifest, mod));

		mod.getAuthors().ifPresent(authors -> json.add("authors", convertAuthorsList(authors)));

		Optional<String> logo = mod.getLogoFile();

		if(!logo.isPresent()) {
			logo = manifest.getLogoFile();
		}

		logo.ifPresent(icon -> json.addProperty("icon", icon));

		dependencies.removeIf(entry -> entry.getModId().equals("forge"));

		if(dependencies.size() != 0) {
			// TODO: throw new UnsupportedOperationException("Cannot write dependencies yet!");
		}

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

		for(String author : authorsList) {
			array.add(author.trim());
		}

		return array;
	}
}
