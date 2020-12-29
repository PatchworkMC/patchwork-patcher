package net.patchworkmc.patcher.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

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

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.patchworkmc.patcher.Patchwork;
import net.patchworkmc.patcher.util.MinecraftVersion;

public class PatchworkUI {
	private static final String[] SUPPORTED_VERSIONS = Arrays.stream(MinecraftVersion.values()).map(MinecraftVersion::getVersion).toArray(String[]::new);

	public static final Logger LOGGER = LogManager.getLogger(PatchworkUI.class);
	private static Supplier<JTextPane> area = () -> null;
	private static JComboBox<String> versions;
	private static JTextField modsFolder;
	private static JTextField outputFolder;
	private static JCheckBox ignoreSidedAnnotations;
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
						try {
							startPatching();
						} catch (Throwable throwable) {
							throwable.printStackTrace();
						}
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
		LOGGER.info("Welcome to Patchwork Patcher!");
		LOGGER.info("Patchwork is still an early project, things might not work as expected! Let us know the issues on GitHub!");
	}

	private static void clearCache() throws IOException {
		LOGGER.info("Clearing cache.");
		FileUtils.deleteDirectory(new File(root, "data"));
		LOGGER.info("Cleared cache.");
	}

	private static void startPatching() throws IOException {
		Path current = root.toPath();
		// find the selected minecraft version
		MinecraftVersion version = MinecraftVersion.valueOf("V" + ((String) versions.getSelectedItem()).replace('.', '_'));
		Patchwork patchwork = Patchwork.create(new File(modsFolder.getText()).toPath(), new File(outputFolder.getText()).toPath(), current.resolve("data"), version);
		LOGGER.info("Successfully patched {} mods!", patchwork.patchAndFinish());
	}

	private static class ExitTrappedException extends SecurityException {
		// Prevent serializable warning.
		private static final long serialVersionUID = -8774888159798495064L;
	}
}
