package com.patchworkmc;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Permission;
import java.util.List;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.patchworkmc.mapping.RawMapping;
import com.patchworkmc.mapping.Tsrg;
import com.patchworkmc.mapping.TsrgClass;
import com.patchworkmc.mapping.TsrgMappings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import net.fabricmc.mappings.Mappings;
import net.fabricmc.mappings.MappingsProvider;
import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.TinyUtils;

public class PatchworkUI {
	private static final String[] SUPPORTED_VERSIONS = {"1.14.4"};

	private static Supplier<JTextArea> area = () -> null;
	private static JComboBox<String> versions;
	private static JTextField modsFolder;
	private static JTextField outputFolder;
	private static JCheckBox generateMCPTiny;
	private static File root = new File(System.getProperty("user.dir"));

	public static void main(String[] args) throws Exception {
		new File(root, "input").mkdirs();
		new File(root, "output").mkdirs();

		setupConsole();
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		JFrame frame = new JFrame("Patchwork Patcher");
		JPanel overallPane = new JPanel();
		frame.setContentPane(overallPane);
		overallPane.setLayout(new BorderLayout());

		JTextArea area = new JTextArea();
		PatchworkUI.area = () -> area;
		area.setEditable(false);
		area.setLineWrap(true);
		JScrollPane scrollPane = new JScrollPane(area, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		overallPane.add(scrollPane, BorderLayout.CENTER);

		{
			JPanel pane = new JPanel();
			pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

			{
				JLabel title = new JLabel("Patchwork Patcher");
				title.setAlignmentX(Component.CENTER_ALIGNMENT);
				title.setBorder(new EmptyBorder(10, 10, 10, 10));
				title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
				pane.add(title);
			}

			{
				PatchworkUI.versions = new JComboBox<>(SUPPORTED_VERSIONS);
				JPanel versionsPane = new JPanel(new BorderLayout());
				versionsPane.add(new JLabel("Minecraft Version:  "), BorderLayout.WEST);
				versionsPane.add(versions, BorderLayout.CENTER);
				versionsPane.setBorder(new EmptyBorder(0, 0, 10, 0));
				pane.add(versionsPane);
			}

			{
				PatchworkUI.modsFolder = new JTextField(new File(root, "input").getAbsolutePath(), 20);
				JButton button = new JButton("Browse");
				button.addActionListener(e -> {
					JFileChooser chooser = new JFileChooser();
					File file = null;

					try {
						file = new File(modsFolder.getName());
					} catch (Exception ignored) {
						// ignored
					}

					chooser.setCurrentDirectory(file != null && file.exists() ? file : new File(root, "input"));
					chooser.setDialogTitle("Browse Input Mods Folder");
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					chooser.setAcceptAllFileFilterUsed(false);

					if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
						if (chooser.getSelectedFile() != null) {
							modsFolder.setText(chooser.getSelectedFile().getAbsolutePath());
						} else if (chooser.getCurrentDirectory() != null) {
							modsFolder.setText(chooser.getCurrentDirectory().getAbsolutePath());
						}

						modsFolder.requestFocus();
						modsFolder.setCaretPosition(modsFolder.getDocument().getLength());
					}
				});
				JPanel modsPane = new JPanel(new BorderLayout());
				modsPane.add(new JLabel("Input Mods:  "), BorderLayout.WEST);
				modsPane.add(modsFolder, BorderLayout.CENTER);
				modsPane.add(button, BorderLayout.EAST);
				modsPane.setBorder(new EmptyBorder(0, 0, 5, 0));
				pane.add(modsPane);
			}

			{
				PatchworkUI.outputFolder = new JTextField(new File(root, "output").getAbsolutePath(), 20);
				JButton button = new JButton("Browse");
				button.addActionListener(e -> {
					JFileChooser chooser = new JFileChooser();
					File file = null;

					try {
						file = new File(outputFolder.getName());
					} catch (Exception ignored) {
						// ignored
					}

					chooser.setCurrentDirectory(file != null && file.exists() ? file : new File(root, "output"));
					chooser.setDialogTitle("Browse Output Folder");
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					chooser.setAcceptAllFileFilterUsed(false);

					if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
						if (chooser.getSelectedFile() != null) {
							outputFolder.setText(chooser.getSelectedFile().getAbsolutePath());
						} else if (chooser.getCurrentDirectory() != null) {
							outputFolder.setText(chooser.getCurrentDirectory().getAbsolutePath());
						}

						outputFolder.requestFocus();
						outputFolder.setCaretPosition(outputFolder.getDocument().getLength());
					}
				});
				JPanel outputPane = new JPanel(new BorderLayout());
				outputPane.add(new JLabel("Output Folder:  "), BorderLayout.WEST);
				outputPane.add(outputFolder, BorderLayout.CENTER);
				outputPane.add(button, BorderLayout.EAST);
				outputPane.setBorder(new EmptyBorder(0, 0, 10, 0));
				pane.add(outputPane);
			}

			{
				generateMCPTiny = new JCheckBox("Generate Tiny MCP", false);
				JPanel mcpTiny = new JPanel(new BorderLayout());
				mcpTiny.add(generateMCPTiny, BorderLayout.WEST);
				mcpTiny.setBorder(new EmptyBorder(0, 0, 10, 0));
				pane.add(mcpTiny);
			}

			JPanel jPanel = new JPanel(new BorderLayout());

			{
				JButton clearCache = new JButton("Clear Cached Data");
				clearCache.addActionListener(e -> {
					jPanel.setVisible(false);
					new Thread(() -> {
						try {
							clearCache();
						} catch (Throwable throwable) {
							throwable.printStackTrace();
						}

						SwingUtilities.invokeLater(() -> jPanel.setVisible(true));
					}).start();
				});
				JPanel clearCachePanel = new JPanel(new BorderLayout());
				clearCachePanel.add(clearCache, BorderLayout.WEST);
				clearCachePanel.setBorder(new EmptyBorder(0, 0, 10, 0));
				pane.add(clearCachePanel);
			}

			JPanel jPanel1 = new JPanel();
			jPanel1.add(pane);
			jPanel.add(jPanel1, BorderLayout.CENTER);
			jPanel.setPreferredSize(new Dimension(400, 500));

			JButton doneButton = new JButton("Done");

			doneButton.addActionListener(e -> {
				jPanel.setVisible(false);
				new Thread(() -> {
					try {
						runWithNoExitCall(() -> {
							try {
								startPatching();
							} catch (Throwable throwable) {
								throwable.printStackTrace();
							}
						});
					} catch (Throwable throwable) {
						throwable.printStackTrace();
					}

					SwingUtilities.invokeLater(() -> jPanel.setVisible(true));
				}).start();
			});
			jPanel.add(doneButton, BorderLayout.SOUTH);

			overallPane.add(jPanel, BorderLayout.WEST);
		}

