package net.patchworkmc.patcher.ui;

import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.JsonLayout;

@Plugin(name = "UIAppender", category = "Core", elementType = "appender", printObject = true)
public class UIAppender extends AbstractAppender {
	private static ColorPane pane;
	private final int maxLines;

	private UIAppender(String name, Layout<?> layout, Filter filter, int maxLines, boolean ignoreExceptions) {
		super(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);
		this.maxLines = maxLines;
	}

	@SuppressWarnings("unused")
	@PluginFactory
	public static UIAppender createAppender(@PluginAttribute("name") String name, @PluginAttribute("maxLines") int maxLines,
				@PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
				@PluginElement("Layout") Layout<?> layout, @PluginElement("Filters") Filter filter) {
		if (name == null) {
			LOGGER.error("No name provided for UIAppender");
			return null;
		}

		if (layout == null) {
			layout = JsonLayout.createDefaultLayout();
		}

		return new UIAppender(name, layout, filter, maxLines, ignoreExceptions);
	}

	public static void setPane(ColorPane pane) {
		UIAppender.pane = pane;
	}

	@Override
	public void append(LogEvent event) {
		if (pane == null) {
			return;
		}

		String message = new String(this.getLayout().toByteArray(event));

		try {
			SwingUtilities.invokeLater(() -> {
				try {
					pane.appendANSI(message);
					Document document = pane.getDocument();
					String text = document.getText(0, document.getLength());
					int lines = text.split("\n").length;

					if (maxLines > 0 && lines > maxLines) {
						document.remove(0, ordinalIndexOf(text, "\n", lines - maxLines));
					}
				} catch (BadLocationException ex) {
					LOGGER.throwing(Level.FATAL, ex);
				}
			});
		} catch (IllegalStateException ex) {
			LOGGER.throwing(Level.FATAL, ex);
		}
	}

	private static int ordinalIndexOf(String str, String substr, int n) {
		int pos = str.indexOf(substr);

		while (--n > 0 && pos != -1) {
			pos = str.indexOf(substr, pos + 1);
		}

		return pos;
	}
}
