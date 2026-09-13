package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.apache.commons.io.FilenameUtils;

public class ResourceSelectorArgument<T> implements ArgumentType<Collection<Holder.Reference<T>>> {
   private static final Collection<String> EXAMPLES = List.of("minecraft:*", "*:asset", "*");
   public static final Dynamic2CommandExceptionType ERROR_NO_MATCHES = new Dynamic2CommandExceptionType(
      (p_396458_, p_395304_) -> Component.translatableEscape("argument.resource_selector.not_found", p_396458_, p_395304_)
   );
   final ResourceKey<? extends Registry<T>> registryKey;
   private final HolderLookup<T> registryLookup;

   ResourceSelectorArgument(CommandBuildContext p_394732_, ResourceKey<? extends Registry<T>> p_397573_) {
      this.registryKey = p_397573_;
      this.registryLookup = p_394732_.lookupOrThrow(p_397573_);
   }

   public Collection<Holder.Reference<T>> parse(StringReader p_393369_) throws CommandSyntaxException {
      String s = ensureNamespaced(readPattern(p_393369_));
      List<Holder.Reference<T>> list = this.registryLookup.listElements().filter(p_448504_ -> matches(s, p_448504_.key().identifier())).toList();
      if (list.isEmpty()) {
         throw ERROR_NO_MATCHES.createWithContext(p_393369_, s, this.registryKey.identifier());
      } else {
         return list;
      }
   }

   public static <T> Collection<Holder.Reference<T>> parse(StringReader p_393805_, HolderLookup<T> p_396234_) {
      String s = ensureNamespaced(readPattern(p_393805_));
      return p_396234_.listElements().filter(p_448502_ -> matches(s, p_448502_.key().identifier())).toList();
   }

   private static String readPattern(StringReader p_391311_) {
      int i = p_391311_.getCursor();

      while (p_391311_.canRead() && isAllowedPatternCharacter(p_391311_.peek())) {
         p_391311_.skip();
      }

      return p_391311_.getString().substring(i, p_391311_.getCursor());
   }

   private static boolean isAllowedPatternCharacter(char p_392586_) {
      return Identifier.isAllowedInIdentifier(p_392586_) || p_392586_ == '*' || p_392586_ == '?';
   }

   private static String ensureNamespaced(String p_396733_) {
      return !p_396733_.contains(":") ? "minecraft:" + p_396733_ : p_396733_;
   }

   private static boolean matches(String p_396263_, Identifier p_459609_) {
      return FilenameUtils.wildcardMatch(p_459609_.toString(), p_396263_);
   }

   public static <T> ResourceSelectorArgument<T> resourceSelector(CommandBuildContext p_397963_, ResourceKey<? extends Registry<T>> p_393390_) {
      return new ResourceSelectorArgument<>(p_397963_, p_393390_);
   }

   public static <T> Collection<Holder.Reference<T>> getSelectedResources(CommandContext<CommandSourceStack> p_394081_, String p_393093_) {
      return (Collection<Holder.Reference<T>>)p_394081_.getArgument(p_393093_, Collection.class);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_396736_, SuggestionsBuilder p_395731_) {
      return SharedSuggestionProvider.listSuggestions(p_396736_, p_395731_, this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ELEMENTS);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static class Info<T> implements ArgumentTypeInfo<ResourceSelectorArgument<T>, ResourceSelectorArgument.Info<T>.Template> {
      public void serializeToNetwork(ResourceSelectorArgument.Info<T>.Template p_395331_, FriendlyByteBuf p_392665_) {
         p_392665_.writeResourceKey(p_395331_.registryKey);
      }

      public ResourceSelectorArgument.Info<T>.Template deserializeFromNetwork(FriendlyByteBuf p_395716_) {
         return new ResourceSelectorArgument.Info.Template(p_395716_.readRegistryKey());
      }

      public void serializeToJson(ResourceSelectorArgument.Info<T>.Template p_397745_, JsonObject p_391870_) {
         p_391870_.addProperty("registry", p_397745_.registryKey.identifier().toString());
      }

      public ResourceSelectorArgument.Info<T>.Template unpack(ResourceSelectorArgument<T> p_391303_) {
         return new ResourceSelectorArgument.Info.Template(p_391303_.registryKey);
      }

      public final class Template implements ArgumentTypeInfo.Template<ResourceSelectorArgument<T>> {
         final ResourceKey<? extends Registry<T>> registryKey;

         Template(final ResourceKey<? extends Registry<T>> p_393919_) {
            this.registryKey = p_393919_;
         }

         public ResourceSelectorArgument<T> instantiate(CommandBuildContext p_397803_) {
            return new ResourceSelectorArgument<>(p_397803_, this.registryKey);
         }

         @Override
         public ArgumentTypeInfo<ResourceSelectorArgument<T>, ?> type() {
            return Info.this;
         }
      }
   }
}
