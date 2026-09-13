package net.minecraft.commands.arguments;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignableCommand;
import org.jspecify.annotations.Nullable;

public record ArgumentSignatures(List<ArgumentSignatures.Entry> entries) {
    public static final ArgumentSignatures EMPTY = new ArgumentSignatures(List.of());
    private static final int MAX_ARGUMENT_COUNT = 8;
    private static final int MAX_ARGUMENT_NAME_LENGTH = 16;

    public ArgumentSignatures(final FriendlyByteBuf input) {
        this(input.<Entry, List<Entry>>readCollection(FriendlyByteBuf.limitValue(ArrayList::new, 8), ArgumentSignatures.Entry::new));
    }

    public void write(final FriendlyByteBuf output) {
        output.writeCollection(this.entries, (out, entry) -> entry.write(out));
    }

    public static ArgumentSignatures signCommand(final SignableCommand<?> command, final ArgumentSignatures.Signer signer) {
        List<ArgumentSignatures.Entry> entries = command.arguments().stream().map(argument -> {
            MessageSignature signature = signer.sign(argument.value());
            return signature != null ? new ArgumentSignatures.Entry(argument.name(), signature) : null;
        }).filter(Objects::nonNull).toList();
        return new ArgumentSignatures(entries);
    }

    public record Entry(String name, MessageSignature signature) {
        public Entry(final FriendlyByteBuf input) {
            this(input.readUtf(16), MessageSignature.read(input));
        }

        public void write(final FriendlyByteBuf output) {
            output.writeUtf(this.name, 16);
            MessageSignature.write(output, this.signature);
        }
    }

    @FunctionalInterface
    public interface Signer {
        @Nullable MessageSignature sign(String content);
    }
}
