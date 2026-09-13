package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public class Vec3Argument implements ArgumentType<Coordinates> {
    private static final Collection<
            String> EXAMPLES = Arrays.asList("0 0 0", "~ ~ ~", "^ ^ ^", "^1 ^ ^-5", "0.1 -0.5 .9", "~0.5 ~1 ~-5");
    public static final SimpleCommandExceptionType ERROR_NOT_COMPLETE = new SimpleCommandExceptionType(Component.translatable("argument.pos3d.incomplete"));
    public static final SimpleCommandExceptionType ERROR_MIXED_TYPE = new SimpleCommandExceptionType(Component.translatable("argument.pos.mixed"));
    private final boolean centerCorrect;

    public Vec3Argument(final boolean centerCorrect) {
        this.centerCorrect = centerCorrect;
    }

    public static Vec3Argument vec3() {
        return new Vec3Argument(true);
    }

    public static Vec3Argument vec3(final boolean centerCorrect) {
        return new Vec3Argument(centerCorrect);
    }

    public static Vec3 getVec3(final CommandContext<
                    CommandSourceStack> context, final String name) {
        return context.getArgument(name, Coordinates.class).getPosition(context.getSource());
    }

    // 在 Coordinates 或 Vec3Argument 中
    private static double parseCoordinate(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "infinity", "+infinity" -> Double.POSITIVE_INFINITY;
            case "-infinity" -> Double.NEGATIVE_INFINITY;
            case "nan" -> Double.NaN;
            default -> Double.parseDouble(input);
        };
    }

    public static Coordinates getCoordinates(final CommandContext<
                    CommandSourceStack> context, final String name) {
        return context.getArgument(name, Coordinates.class);
    }

    public Coordinates parse(final StringReader reader) throws CommandSyntaxException {
        return reader.canRead() && reader.peek() == '^' ? LocalCoordinates.parse(reader) : WorldCoordinates.parseDouble(reader, this.centerCorrect);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<
                    S> context, final SuggestionsBuilder builder) {
        if (!(context.getSource() instanceof SharedSuggestionProvider)) {
            return Suggestions.empty();
        }

        String remainder = builder.getRemaining();
        Collection<SharedSuggestionProvider.TextCoordinates> suggestedCoordinates;
        if (!remainder.isEmpty() && remainder.charAt(0) == '^') {
            suggestedCoordinates = Collections.singleton(SharedSuggestionProvider.TextCoordinates.DEFAULT_LOCAL);
        } else {
            suggestedCoordinates = ((SharedSuggestionProvider) context.getSource()).getAbsoluteCoordinates();
        }

        return SharedSuggestionProvider.suggestCoordinates(remainder, suggestedCoordinates, builder, Commands.createValidator(this
                ::parse));
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}