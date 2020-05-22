package com.patchworkmc;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Permission;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SizeRequirements;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.text.Element;
import javax.swing.text.ParagraphView;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.InlineView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.TinyUtils;

import com.patchworkmc.mapping.BridgedMappings;
import com.patchworkmc.mapping.RawMapping;
import com.patchworkmc.mapping.TinyWriter;
import com.patchworkmc.mapping.Tsrg;
import com.patchworkmc.mapping.TsrgClass;
import com.patchworkmc.mapping.TsrgMappings;

public class PatchworkUI {
	private static final String[] SUPPORTED_VERSIONS = {"1.14.4"};

	public static final Logger LOGGER = LogManager.getFormatterLogger("Patchwork/UI");
	private static Supplier<JTextPane> area = () -> null;
	private static JComboBox<String> versions;
	private static JTextField modsFolder;
	private static JTextField outputFolder;
	private static JCheckBox generateMCPTiny;
	private static JCheckBox generateDevJar;
	private static JCheckBox ignoreSidedAnnotations;
	private static JComboBox<YarnBuild> yarnVersions;
	private static File root = new File(System.getProperty("user.dir"));
	private static ExecutorService service = Executors.newScheduledThreadPool(4);
	private static PrintStream oldOut;
	private static PrintStream oldErr;

	public static void main(String[] args) throws Exception {
		new File(root, "input").mkdirs();
		new File(root, "output").mkdirs();

		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		JFrame frame = new JFrame("Patchwork Patcher");
		frame.setIconImage(Toolkit.getDefaultToolkit().getImage(PatchworkUI.class.getResource("/patchwork.png")));
		JPanel overallPane = new JPanel();
		frame.setContentPane(overallPane);
		overallPane.setLayout(new BorderLayout());

		ColorPane area = new ColorPane();
		PatchworkUI.area = () -> area;
		UIAppender.setPane(area);
		area.setEditable(false);
		area.setEditorKit(new HTMLEditorKit() {
			// Prevent serializable warning.
			private static final long serialVersionUID = -828745134521267417L;

			@Override
			public ViewFactory getViewFactory() {
				return new HTMLFactory() {
					@Override
					public View create(Element e) {
						View v = super.create(e);

						if (v instanceof InlineView) {
							return new InlineView(e) {
								@Override
								public int getBreakWeight(int axis, float pos, float len) {
									return GoodBreakWeight;
								}

								@Override
								public View breakView(int axis, int p0, float pos, float len) {
									if (axis == View.X_AXIS) {
										checkPainter();
										int p1 = getGlyphPainter().getBoundedPosition(this, p0, pos, len);

										if (p0 == getStartOffset() && p1 == getEndOffset()) {
											return this;
										}

										return createFragment(p0, p1);
									}

									return this;
								}
							};
						} else if (v instanceof ParagraphView) {
							return new ParagraphView(e) {
								@Override
								protected SizeRequirements calculateMinorAxisRequirements(int axis, SizeRequirements r) {
									if (r == null) {
										r = new SizeRequirements();
									}

									float pref = layoutPool.getPreferredSpan(axis);
									float min = layoutPool.getMinimumSpan(axis);
									r.minimum = (int) min;
									r.preferred = Math.max(r.minimum, (int) pref);
									r.maximum = Integer.MAX_VALUE;
									r.alignment = 0.5f;
									return r;
								}
							};
						}

						return v;
					}
				};
			}
		});
		area.setFont(area.getFont().deriveFont(14f));
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
				versions.addItemListener(e -> service.submit(() -> {
					try {
						updateYarnVersions();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}));
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
				ignoreSidedAnnotations = new JCheckBox("Ignore Sided Events", System.getProperty("patchwork:ignore_sided_annotations", "false").equals("true"));
				JPanel checkboxPanel = new JPanel(new BorderLayout());
				checkboxPanel.add(generateMCPTiny, BorderLayout.WEST);
				checkboxPanel.add(ignoreSidedAnnotations, BorderLayout.CENTER);
				checkboxPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
				pane.add(checkboxPanel);
			}

			{
				generateDevJar = new JCheckBox("Generate Development Jar", false);
				JPanel checkboxPanel = new JPanel(new BorderLayout());
				checkboxPanel.add(generateDevJar, BorderLayout.WEST);
				generateDevJar.addActionListener(e -> yarnVersions.setEnabled(generateDevJar.isSelected()));
				checkboxPanel.setBorder(new EmptyBorder(0, 0, 5, 0));
				pane.add(checkboxPanel);
			}

			{
				yarnVersions = new JComboBox<>();
				JPanel yarnPanel = new JPanel(new BorderLayout());
				yarnVersions.setEnabled(generateDevJar.isSelected());
				yarnPanel.add(new JLabel("Yarn Version:  "), BorderLayout.WEST);
				yarnPanel.add(yarnVersions, BorderLayout.CENTER);
				yarnPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
				pane.add(yarnPanel);
			}

			JPanel jPanel = new JPanel(new BorderLayout());

			{
				JButton clearCache = new JButton("Clear Cached Data");
				clearCache.addActionListener(e -> {
					jPanel.setVisible(false);
					service.submit(() -> {
						try {
							clearCache();
						} catch (Throwable throwable) {
							throwable.printStackTrace();
						}

						SwingUtilities.invokeLater(() -> jPanel.setVisible(true));
					});
				});
				JPanel clearCachePanel = new JPanel(new BorderLayout());
				clearCachePanel.add(clearCache, BorderLayout.WEST);
				clearCachePanel.setBorder(new EmptyBorder(0, 0, 10, 0));
				pane.add(clearCachePanel);
			}

			JPanel jPanel1 = new JPanel();
			jPanel1.add(pane);
			jPanel.add(jPanel1, BorderLayout.CENTER);

			JButton patchButton = new JButton("Patch");

			patchButton.addActionListener(e -> {
				jPanel.setVisible(false);
				service.submit(() -> {
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
				});
			});
			jPanel.add(patchButton, BorderLayout.SOUTH);

			overallPane.add(jPanel, BorderLayout.WEST);
		}

