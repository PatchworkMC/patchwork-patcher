package net.patchworkmc.patcher;

import java.io.File;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// TODO: This should be a full CLI instead of just a wrapper with the default options.
public class PatchworkCLI {
	public static final Logger LOGGER = LogManager.getLogger(PatchworkCLI.class);

	public static void main(String[] args) throws Exception {
		Path current = new File(System.getProperty("user.dir")).toPath();

		Patchwork.create(current.resolve("input"), current.resolve("output"), current.resolve("data")).patchAndFinish();
	}
}
