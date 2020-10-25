package net.patchworkmc.patcher.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

public class VersionResolver {
	public static String getForgeVersion(MinecraftVersion minecraftVersion) {
		try {
			Document document = new SAXReader().read(new URL(ResourceDownloader.FORGE_MAVEN + "/net/minecraftforge/forge/maven-metadata.xml"));
			return findNewestVersion(document.selectNodes("/metadata/versioning/versions/version"), minecraftVersion);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		} catch (DocumentException ex) {
			throw new UncheckedIOException(new IOException(ex));
		}
	}

	/**
	 * This uses the fact that maven-metadata.xml has it's versions kept in oldest to newest version.
	 * To find the newest version of a Forge dependency for our Minecraft version, we just
	 * reverse the list and find the first (newest) dependency.
	 */
	private static String findNewestVersion(List<Node> nodes, MinecraftVersion minecraftVersion) {
		Collections.reverse(nodes);

		for (Node node : nodes) {
			if (node.getText().startsWith(minecraftVersion.getVersion())) {
				return node.getText();
			}
		}

		throw new IllegalArgumentException("Could not find a release of Forge for minecraft version " + minecraftVersion.getVersion());
	}
}
