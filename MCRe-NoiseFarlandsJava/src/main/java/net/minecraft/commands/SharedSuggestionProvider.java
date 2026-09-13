package net.minecraft.commands;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.permissions.PermissionSetSupplier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.Level;

public interface SharedSuggestionProvider extends PermissionSetSupplier {
    CharMatcher MATCH_SPLITTER = CharMatcher.anyOf("._/");

    Collection<String> getOnlinePlayerNames();

    default Collection<String> getCustomTabSuggestions() {
        return this.getOnlinePlayerNames();
    }

    default Collection<String> getSelectedEntities() {
        return Collections.emptyList();
    }

    Collection<String> getAllTeams();

    Stream<Identifier> getAvailableSounds();

    CompletableFuture<Suggestions> customSuggestion(CommandContext<?> context);

    default Collection<SharedSuggestionProvider.TextCoordinates> getRelevantCoordinates() {
        return Collections.singleton(SharedSuggestionProvider.TextCoordinates.DEFAULT_GLOBAL);
    }

    default Collection<SharedSuggestionProvider.TextCoordinates> getAbsoluteCoordinates() {
        return Collections.singleton(SharedSuggestionProvider.TextCoordinates.DEFAULT_GLOBAL);
    }

    Set<ResourceKey<Level>> levels();

    RegistryAccess registryAccess();

    FeatureFlagSet enabledFeatures();

    default void suggestRegistryElements(
        final HolderLookup<?> registry, final SharedSuggestionProvider.ElementSuggestionType elements, final SuggestionsBuilder builder
    ) {
        if (elements.shouldSuggestTags()) {
            suggestResource(registry.listTagIds().map(TagKey::location), builder, "#");
        }

        if (elements.shouldSuggestElements()) {
            suggestResource(registry.listElementIds().map(ResourceKey::identifier), builder);
        }
    }

    static <S> CompletableFuture<Suggestions> listSuggestions(
        final CommandContext<S> context,
        final SuggestionsBuilder builder,
        final ResourceKey<? extends Registry<?>> registryKey,
        final SharedSuggestionProvider.ElementSuggestionType type
    ) {
        return context.getSource() instanceof SharedSuggestionProvider suggestionProvider
            ? suggestionProvider.suggestRegistryElements(registryKey, type, builder, context)
            : builder.buildFuture();
    }

    CompletableFuture<Suggestions> suggestRegistryElements(
        final ResourceKey<? extends Registry<?>> key,
        final SharedSuggestionProvider.ElementSuggestionType elements,
        final SuggestionsBuilder builder,
        final CommandContext<?> context
    );

    static <T> void filterResources(final Iterable<T> values, final String contents, final Function<T, Identifier> converter, final Consumer<T> consumer) {
        boolean hasNamespace = contents.indexOf(58) > -1;

        for (T value : values) {
            Identifier id = converter.apply(value);
            if (hasNamespace) {
                String name = id.toString();
                if (matchesSubStr(contents, name)) {
                    consumer.accept(value);
                }
            } else if (matchesSubStr(contents, id.getNamespace()) || matchesSubStr(contents, id.getPath())) {
                consumer.accept(value);
            }
        }
    }

    static <T> void filterResources(
        final Iterable<T> values, final String contents, final String prefix, final Function<T, Identifier> converter, final Consumer<T> consumer
    ) {
        if (contents.isEmpty()) {
            values.forEach(consumer);
        } else {
            String commonPrefix = Strings.commonPrefix(contents, prefix);
            if (!commonPrefix.isEmpty()) {
                String strippedContents = contents.substring(commonPrefix.length());
                filterResources(values, strippedContents, converter, consumer);
            }
        }
    }

    static CompletableFuture<Suggestions> suggestResource(final Iterable<Identifier> values, final SuggestionsBuilder builder, final String prefix) {
        String contents = builder.getRemaining().toLowerCase(Locale.ROOT);
        filterResources(values, contents, prefix, t -> t, v -> builder.suggest(prefix + v));
        return builder.buildFuture();
    }

    static CompletableFuture<Suggestions> suggestResource(final Stream<Identifier> values, final SuggestionsBuilder builder, final String prefix) {
        return suggestResource(values::iterator, builder, prefix);
    }

    static CompletableFuture<Suggestions> suggestResource(final Iterable<Identifier> values, final SuggestionsBuilder builder) {
        String contents = builder.getRemaining().toLowerCase(Locale.ROOT);
        filterResources(values, contents, t -> t, v -> builder.suggest(v.toString()));
        return builder.buildFuture();
    }

    static <T> CompletableFuture<Suggestions> suggestResource(
        final Iterable<T> values, final SuggestionsBuilder builder, final Function<T, Identifier> id, final Function<T, Message> tooltip
    ) {
        String contents = builder.getRemaining().toLowerCase(Locale.ROOT);
        filterResources(values, contents, id, v -> builder.suggest(id.apply(v).toString(), tooltip.apply(v)));
        return builder.buildFuture();
    }

    static CompletableFuture<Suggestions> suggestResource(final Stream<Identifier> values, final SuggestionsBuilder builder) {
        return suggestResource(values::iterator, builder);
    }

    static <T> CompletableFuture<Suggestions> suggestResource(
        final Stream<T> values, final SuggestionsBuilder builder, final Function<T, Identifier> id, final Function<T, Message> tooltip
    ) {
        return suggestResource(values::iterator, builder, id, tooltip);
    }

