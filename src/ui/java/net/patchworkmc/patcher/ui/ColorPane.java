package net.patchworkmc.patcher.ui;

import java.awt.Color;
import java.util.function.Supplier;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

import org.apache.logging.log4j.Level;

public class ColorPane extends JTextPane {
	private static final Color D_Black = Color.getHSBColor(0.000f, 0.000f, 0.000f);
	private static final Color D_Red = Color.getHSBColor(0.000f, 1.000f, 0.502f);
	private static final Color D_Blue = Color.getHSBColor(0.667f, 1.000f, 0.502f);
	private static final Color D_Magenta = Color.getHSBColor(0.833f, 1.000f, 0.502f);
	private static final Color D_Green = Color.getHSBColor(0.333f, 1.000f, 0.502f);
	private static final Color D_Yellow = Color.getHSBColor(0.167f, 1.000f, 0.502f);
	private static final Color D_Cyan = Color.getHSBColor(0.500f, 1.000f, 0.502f);
	private static final Color D_White = Color.getHSBColor(0.000f, 0.000f, 0.753f);
	private static final Color B_Black = Color.getHSBColor(0.000f, 0.000f, 0.502f);
	private static final Color B_Red = Color.getHSBColor(0.000f, 1.000f, 1.000f);
	private static final Color B_Blue = Color.getHSBColor(0.667f, 1.000f, 1.000f);
	private static final Color B_Magenta = Color.getHSBColor(0.833f, 1.000f, 1.000f);
	private static final Color B_Green = Color.getHSBColor(0.333f, 1.000f, 1.000f);
	private static final Color B_Yellow = Color.getHSBColor(0.167f, 1.000f, 1.000f);
	private static final Color B_Cyan = Color.getHSBColor(0.500f, 1.000f, 1.000f);
	private static final Color B_White = Color.getHSBColor(0.000f, 0.000f, 1.000f);

	// TODO: instance-based and mutable when we have a dark mode
	private static final boolean IS_LIGHT = true;
	private static final Supplier<Color> cReset = () -> IS_LIGHT ? D_Black : D_White;

	// Cache to prevent looking up the color when it hasn't changed.
	private Color currentColor = D_Black;

	String remaining = "";

	private final Style oneStyleToRuleThemAll;

	public ColorPane() {
		this.oneStyleToRuleThemAll = this.addStyle("An interesting title.", null);
	}

	public void append(Color color, String string) {
		if (color != null) {
			StyleConstants.setForeground(oneStyleToRuleThemAll, color);
		}

		try {
			this.getDocument().insertString(this.getDocument().getLength(), string, oneStyleToRuleThemAll);
		} catch (BadLocationException e) {
			PatchworkUI.LOGGER.throwing(Level.ERROR, e);
		}
	}

	public void appendANSI(String s) {
		// convert ANSI color codes first
		int aPos = 0;   // current char position in addString
		int aIndex; // index of next Escape sequence
		int mIndex; // index of "m" terminating Escape sequence
		String tmpString = "";
		String addString = remaining + s;
		remaining = "";

		if (!addString.isEmpty()) {
			aIndex = addString.indexOf("\u001B"); // find first escape
			if (aIndex == -1) { // no escape/color change in this string, so just send it with current color
				append(currentColor, addString);
				return;
			}

			// otherwise There is an escape character in the string, so we must process it

			if (aIndex > 0) { // Escape is not first char, so send text up to first escape
				tmpString = addString.substring(0, aIndex);
				append(currentColor, tmpString);
				aPos = aIndex;
			}

			boolean stillSearching = true; // true until no more Escape sequences

			// aPos is now at the beginning of the first escape sequence
			while (stillSearching) {
				mIndex = addString.indexOf('m', aPos); // find the end of the escape sequence

				if (mIndex < 0) { // the buffer ends halfway through the ansi string!
					remaining = addString.substring(aPos);
					stillSearching = false;
					continue;
				} else {
					tmpString = addString.substring(aPos, mIndex + 1);
					currentColor = getANSIColor(tmpString);
				}

				aPos = mIndex + 1;
				// now we have the color, send text that is in that color (up to next escape)

				aIndex = addString.indexOf("\u001B", aPos);

				if (aIndex == -1) { // if that was the last sequence of the input, send remaining text
					tmpString = addString.substring(aPos);
					append(currentColor, tmpString);
					stillSearching = false;
					continue; // jump out of loop early, as the whole string has been sent now
				}

				// there is another escape sequence, so send part of the string and prepare for the next
				tmpString = addString.substring(aPos, aIndex);
				aPos = aIndex;
				append(currentColor, tmpString);
			} // while there's text in the input buffer
		}
	}

	private Color getANSIColor(String ANSIColor) {
		switch (ANSIColor) {
		case "\u001B[30m":
		case "\u001B[0;30m":
			// If we're in dark mode, blacks need to be white.
			return !IS_LIGHT ? D_Black : D_White;
		case "\u001B[31m":
		case "\u001B[0;31m":
			return D_Red;
		case "\u001B[32m":
		case "\u001B[0;32m":
			return D_Green;
		case "\u001B[33m":
		case "\u001B[0;33m":
			return D_Yellow;
		case "\u001B[34m":
		case "\u001B[0;34m":
			return D_Blue;
		case "\u001B[35m":
		case "\u001B[0;35m":
			return D_Magenta;
		case "\u001B[36m":
		case "\u001B[0;36m":
			return D_Cyan;
		case "\u001B[37m":
		case "\u001B[0;37m":
			// If we're in light mode, whites need to be black
			return IS_LIGHT ? D_White : D_Black;
		case "\u001B[1;30m":
			// Etc...
			return !IS_LIGHT ? B_Black : B_White;
		case "\u001B[1;31m":
			return B_Red;
		case "\u001B[1;32m":
			return B_Green;
		case "\u001B[1;33m":
			return B_Yellow;
		case "\u001B[1;34m":
			return B_Blue;
		case "\u001B[1;35m":
			return B_Magenta;
		case "\u001B[1;36m":
			return B_Cyan;
		case "\u001B[1;37m":
			return !IS_LIGHT ? B_White : B_Black;
		case "\u001B[0m":
			return cReset.get();
		default:
			return IS_LIGHT ? B_Black : B_White;
		}
	}
}
