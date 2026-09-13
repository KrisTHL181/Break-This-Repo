package net.minecraft.server.commands.data;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.CommandStorage;

public class StorageDataAccessor implements DataAccessor {
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_STORAGE = (c, p) -> SharedSuggestionProvider.suggestResource(getGlobalTags(c).keys(), p);
    public static final Function<String, DataCommands.DataProvider> PROVIDER = arg -> new DataCommands.DataProvider() {
        @Override
        public DataAccessor access(final CommandContext<CommandSourceStack> context) {
            return new StorageDataAccessor(StorageDataAccessor.getGlobalTags(context), IdentifierArgument.getId(context, arg));
        }

        @Override
        public ArgumentBuilder<CommandSourceStack, ?> wrap(
            final ArgumentBuilder<CommandSourceStack, ?> parent,
            final Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> function
        ) {
            return parent.then(
                Commands.literal("storage").then(function.apply(Commands.argument(arg, IdentifierArgument.id()).suggests(StorageDataAccessor.SUGGEST_STORAGE)))
            );
        }
    };
    private final CommandStorage storage;
    private final Identifier id;

    private static CommandStorage getGlobalTags(final CommandContext<CommandSourceStack> context) {
        return context.getSource().getServer().getCommandStorage();
    }

    private StorageDataAccessor(final CommandStorage storage, final Identifier id) {
        this.storage = storage;
        this.id = id;
    }

    @Override
    public void setData(final CompoundTag tag) {
        this.storage.set(this.id, tag);
    }

    @Override
    public CompoundTag getData() {
        return this.storage.get(this.id);
    }

    @Override
    public Component getModifiedSuccess() {
        return Component.translatable("commands.data.storage.modified", Component.translationArg(this.id));
    }

    @Override
    public Component getPrintSuccess(final Tag data) {
        return Component.translatable("commands.data.storage.query", Component.translationArg(this.id), NbtUtils.toPrettyComponent(data));
    }

    @Override
    public Component getPrintSuccess(final NbtPathArgument.NbtPath path, final double scale, final int value) {
        return Component.translatable(
            "commands.data.storage.get", path.asString(), Component.translationArg(this.id), String.format(Locale.ROOT, "%.2f", scale), value
        );
    }
}