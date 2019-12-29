package com.patchworkmc.mapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Tsrg {
	private Tsrg() {
		// NO-OP
	}

	public static List<TsrgClass<RawMapping>> readMappings(InputStream stream) throws IOException {
		InputStreamReader streamReader = new InputStreamReader(stream);
		BufferedReader reader = new BufferedReader(streamReader);

		List<TsrgClass<RawMapping>> classes = new ArrayList<>();
		TsrgClass<RawMapping> last = null;
		String line;

		try {
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(" ");

				if (line.startsWith("\t")) {
					if (last == null) {
						throw new IllegalStateException("A .tsrg file cannot start with a tabbed entry!");
					}

					String official = parts[0].trim();

					if (parts.length == 2) {
						// Field

						String mcp = parts[1];

						last.addField(new RawMapping(official, mcp));
					} else {
						// Method

						String description = parts[1];
						String mcp = parts[2];

						last.addMethod(new Mapping(official, mcp, description));
					}
				} else {
					String official = parts[0];
					String mcp = parts[1];

					last = new TsrgClass<>(official, mcp);

					classes.add(last);
				}
			}
		} finally {
			reader.close();
			streamReader.close();
		}

		return classes;
	}
}
