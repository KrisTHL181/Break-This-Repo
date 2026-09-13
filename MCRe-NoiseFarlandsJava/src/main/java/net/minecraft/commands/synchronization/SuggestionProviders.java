package net.minecraft.commands.synchronization;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

public class SuggestionProviders {
    private static final Map<Identifier, SuggestionProvider<SharedSuggestionProvider>> PROVIDERS_BY_NAME = new HashMap<>();
    private static final Identifier ID_ASK_SERVER = Identifier.withDefaultNamespace("ask_server");
    public static final SuggestionProvider<SharedSuggestionProvider> ASK_SERVER = register(ID_ASK_SERVER, (c, p) -> c.getSource().customSuggestion(c));
    public static final SuggestionProvider<SharedSuggestionProvider> AVAILABLE_SOUNDS = register(
        Identifier.withDefaultNamespace("available_sounds"), (c, p) -> SharedSuggestionProvider.suggestResource(c.getSource().getAvailableSounds(), p)
    );
    public static final SuggestionProvider<SharedSuggestionProvider> SUMMONABLE_ENTITIES = register(
        Identifier.withDefaultNamespace("summonable_entities"),
        (c, p) -> SharedSuggestionProvider.suggestResource(
            BuiltInRegistries.ENTITY_TYPE.stream().filter(entityType -> entityType.isEnabled(c.getSource().enabledFeatures()) && entityType.canSummon()),
            p,
            EntityType::getKey,
            EntityType::getDescription
        )
    );

    public static <S extends SharedSuggestionProvider> SuggestionProvider<S> register(
        final Identifier name, final SuggestionProvider<SharedSuggestionProvider> provider
    ) {
        SuggestionProvider<SharedSuggestionProvider> previous = PROVIDERS_BY_NAME.putIfAbsent(name, provider);
        if (previous != null) {
            throw new IllegalArgumentException("A command suggestion provider is already registered with the name '" + name + "'");
        } else {
            return (SuggestionProvider<S>)new SuggestionProviders.RegisteredSuggestion(name, provider);
        }
    }

    public static <S extends SharedSuggestionProvider> SuggestionProvider<S> cast(final SuggestionProvider<SharedSuggestionProvider> provider) {
        return (SuggestionProvider<S>)provider;
    }

    public static <S extends SharedSuggestionProvider> SuggestionProvider<S> getProvider(final Identifier name) {
        return cast(PROVIDERS_BY_NAME.getOrDefault(name, ASK_SERVER));
    }

    public static Identifier getName(final SuggestionProvider<?> provider) {
        return provider instanceof SuggestionProviders.RegisteredSuggestion registeredProvider ? registeredProvider.name : ID_ASK_SERVER;
    }

    private record RegisteredSuggestion(Identifier name, SuggestionProvider<SharedSuggestionProvider> delegate)
        implements SuggestionProvider<SharedSuggestionProvider> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(final CommandContext<SharedSuggestionProvider> context, final SuggestionsBuilder builder) throws CommandSyntaxException {
            return this.delegate.getSuggestions(context, builder);
        }
    }
}