		frame.setMinimumSize(new Dimension(800, 300));
		frame.setSize(new Dimension(800, 500));
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		service.submit(() -> {
			try {
				updateYarnVersions();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		LOGGER.info("Welcome to Patchwork Patcher!");
		LOGGER.info("Patchwork is still an early project, things might not work as expected! Let us know the issues on GitHub!");
	}

	private static void updateYarnVersions() throws IOException {
		Gson gson = new GsonBuilder().disableHtmlEscaping().create();
		List<YarnBuild> builds = gson.fromJson(new InputStreamReader(new URL("https://meta.fabricmc.net/v2/versions/yarn").openStream()), new TypeToken<List<YarnBuild>>() {
		}.getType());
		SwingUtilities.invokeLater(() -> {
			yarnVersions.removeAllItems();

			for (YarnBuild build : builds) {
				if (build.gameVersion.equals(versions.getSelectedItem())) {
					yarnVersions.addItem(build);
				}
			}

			if (yarnVersions.getItemCount() > 0) {
				yarnVersions.setSelectedIndex(0);
			} else {
				yarnVersions.setSelectedIndex(-1);
			}
		});
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

	private static void clearCache() throws IOException {
		LOGGER.info("Clearing cache.");
		FileUtils.deleteDirectory(new File(root, "data"));
		FileUtils.deleteDirectory(new File(root, "temp"));
		LOGGER.info("Cleared cache.");
	}

	private static void startPatching() throws IOException {
		System.setProperty("patchwork:ignore_sided_annotations", ignoreSidedAnnotations.isSelected() + "");
		Path rootPath = root.toPath();
		String version = (String) versions.getSelectedItem();
		YarnBuild yarnBuild = PatchworkUI.generateDevJar.isSelected() ? (YarnBuild) yarnVersions.getSelectedItem() : null;

		LOGGER.info("Checking whether intermediary for %s exists...", version);
		loadOrDownloadIntermediary(version, new File(root, "data/mappings"));

		LOGGER.info("Checking whether MCPConfig for %s exists...", version);
		File voldemapTiny = new File(root, "data/mappings/voldemap-" + version + ".tiny");
		List<TsrgClass<RawMapping>> classes = Tsrg.readMappings(loadOrDownloadMCPConfig(version, new File(root, "data/mappings")));
		System.out.println("Creating tiny mappings provider...");
		IMappingProvider intermediary = TinyUtils.createTinyMappingProvider(rootPath.resolve("data/mappings/intermediary-" + version + ".tiny"), "official", "intermediary");

		System.out.println("Creating tsrg mappings...");
		TsrgMappings mappings = new TsrgMappings(classes, intermediary);

		File voldemapBridged = new File(root, "data/mappings/voldemap-bridged-" + version + ".tiny");

		IMappingProvider bridged;
		IMappingProvider bridgedInverted;

		if (!voldemapBridged.exists()) {
			System.out.println("Generating bridged (srg -> intermediary) tiny mappings...");

			TinyWriter tinyWriter = new TinyWriter("srg", "intermediary");
			bridged = new BridgedMappings(mappings, intermediary);
			bridged.load(tinyWriter);
			Files.write(voldemapBridged.toPath(), tinyWriter.toString().getBytes(StandardCharsets.UTF_8));

			System.out.println("Using generated bridged (srg -> intermediary) tiny mappings");
		} else {
			System.out.println("Using cached bridged (srg -> intermediary) tiny mappings");
			bridged = TinyUtils.createTinyMappingProvider(voldemapBridged.toPath(), "srg", "intermediary");
		}

		bridgedInverted = TinyUtils.createTinyMappingProvider(voldemapBridged.toPath(), "intermediary", "srg");

		if (yarnBuild != null) {
			LOGGER.info("Checking whether yarn for %s exists...", yarnBuild.toString());
			downloadYarn(yarnBuild, new File(root, "data/mappings"));
		}

		if (generateMCPTiny.isSelected()) {
			LOGGER.info("Generating tiny MCP.");

			if (voldemapTiny.exists()) {
				LOGGER.info("Tiny MCP already exists. deleting existing tiny file.");
				Files.delete(voldemapTiny.toPath());
			}

			LOGGER.info("Generating tiny MCP from tsrg data.");
			TinyWriter tinyWriter = new TinyWriter("official", "srg");
			mappings.load(tinyWriter);
			String tiny = tinyWriter.toString();
			Files.write(voldemapTiny.toPath(), tiny.getBytes(StandardCharsets.UTF_8));
			LOGGER.info("Generated tiny MCP.");
		}

		Files.createDirectories(rootPath.resolve("input"));
		Files.createDirectories(rootPath.resolve("temp"));
		Files.createDirectories(rootPath.resolve("output"));

		Path officialJar = rootPath.resolve("data/" + version + "-client+official.jar");
		Path srgJar = rootPath.resolve("data/" + version + "-client+srg.jar");

		IMappingProvider[] yarnMappings = {null};

		{
			if (!officialJar.toFile().exists()) {
				LOGGER.info("Trying to download Minecraft " + version + " client jar.");
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
							LOGGER.info("Downloading Minecraft client " + version + ".");
							FileUtils.copyURLToFile(new URL(versionJarUrl), officialJar.toFile());
							LOGGER.info("Downloaded Minecraft client " + version + ".");
							break;
						}
					}
				}

				if (!officialJar.toFile().exists()) {
					throw new IllegalStateException("Failed to find Minecraft version " + version);
				}
			} else {
				LOGGER.info("Minecraft jar already exists for Minecraft " + version + ".");
			}

			if (!srgJar.toFile().exists()) {
				LOGGER.info("Remapping Minecraft (official -> srg)");
				Patchwork.remap(mappings, officialJar, srgJar);
			}

			if (yarnBuild != null) {
				Path intermediaryJar = rootPath.resolve("data/" + version + "-client+intermediary.jar");
				yarnMappings[0] = TinyUtils.createTinyMappingProvider(rootPath.resolve("data/mappings/yarn-" + yarnBuild.version + "-v2.tiny"), "intermediary", "named");

				if (!intermediaryJar.toFile().exists()) {
					LOGGER.info("Remapping Minecraft (official -> intermediary)");
					Patchwork.remap(intermediary, officialJar, intermediaryJar);
				}
			}
		}

		LOGGER.info("Preparation Complete!\n");

		Path inputFolder = new File(modsFolder.getText()).toPath();
		Path outputFolder = new File(PatchworkUI.outputFolder.getText()).toPath();
		Path dataFolder = rootPath.resolve("data");
		Path tempFolder = Files.createTempDirectory(new File(System.getProperty("java.io.tmpdir")).toPath(), "patchwork-patcher-ui");
		List<IMappingProvider> devMappings = generateDevJar.isSelected() ? Collections.singletonList(yarnMappings[0]) : Collections.emptyList();

		Patchwork patchwork = new Patchwork(inputFolder, outputFolder, dataFolder, tempFolder, bridged, bridgedInverted, devMappings);

		int patched = patchwork.patchAndFinish();
		LOGGER.info("Successfully patched " + patched + " mod(s)!");
	}

