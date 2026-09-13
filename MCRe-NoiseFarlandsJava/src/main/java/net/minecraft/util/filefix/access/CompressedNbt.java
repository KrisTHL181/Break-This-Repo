package net.minecraft.util.filefix.access;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.FileUtil;
import org.slf4j.Logger;

public abstract class CompressedNbt implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Path path;
    private final CompressedNbt.MissingSeverity missingSeverity;

    public CompressedNbt(final Path path, final CompressedNbt.MissingSeverity missingSeverity) {
        this.path = path;
        this.missingSeverity = missingSeverity;
    }

    public abstract Optional<Dynamic<Tag>> read() throws IOException;

    protected final Optional<Dynamic<Tag>> readFile() throws IOException {
        try {
            return Optional.of(new Dynamic<>(NbtOps.INSTANCE, NbtIo.readCompressed(this.path, NbtAccounter.unlimitedHeap())));
        } catch (NoSuchFileException ignored) {
            this.missingSeverity.log("Missing file: {}", this.path);
            return Optional.empty();
        }
    }

    public abstract <T> void write(final Dynamic<T> data);

    protected final <T> void writeFile(final Dynamic<T> data) {
        CompoundTag cast = (CompoundTag)data.cast(NbtOps.INSTANCE);

        try {
            FileUtil.createDirectoriesSafe(this.path.getParent());
            NbtIo.writeCompressed(cast, this.path);
        } catch (IOException e) {
            LOGGER.error("Failed to write to {}: {}", this.path, e);
        }
    }

    public Path path() {
        return this.path;
    }

    @Override
    public void close() {
    }

    public enum MissingSeverity {
        IMPORTANT(CompressedNbt.LOGGER::error),
        NEUTRAL(CompressedNbt.LOGGER::info),
        MINOR(CompressedNbt.LOGGER::debug);

        private final BiConsumer<String, Object> logFunction;

        MissingSeverity(final BiConsumer<String, Object> logFunction) {
            this.logFunction = logFunction;
        }

        public void log(final String message, final Path path) {
            this.logFunction.accept(message, path);
        }
    }
}