    static CompletableFuture<Suggestions> suggestCoordinates(
        final String currentInput,
        final Collection<SharedSuggestionProvider.TextCoordinates> allSuggestions,
        final SuggestionsBuilder builder,
        final Predicate<String> validator
    ) {
        List<String> result = Lists.newArrayList();
        if (Strings.isNullOrEmpty(currentInput)) {
            for (SharedSuggestionProvider.TextCoordinates coordinate : allSuggestions) {
                String fullValue = coordinate.x + " " + coordinate.y + " " + coordinate.z;
                if (validator.test(fullValue)) {
                    result.add(coordinate.x);
                    result.add(coordinate.x + " " + coordinate.y);
                    result.add(fullValue);
                }
            }
        } else {
            String[] fields = currentInput.split(" ");
            if (fields.length == 1) {
                for (SharedSuggestionProvider.TextCoordinates coordinate : allSuggestions) {
                    String fullValue = fields[0] + " " + coordinate.y + " " + coordinate.z;
                    if (validator.test(fullValue)) {
                        result.add(fields[0] + " " + coordinate.y);
                        result.add(fullValue);
                    }
                }
            } else if (fields.length == 2) {
                for (SharedSuggestionProvider.TextCoordinates coordinate : allSuggestions) {
                    String fullValue = fields[0] + " " + fields[1] + " " + coordinate.z;
                    if (validator.test(fullValue)) {
                        result.add(fullValue);
                    }
                }
            }
        }

        return suggest(result, builder);
    }

    static CompletableFuture<Suggestions> suggest2DCoordinates(
        final String currentInput,
        final Collection<SharedSuggestionProvider.TextCoordinates> allSuggestions,
        final SuggestionsBuilder builder,
        final Predicate<String> validator
    ) {
        List<String> result = Lists.newArrayList();
        if (Strings.isNullOrEmpty(currentInput)) {
            for (SharedSuggestionProvider.TextCoordinates coordinate : allSuggestions) {
                String fullValue = coordinate.x + " " + coordinate.z;
                if (validator.test(fullValue)) {
                    result.add(coordinate.x);
                    result.add(fullValue);
                }
            }
        } else {
            String[] fields = currentInput.split(" ");
            if (fields.length == 1) {
                for (SharedSuggestionProvider.TextCoordinates coordinate : allSuggestions) {
                    String fullValue = fields[0] + " " + coordinate.z;
                    if (validator.test(fullValue)) {
                        result.add(fullValue);
                    }
                }
            }
        }

        return suggest(result, builder);
    }

    static CompletableFuture<Suggestions> suggest(final Iterable<String> values, final SuggestionsBuilder builder) {
        String lowerPrefix = builder.getRemaining().toLowerCase(Locale.ROOT);

        for (String name : values) {
            if (matchesSubStr(lowerPrefix, name.toLowerCase(Locale.ROOT))) {
                builder.suggest(name);
            }
        }

        return builder.buildFuture();
    }

    static CompletableFuture<Suggestions> suggest(final Stream<String> values, final SuggestionsBuilder builder) {
        String lowerPrefix = builder.getRemaining().toLowerCase(Locale.ROOT);
        values.filter(v -> matchesSubStr(lowerPrefix, v.toLowerCase(Locale.ROOT))).forEach(builder::suggest);
        return builder.buildFuture();
    }

    static CompletableFuture<Suggestions> suggest(final String[] values, final SuggestionsBuilder builder) {
        String lowerPrefix = builder.getRemaining().toLowerCase(Locale.ROOT);

        for (String name : values) {
            if (matchesSubStr(lowerPrefix, name.toLowerCase(Locale.ROOT))) {
                builder.suggest(name);
            }
        }

        return builder.buildFuture();
    }

    static <T> CompletableFuture<Suggestions> suggest(
        final Iterable<T> values, final SuggestionsBuilder builder, final Function<T, String> toString, final Function<T, Message> tooltip
    ) {
        String lowerPrefix = builder.getRemaining().toLowerCase(Locale.ROOT);

        for (T value : values) {
            String name = toString.apply(value);
            if (matchesSubStr(lowerPrefix, name.toLowerCase(Locale.ROOT))) {
                builder.suggest(name, tooltip.apply(value));
            }
        }

        return builder.buildFuture();
    }

    static boolean matchesSubStr(final String pattern, final String input) {
        int index = 0;

        while (!input.startsWith(pattern, index)) {
            int indexOfSplitter = MATCH_SPLITTER.indexIn(input, index);
            if (indexOfSplitter < 0) {
                return false;
            }

            index = indexOfSplitter + 1;
        }

        return true;
    }

    enum ElementSuggestionType {
        TAGS,
        ELEMENTS,
        ALL;

        public boolean shouldSuggestTags() {
            return this == TAGS || this == ALL;
        }

        public boolean shouldSuggestElements() {
            return this == ELEMENTS || this == ALL;
        }
    }

    class TextCoordinates {
        public static final SharedSuggestionProvider.TextCoordinates DEFAULT_LOCAL = new SharedSuggestionProvider.TextCoordinates("^", "^", "^");
        public static final SharedSuggestionProvider.TextCoordinates DEFAULT_GLOBAL = new SharedSuggestionProvider.TextCoordinates("~", "~", "~");
        public final String x;
        public final String y;
        public final String z;

        public TextCoordinates(final String x, final String y, final String z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}