	private static void downloadYarn(YarnBuild yarnBuild, File parent) throws IOException {
		parent.mkdirs();
		File file = new File(parent, "yarn-" + yarnBuild.version + "-v2.tiny");

		if (!file.exists()) {
			LOGGER.info("Downloading Yarn for " + yarnBuild.version + ".");
			InputStream stream = new URL("https://maven.fabricmc.net/" + yarnBuild.maven.replace(yarnBuild.version, "").replace('.', '/').replace(':', '/') + yarnBuild.version + "/" + "yarn-" + yarnBuild.version + "-v2.jar").openStream();
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
					LOGGER.info("Downloaded Yarn for " + yarnBuild.version + ".");
					break;
				}
			}

			zipInputStream.close();
		} else {
			LOGGER.info("Yarn for " + yarnBuild.version + " already exists, using downloaded data.");
		}
	}

	public static InputStream loadOrDownloadMCPConfig(String version, File parent) throws IOException {
		parent.mkdirs();
		File file = new File(parent, "voldemap-" + version + ".tsrg");

		if (!file.exists()) {
			LOGGER.info("Downloading MCPConfig for " + version + ".");
			InputStream stream = new URL("http://files.minecraftforge.net/maven/de/oceanlabs/mcp/mcp_config/" + version + "/mcp_config-" + version + ".zip").openStream();
			ZipInputStream zipInputStream = new ZipInputStream(stream);

			while (true) {
				ZipEntry nextEntry = zipInputStream.getNextEntry();

				if (nextEntry == null) {
					break;
				}

				if (!nextEntry.isDirectory() && nextEntry.getName().endsWith("/joined.tsrg")) {
					FileWriter writer = new FileWriter(file, false);
					IOUtils.copy(zipInputStream, writer, Charset.defaultCharset());
					writer.close();
					LOGGER.info("Downloaded MCPConfig for " + version + ".");
					break;
				}
			}
		} else {
			LOGGER.info("MCPConfig for " + version + " already exists, using downloaded data.");
		}

		return new FileInputStream(file);
	}

	public static InputStream loadOrDownloadIntermediary(String version, File parent) throws IOException {
		parent.mkdirs();
		File file = new File(parent, "intermediary-" + version + ".tiny");

		if (!file.exists()) {
			LOGGER.info("Downloading Intermediary for " + version + ".");
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
					LOGGER.info("Downloaded intermediary for " + version + ".");
					break;
				}
			}
		} else {
			LOGGER.info("Intermediary for " + version + " already exists, using downloaded data.");
		}

		return new FileInputStream(file);
	}

	@SuppressWarnings("unused")
	private static class YarnBuild {
		String gameVersion;
		String separator;
		int build;
		String maven;
		String version;
		boolean stable;

		@Override
		public String toString() {
			return version;
		}
	}

	private static class ExitTrappedException extends SecurityException {
		// Prevent serializable warning.
		private static final long serialVersionUID = -8774888159798495064L;
	}
}
