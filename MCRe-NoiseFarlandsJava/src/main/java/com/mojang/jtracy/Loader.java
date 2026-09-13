package com.mojang.jtracy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

class Loader {
    private final String name;

    Loader() {
        final String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        final String osArch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);

        String prefix = "";
        final String name = "jtracy-jni";
        String suffix = "";

        switch (osArch) {
            case "amd64", "x86_64", "x86-64" -> {
                if (osName.contains("win")) {
                    suffix = "-windows.dll";
                } else if (osName.contains("mac") || osName.contains("darwin")) {
                    prefix = "lib";
                    suffix = "-macos.dylib";
                } else if (osName.contains("linux") || osName.contains("unix")) {
                    prefix = "lib";
                    suffix = "-linux.so";
                } else {
                    throw new UnsatisfiedLinkError("Unsupported OS name: " + osName + " / " + osArch);
                }
            }
            case "aarch64" -> {
                if (osName.contains("mac") || osName.contains("darwin")) {
                    prefix = "lib";
                    suffix = "-macos-arm64.dylib";
                } else {
                    throw new UnsatisfiedLinkError("Unsupported OS name: " + osName + " / " + osArch);
                }
            }
            default -> throw new UnsatisfiedLinkError("Unsupported OS arch: " + osName + " / " + osArch);
        }

        this.name = prefix + name + suffix;
    }

    private Path createUnpackRoot() {
        final Path path = Path.of(System.getProperty("java.io.tmpdir")).resolve("jtracy-" + UUID.randomUUID());
        try {
            Files.createDirectory(path);
        } catch (IOException ignored) {
        }
        return path;
    }

    public void load() {
        final Path root = createUnpackRoot();
        try {
            final Path path = unpackLibrary(root);
            System.load(path.toAbsolutePath().toString());
        } finally {
            try {
                Files.walkFileTree(root, Set.of(), 1, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (final IOException ignored) {
            }

            try {
                Files.deleteIfExists(root);
            } catch (final IOException ignored) {
            }
        }
    }

    private Path unpackLibrary(final Path root) {
        try (final InputStream input = Loader.class.getClassLoader().getResourceAsStream(name)) {
            if (input == null) {
                throw new UnsatisfiedLinkError("Could not find jtracy natives at " + name);
            }
            final Path path = Files.createTempFile(root, name, null);
            Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING);
            return path;
        } catch (final IOException e) {
            throw new LinkageError("Can't unpack jtracy natives found at " + name + " to " + root, e);
        }
    }

}
