package net.patchworkmc.patcher.annotation;

import java.util.function.Consumer;

import net.patchworkmc.patcher.Patchwork;

/**
 * Implements the
 * <a href="https://www.scala-lang.org/old/sites/default/files/sids/dubochet/Mon,%202010-05-31,%2015:25/Storage%20of%20pickled%20Scala%20signatures%20in%20class%20files.pdf">Scala Signature format</a>.
 */
public class ScalaSignatureHandler implements Consumer<String> {
	@Override
	public void accept(String signature) {
		byte[] bytes = new byte[(signature.length() * 7) / 8];
		int i = 0;
		int accumulator = 0;
		int bits = 0;

		for (char character : signature.toCharArray()) {
			if (character > 127) {
				Patchwork.LOGGER.error("Invalid byte in @ScalaSignature: %d was greater than 127", (int) character);
				return;
			}

			byte value = (byte) character;

			if (value == 0) {
				value = 0x7F;
			} else {
				value = (byte) (value - 1);
			}

			accumulator |= value << bits;
			bits += 7;

			if (bits >= 8) {
				bytes[i++] = (byte) (accumulator & 0xFF);
				accumulator >>>= 8;
				bits -= 8;
			}
		}

		StringBuilder hex = new StringBuilder();
		StringBuilder printable = new StringBuilder();

		for (byte value : bytes) {
			if ((value & 0xFF) < 16) {
				hex.append('0');
			}

			hex.append(Integer.toHexString(value & 0xFF).toUpperCase());
			hex.append(' ');

			if (Character.isDigit(value) || Character.isAlphabetic(value)) {
				printable.append((char) value);
			} else {
				printable.append(".");
			}
		}

		Patchwork.LOGGER.trace("Parsed @ScalaSignature (displaying unprintable characters as .): [" + hex + "]");
	}
}
