package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.ParserUtils;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.SlotRange;
import net.minecraft.world.inventory.SlotRanges;

public class SlotsArgument implements ArgumentType<SlotRange> {
    private static final Collection<String> EXAMPLES = List.of("container.*", "container.5", "weapon");
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_SLOT = new DynamicCommandExceptionType(
        id -> Component.translatableEscape("slot.unknown", id)
    );

    public static SlotsArgument slots() {
        return new SlotsArgument();
    }

    public static SlotRange getSlots(final CommandContext<CommandSourceStack> context, final String name) {
        return context.getArgument(name, SlotRange.class);
    }

    public SlotRange parse(final StringReader reader) throws CommandSyntaxException {
        String name = ParserUtils.readWhile(reader, c -> c != ' ');
        SlotRange result = SlotRanges.nameToIds(name);
        if (result == null) {
            throw ERROR_UNKNOWN_SLOT.createWithContext(reader, name);
        } else {
            return result;
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> contextBuilder, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(SlotRanges.allNames(), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}