		frame.setMinimumSize(new Dimension(800, 300));
		frame.setSize(new Dimension(800, 500));
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		System.out.println("Welcome to Patchwork Patcher!\nPatchwork is still an early project, things might not work as expected! Let us know the issues on GitHub!");
	}

	private static void runWithNoExitCall(Runnable runnable) {
		forbidSystemExitCall();
		runnable.run();
		enableSystemExitCall();
	}

	private static void forbidSystemExitCall() {
		final SecurityManager securityManager = new SecurityManager() {
			@Override
			public void checkPermission(Permission perm) {
				if (perm.getName().contains("exitVM")) {
					throw new ExitTrappedException();
				}
			}
		};
		System.setSecurityManager(securityManager);
	}

	private static void enableSystemExitCall() {
		System.setSecurityManager(null);
	}

	private static void clearCache() throws Throwable {
		System.out.println("\nClearing cache.");
		FileUtils.deleteDirectory(new File(root, "data"));
		FileUtils.deleteDirectory(new File(root, "temp"));
		System.out.println("Cleared cache.");
	}

	private static void startPatching() throws Throwable {
		System.out.println("");
		Path rootPath = root.toPath();
		String version = (String) versions.getSelectedItem();
		System.out.printf("Checking whether intermediary for %s exists...%n", version);
		Mappings intermediary = MappingsProvider.readTinyMappings(loadOrDownloadIntermediary(version, new File(root, "data/mappings")));
		System.out.printf("Checking whether MCPConfig for %s exists...%n", version);
		File voldemapTiny = new File(root, "data/mappings/voldemap-" + version + ".tiny");
		List<TsrgClass<RawMapping>> classes = Tsrg.readMappings(loadOrDownloadMCPConfig(version, new File(root, "data/mappings")));
		IMappingProvider intermediaryMappings = TinyUtils.createTinyMappingProvider(rootPath.resolve("data/mappings/intermediary-" + version + ".tiny"), "official", "intermediary");
		System.out.println("Created tiny mappings provider.");
		TsrgMappings mappings = new TsrgMappings(classes, intermediary, "official");
		System.out.println("Created tsrg mappings.");

		if (generateMCPTiny.isSelected()) {
			System.out.println("Generating tiny MCP.");

			if (voldemapTiny.exists()) {
				System.out.println("Tiny MCP already exists. deleting existing tiny file.");
				voldemapTiny.delete();
			}

			System.out.println("Generating tiny MCP from tsrg data.");
			String tiny = mappings.writeTiny("srg");
			Files.write(voldemapTiny.toPath(), tiny.getBytes(StandardCharsets.UTF_8));
			System.out.println("Generated tiny MCP.");
		}

		Files.createDirectories(rootPath.resolve("input"));
		Files.createDirectories(rootPath.resolve("temp"));
		Files.createDirectories(rootPath.resolve("output"));

		Path officialJar = rootPath.resolve("data/" + version + "-client+official.jar");
		Path srgJar = rootPath.resolve("data/" + version + "-client+srg.jar");

		{
			if (!Files.exists(officialJar)) {
				System.out.println("Trying to download Minecraft " + version + " client jar.");
				Gson gson = new GsonBuilder().disableHtmlEscaping().create();
				JsonArray versions = gson.fromJson(new InputStreamReader(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json").openStream()), JsonObject.class).get("versions").getAsJsonArray();
				Files.deleteIfExists(srgJar);

				for (JsonElement jsonElement : versions) {
					if (jsonElement.isJsonObject()) {
						JsonObject object = jsonElement.getAsJsonObject();
						String id = object.get("id").getAsJsonPrimitive().getAsString();

						if (id.equals(version)) {
							String versionUrl = object.get("url").getAsJsonPrimitive().getAsString();
							JsonObject versionMeta = gson.fromJson(new InputStreamReader(new URL(versionUrl).openStream()), JsonObject.class);
							String versionJarUrl = versionMeta.get("downloads").getAsJsonObject().get("client").getAsJsonObject().get("url").getAsJsonPrimitive().getAsString();
							System.out.println("Downloading Minecraft client " + version + ".");
							FileUtils.copyURLToFile(new URL(versionJarUrl), officialJar.toFile());
							System.out.println("Downloaded Minecraft client " + version + ".");
							break;
						}
					}
				}

				if (!Files.exists(officialJar)) {
					throw new IllegalStateException("Failed to find Minecraft version " + version);
				}
			} else {
				System.out.println("Minecraft jar already exists for Minecraft " + version + ".");
			}

			if (!Files.exists(srgJar)) {
				System.out.println("Remapping Minecraft (official -> srg)");
				Patchwork.remap(mappings, officialJar, srgJar);
			}
		}

		System.out.println("Preparation Complete!\n");

		File inputFolder = new File(modsFolder.getText());
		Path outputFolder = new File(PatchworkUI.outputFolder.getText()).toPath();
		int[] patched = {0};

		Files.walk(inputFolder.toPath()).forEach(path -> {
			if (!path.toString().endsWith(".jar")) {
				return;
			}

			String modName = path.getFileName().toString().replaceAll(".jar", "");
			System.out.println("=== Patching " + path.toString() + " ===");

			try {
				Patchwork.transformMod(rootPath, path, outputFolder, modName, mappings, intermediaryMappings);
				patched[0]++;
			} catch (Throwable t) {
				System.err.println("Transformation failed, skipping current mod!");

				t.printStackTrace();
			}
		});
		System.out.println("\nSuccessfully patched " + patched[0] + " mod(s)!");
	}

	private static void writeToArea(char c) {
		SwingUtilities.invokeLater(() -> {
			JTextArea area = PatchworkUI.area.get();

			if (area == null) {
				return;
			}

			area.append(String.valueOf(c));
			area.requestFocus();
			area.setCaretPosition(area.getDocument().getLength());
		});
	}

	private static void setupConsole() {
		PrintStream out = System.out;
		System.setOut(new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				writeToArea((char) b);
				out.write(b);
			}
		}));
		PrintStream err = System.err;
		System.setErr(new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				writeToArea((char) b);
				err.write(b);
			}
		}));
	}

	public static InputStream loadOrDownloadMCPConfig(String version, File parent) throws IOException {
		parent.mkdirs();
		File file = new File(parent, "voldemap-" + version + ".tsrg");

		if (!file.exists()) {
			System.out.println("Downloading MCP Config for " + version + ".");
			InputStream stream = new URL("http://files.minecraftforge.net/maven/de/oceanlabs/mcp/mcp_config/" + version + "/mcp_config-" + version + ".zip").openStream();
			ZipInputStream zipInputStream = new ZipInputStream(stream);

			while (true) {
				ZipEntry nextEntry = zipInputStream.getNextEntry();
				if (nextEntry == null) break;

				if (!nextEntry.isDirectory() && nextEntry.getName().endsWith("/joined.tsrg")) {
					FileWriter writer = new FileWriter(file, false);
					IOUtils.copy(zipInputStream, writer, Charset.defaultCharset());
					writer.close();
					System.out.println("Downloaded MCPConfig for " + version + ".");
					break;
				}
			}
		} else {
			System.out.println("MCPConfig for " + version + " already exists, using downloaded data.");
		}

		return new FileInputStream(file);
	}

	public static InputStream loadOrDownloadIntermediary(String version, File parent) throws IOException {
		parent.mkdirs();
		File file = new File(parent, "intermediary-" + version + ".tiny");

		if (!file.exists()) {
			System.out.println("Downloading Intermediary for " + version + ".");
			InputStream stream = new URL("https://maven.fabricmc.net/net/fabricmc/intermediary/" + version + "/intermediary-" + version + ".jar").openStream();
			ZipInputStream zipInputStream = new ZipInputStream(stream);

			while (true) {
				ZipEntry nextEntry = zipInputStream.getNextEntry();

				if (nextEntry == null) {
					break;
				}

				if (!nextEntry.isDirectory() && nextEntry.getName().endsWith("/mappings.tiny")) {
					FileWriter writer = new FileWriter(file, false);
					IOUtils.copy(zipInputStream, writer, Charset.defaultCharset());
					writer.close();
					System.out.println("Downloaded intermediary for " + version + ".");
					break;
				}
			}
		} else {
			System.out.println("Intermediary for " + version + " already exists, using downloaded data.");
		}

		return new FileInputStream(file);
	}

	private static class ExitTrappedException extends SecurityException {
	}
}
