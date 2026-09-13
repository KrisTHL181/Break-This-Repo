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
   public static final SuggestionProvider<SharedSuggestionProvider> ASK_SERVER = register(
      ID_ASK_SERVER, (p_121673_, p_121674_) -> ((SharedSuggestionProvider)p_121673_.getSource()).customSuggestion(p_121673_)
   );
   public static final SuggestionProvider<SharedSuggestionProvider> AVAILABLE_SOUNDS = register(
      Identifier.withDefaultNamespace("available_sounds"),
      (p_121667_, p_121668_) -> SharedSuggestionProvider.suggestResource(((SharedSuggestionProvider)p_121667_.getSource()).getAvailableSounds(), p_121668_)
   );
   public static final SuggestionProvider<SharedSuggestionProvider> SUMMONABLE_ENTITIES = register(
      Identifier.withDefaultNamespace("summonable_entities"),
      (p_358078_, p_358079_) -> SharedSuggestionProvider.suggestResource(
         BuiltInRegistries.ENTITY_TYPE
            .stream()
            .filter(p_247987_ -> p_247987_.isEnabled(((SharedSuggestionProvider)p_358078_.getSource()).enabledFeatures()) && p_247987_.canSummon()),
         p_358079_,
         EntityType::getKey,
         EntityType::getDescription
      )
   );

   public static <S extends SharedSuggestionProvider> SuggestionProvider<S> register(
      Identifier p_458562_, SuggestionProvider<SharedSuggestionProvider> p_121660_
   ) {
      SuggestionProvider<SharedSuggestionProvider> suggestionprovider = PROVIDERS_BY_NAME.putIfAbsent(p_458562_, p_121660_);
      if (suggestionprovider != null) {
         throw new IllegalArgumentException("A command suggestion provider is already registered with the name '" + p_458562_ + "'");
      } else {
         return new SuggestionProviders.RegisteredSuggestion(p_458562_, p_121660_);
      }
   }

   public static <S extends SharedSuggestionProvider> SuggestionProvider<S> cast(SuggestionProvider<SharedSuggestionProvider> p_409850_) {
      return (SuggestionProvider<S>)p_409850_;
   }

   public static <S extends SharedSuggestionProvider> SuggestionProvider<S> getProvider(Identifier p_453515_) {
      return cast(PROVIDERS_BY_NAME.getOrDefault(p_453515_, ASK_SERVER));
   }

   public static Identifier getName(SuggestionProvider<?> p_121655_) {
      return p_121655_ instanceof SuggestionProviders.RegisteredSuggestion suggestionproviders$registeredsuggestion
         ? suggestionproviders$registeredsuggestion.name
         : ID_ASK_SERVER;
   }

   record RegisteredSuggestion(Identifier name, SuggestionProvider<SharedSuggestionProvider> delegate) implements SuggestionProvider<SharedSuggestionProvider> {
      public CompletableFuture<Suggestions> getSuggestions(CommandContext<SharedSuggestionProvider> p_406294_, SuggestionsBuilder p_407076_) throws CommandSyntaxException {
         return this.delegate.getSuggestions(p_406294_, p_407076_);
      }
   }
}
