/*
 * Copyright 2026 The Forbric Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package net.forbric.kernel;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Assumptions;

/**
 * Fixtures a test reads that a clean checkout does not have: the staged game artifacts, the compiled game side
 * (built only when those artifacts are present), and the local mod packs under {@code run/}.
 *
 * <p>CI is such a checkout, and it must stay green: a missing fixture skips the test there. A compatibility run
 * sets {@code FORBRIC_COMPAT_FIXTURES_REQUIRED=1}, and then a missing fixture is a failure, so a machine that is
 * supposed to have them cannot pass by quietly skipping.
 */
public final class TestFixtures {
	private TestFixtures() {
	}

	/** Skips the test when {@code present} is false, or fails it when fixtures are required. */
	public static void require(boolean present, String what) {
		if ("1".equals(System.getenv("FORBRIC_COMPAT_FIXTURES_REQUIRED"))) assertTrue(present, what);
		Assumptions.assumeTrue(present, what);
	}

	/** {@link #require} for files: every path must be a regular file. */
	public static void requireFiles(String what, Path... files) {
		for (Path file : files) require(Files.isRegularFile(file), what + ": " + file);
	}

	/** {@link #require} for a directory, such as a local mod pack. */
	public static void requireDirectory(String what, Path directory) {
		require(Files.isDirectory(directory), what + ": " + directory);
	}

	/**
	 * The Minecraft directory the game-side compile read its libraries from. Gradle hands it to every test task as
	 * {@code MC_DIR} (tools/dev.py's {@code .dev/minecraft} when prepared); outside Gradle it falls back to the
	 * launcher's usual location on this platform, the same default build.gradle uses.
	 */
	public static Path minecraftDir() {
		String env = System.getenv("MC_DIR");
		if (env != null && !env.isBlank()) return Path.of(env);
		String home = System.getProperty("user.home");
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		if (os.contains("windows")) {
			String appData = System.getenv("APPDATA");
			return Path.of(appData != null ? appData : home + "/AppData/Roaming", ".minecraft");
		}
		if (os.contains("mac")) return Path.of(home, "Library", "Application Support", "minecraft");
		return Path.of(home, ".minecraft");
	}

	/** Minecraft 26.2's own client jar inside {@link #minecraftDir()}. */
	public static Path vanillaJar() {
		return minecraftDir().resolve("versions/26.2/26.2.jar");
	}

	/**
	 * Netty's codec library under {@link #minecraftDir()}: 26.2 ships netty 4.2's split {@code netty-codec-base},
	 * which a launcher directory that also holds older versions keeps beside their {@code netty-codec}.
	 */
	public static String nettyCodecLibrary() {
		return Files.isDirectory(minecraftDir().resolve("libraries/io/netty/netty-codec-base"))
				? "io/netty/netty-codec-base" : "io/netty/netty-codec";
	}

	/** The Fabric API build the real-bytecode tests are written against. */
	public static final String FABRIC_API_JAR = "fabric-api-0.155.2+26.2.jar";

	/**
	 * {@link #FABRIC_API_JAR}: the local merged pack's copy when there is one, else the jar the game side compiled
	 * against ({@code forbric.fabricApi}, which tools/dev.py fills with the same pinned file). Another build passed
	 * there is not used: these tests read classes and mixins of this exact one.
	 */
	public static Path fabricApi() {
		Path pack = Path.of("run/client-merged-pack/mods", FABRIC_API_JAR);
		if (Files.isRegularFile(pack)) return pack;
		String configured = System.getProperty("forbric.fabricApi");
		if (configured == null || configured.isBlank()) return pack;
		Path compiled = Path.of(configured);
		return compiled.getFileName().toString().equals(FABRIC_API_JAR) ? compiled : pack;
	}
}
