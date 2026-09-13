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

   default Collection<String> getCustomTabSugggestions() {
      return this.getOnlinePlayerNames();
   }

   default Collection<String> getSelectedEntities() {
      return Collections.emptyList();
   }

   Collection<String> getAllTeams();

   Stream<Identifier> getAvailableSounds();

   CompletableFuture<Suggestions> customSuggestion(CommandContext<?> var1);

   default Collection<SharedSuggestionProvider.TextCoordinates> getRelevantCoordinates() {
      return Collections.singleton(SharedSuggestionProvider.TextCoordinates.DEFAULT_GLOBAL);
   }

   default Collection<SharedSuggestionProvider.TextCoordinates> getAbsoluteCoordinates() {
      return Collections.singleton(SharedSuggestionProvider.TextCoordinates.DEFAULT_GLOBAL);
   }

   Set<ResourceKey<Level>> levels();

   RegistryAccess registryAccess();

   FeatureFlagSet enabledFeatures();

   default void suggestRegistryElements(HolderLookup<?> p_408634_, SharedSuggestionProvider.ElementSuggestionType p_212337_, SuggestionsBuilder p_212338_) {
      if (p_212337_.shouldSuggestTags()) {
         suggestResource(p_408634_.listTagIds().map(TagKey::location), p_212338_, "#");
      }

      if (p_212337_.shouldSuggestElements()) {
         suggestResource(p_408634_.listElementIds().map(ResourceKey::identifier), p_212338_);
      }
   }

   static <S> CompletableFuture<Suggestions> listSuggestions(
      CommandContext<S> p_410537_,
      SuggestionsBuilder p_406393_,
      ResourceKey<? extends Registry<?>> p_405883_,
      SharedSuggestionProvider.ElementSuggestionType p_409995_
   ) {
      return p_410537_.getSource() instanceof SharedSuggestionProvider sharedsuggestionprovider
         ? sharedsuggestionprovider.suggestRegistryElements(p_405883_, p_409995_, p_406393_, p_410537_)
         : p_406393_.buildFuture();
   }

   CompletableFuture<Suggestions> suggestRegistryElements(
      ResourceKey<? extends Registry<?>> var1, SharedSuggestionProvider.ElementSuggestionType var2, SuggestionsBuilder var3, CommandContext<?> var4
   );

   static <T> void filterResources(Iterable<T> p_82945_, String p_82946_, Function<T, Identifier> p_82947_, Consumer<T> p_82948_) {
      boolean flag = p_82946_.indexOf(58) > -1;

      for (T t : p_82945_) {
         Identifier identifier = p_82947_.apply(t);
         if (flag) {
            String s = identifier.toString();
            if (matchesSubStr(p_82946_, s)) {
               p_82948_.accept(t);
            }
         } else if (matchesSubStr(p_82946_, identifier.getNamespace()) || matchesSubStr(p_82946_, identifier.getPath())) {
            p_82948_.accept(t);
         }
      }
   }

   static <T> void filterResources(Iterable<T> p_82939_, String p_82940_, String p_82941_, Function<T, Identifier> p_82942_, Consumer<T> p_82943_) {
      if (p_82940_.isEmpty()) {
         p_82939_.forEach(p_82943_);
      } else {
         String s = Strings.commonPrefix(p_82940_, p_82941_);
         if (!s.isEmpty()) {
            String s1 = p_82940_.substring(s.length());
            filterResources(p_82939_, s1, p_82942_, p_82943_);
         }
      }
   }

   static CompletableFuture<Suggestions> suggestResource(Iterable<Identifier> p_82930_, SuggestionsBuilder p_82931_, String p_82932_) {
      String s = p_82931_.getRemaining().toLowerCase(Locale.ROOT);
      filterResources(p_82930_, s, p_82932_, p_454542_ -> p_454542_, p_460626_ -> p_82931_.suggest(p_82932_ + p_460626_));
      return p_82931_.buildFuture();
   }

   static CompletableFuture<Suggestions> suggestResource(Stream<Identifier> p_205107_, SuggestionsBuilder p_205108_, String p_205109_) {
      return suggestResource(p_205107_::iterator, p_205108_, p_205109_);
   }

   static CompletableFuture<Suggestions> suggestResource(Iterable<Identifier> p_82927_, SuggestionsBuilder p_82928_) {
      String s = p_82928_.getRemaining().toLowerCase(Locale.ROOT);
      filterResources(p_82927_, s, p_460955_ -> p_460955_, p_448475_ -> p_82928_.suggest(p_448475_.toString()));
      return p_82928_.buildFuture();
   }

   static <T> CompletableFuture<Suggestions> suggestResource(
      Iterable<T> p_82934_, SuggestionsBuilder p_82935_, Function<T, Identifier> p_82936_, Function<T, Message> p_82937_
   ) {
      String s = p_82935_.getRemaining().toLowerCase(Locale.ROOT);
      filterResources(p_82934_, s, p_82936_, p_448479_ -> p_82935_.suggest(p_82936_.apply(p_448479_).toString(), p_82937_.apply(p_448479_)));
      return p_82935_.buildFuture();
   }

   static CompletableFuture<Suggestions> suggestResource(Stream<Identifier> p_82958_, SuggestionsBuilder p_82959_) {
      return suggestResource(p_82958_::iterator, p_82959_);
   }

   static <T> CompletableFuture<Suggestions> suggestResource(
      Stream<T> p_82961_, SuggestionsBuilder p_82962_, Function<T, Identifier> p_82963_, Function<T, Message> p_82964_
   ) {
      return suggestResource(p_82961_::iterator, p_82962_, p_82963_, p_82964_);
   }

   static CompletableFuture<Suggestions> suggestCoordinates(
      String p_82953_, Collection<SharedSuggestionProvider.TextCoordinates> p_82954_, SuggestionsBuilder p_82955_, Predicate<String> p_82956_
   ) {
      List<String> list = Lists.newArrayList();
      if (Strings.isNullOrEmpty(p_82953_)) {
         for (SharedSuggestionProvider.TextCoordinates sharedsuggestionprovider$textcoordinates : p_82954_) {
            String s = sharedsuggestionprovider$textcoordinates.x
               + " "
               + sharedsuggestionprovider$textcoordinates.y
               + " "
               + sharedsuggestionprovider$textcoordinates.z;
            if (p_82956_.test(s)) {
               list.add(sharedsuggestionprovider$textcoordinates.x);
               list.add(sharedsuggestionprovider$textcoordinates.x + " " + sharedsuggestionprovider$textcoordinates.y);
               list.add(s);
            }
         }
      } else {
         String[] astring = p_82953_.split(" ");
         if (astring.length == 1) {
            for (SharedSuggestionProvider.TextCoordinates sharedsuggestionprovider$textcoordinates1 : p_82954_) {
               String s1 = astring[0] + " " + sharedsuggestionprovider$textcoordinates1.y + " " + sharedsuggestionprovider$textcoordinates1.z;
               if (p_82956_.test(s1)) {
                  list.add(astring[0] + " " + sharedsuggestionprovider$textcoordinates1.y);
                  list.add(s1);
               }
            }
         } else if (astring.length == 2) {
            for (SharedSuggestionProvider.TextCoordinates sharedsuggestionprovider$textcoordinates2 : p_82954_) {
               String s2 = astring[0] + " " + astring[1] + " " + sharedsuggestionprovider$textcoordinates2.z;
               if (p_82956_.test(s2)) {
                  list.add(s2);
               }
            }
         }
      }

      return suggest(list, p_82955_);
   }

   static CompletableFuture<Suggestions> suggest2DCoordinates(
      String p_82977_, Collection<SharedSuggestionProvider.TextCoordinates> p_82978_, SuggestionsBuilder p_82979_, Predicate<String> p_82980_
   ) {
      List<String> list = Lists.newArrayList();
      if (Strings.isNullOrEmpty(p_82977_)) {
         for (SharedSuggestionProvider.TextCoordinates sharedsuggestionprovider$textcoordinates : p_82978_) {
            String s = sharedsuggestionprovider$textcoordinates.x + " " + sharedsuggestionprovider$textcoordinates.z;
            if (p_82980_.test(s)) {
               list.add(sharedsuggestionprovider$textcoordinates.x);
               list.add(s);
            }
         }
      } else {
         String[] astring = p_82977_.split(" ");
         if (astring.length == 1) {
            for (SharedSuggestionProvider.TextCoordinates sharedsuggestionprovider$textcoordinates1 : p_82978_) {
               String s1 = astring[0] + " " + sharedsuggestionprovider$textcoordinates1.z;
               if (p_82980_.test(s1)) {
                  list.add(s1);
               }
            }
         }
      }

      return suggest(list, p_82979_);
   }

   static CompletableFuture<Suggestions> suggest(Iterable<String> p_82971_, SuggestionsBuilder p_82972_) {
      String s = p_82972_.getRemaining().toLowerCase(Locale.ROOT);

      for (String s1 : p_82971_) {
         if (matchesSubStr(s, s1.toLowerCase(Locale.ROOT))) {
            p_82972_.suggest(s1);
         }
      }

      return p_82972_.buildFuture();
   }

   static CompletableFuture<Suggestions> suggest(Stream<String> p_82982_, SuggestionsBuilder p_82983_) {
      String s = p_82983_.getRemaining().toLowerCase(Locale.ROOT);
      p_82982_.filter(p_82975_ -> matchesSubStr(s, p_82975_.toLowerCase(Locale.ROOT))).forEach(p_82983_::suggest);
      return p_82983_.buildFuture();
   }

   static CompletableFuture<Suggestions> suggest(String[] p_82968_, SuggestionsBuilder p_82969_) {
      String s = p_82969_.getRemaining().toLowerCase(Locale.ROOT);

      for (String s1 : p_82968_) {
         if (matchesSubStr(s, s1.toLowerCase(Locale.ROOT))) {
            p_82969_.suggest(s1);
         }
      }

      return p_82969_.buildFuture();
   }

   static <T> CompletableFuture<Suggestions> suggest(
      Iterable<T> p_165917_, SuggestionsBuilder p_165918_, Function<T, String> p_165919_, Function<T, Message> p_165920_
   ) {
      String s = p_165918_.getRemaining().toLowerCase(Locale.ROOT);

      for (T t : p_165917_) {
         String s1 = p_165919_.apply(t);
         if (matchesSubStr(s, s1.toLowerCase(Locale.ROOT))) {
            p_165918_.suggest(s1, p_165920_.apply(t));
         }
      }

      return p_165918_.buildFuture();
   }

   static boolean matchesSubStr(String p_82950_, String p_82951_) {
      int i = 0;

      while (!p_82951_.startsWith(p_82950_, i)) {
         int j = MATCH_SPLITTER.indexIn(p_82951_, i);
         if (j < 0) {
            return false;
         }

         i = j + 1;
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

      public TextCoordinates(String p_82994_, String p_82995_, String p_82996_) {
         this.x = p_82994_;
         this.y = p_82995_;
         this.z = p_82996_;
      }
   }
}
