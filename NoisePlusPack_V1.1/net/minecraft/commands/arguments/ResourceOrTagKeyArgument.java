package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public class ResourceOrTagKeyArgument<T> implements ArgumentType<ResourceOrTagKeyArgument.Result<T>> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012", "#skeletons", "#minecraft:skeletons");
   final ResourceKey<? extends Registry<T>> registryKey;

   public ResourceOrTagKeyArgument(ResourceKey<? extends Registry<T>> p_248579_) {
      this.registryKey = p_248579_;
   }

   public static <T> ResourceOrTagKeyArgument<T> resourceOrTagKey(ResourceKey<? extends Registry<T>> p_249175_) {
      return new ResourceOrTagKeyArgument<>(p_249175_);
   }

   public static <T> ResourceOrTagKeyArgument.Result<T> getResourceOrTagKey(
      CommandContext<CommandSourceStack> p_252162_, String p_248628_, ResourceKey<Registry<T>> p_249008_, DynamicCommandExceptionType p_251387_
   ) throws CommandSyntaxException {
      ResourceOrTagKeyArgument.Result<?> result = (ResourceOrTagKeyArgument.Result<?>)p_252162_.getArgument(p_248628_, ResourceOrTagKeyArgument.Result.class);
      Optional<ResourceOrTagKeyArgument.Result<T>> optional = result.cast(p_249008_);
      return optional.orElseThrow(() -> p_251387_.create(result));
   }

   public ResourceOrTagKeyArgument.Result<T> parse(StringReader p_250307_) throws CommandSyntaxException {
      if (p_250307_.canRead() && p_250307_.peek() == '#') {
         int i = p_250307_.getCursor();

         try {
            p_250307_.skip();
            Identifier identifier1 = Identifier.read(p_250307_);
            return new ResourceOrTagKeyArgument.TagResult<>(TagKey.create(this.registryKey, identifier1));
         } catch (CommandSyntaxException commandsyntaxexception) {
            p_250307_.setCursor(i);
            throw commandsyntaxexception;
         }
      } else {
         Identifier identifier = Identifier.read(p_250307_);
         return new ResourceOrTagKeyArgument.ResourceResult<>(ResourceKey.create(this.registryKey, identifier));
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_251659_, SuggestionsBuilder p_251141_) {
      return SharedSuggestionProvider.listSuggestions(p_251659_, p_251141_, this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ALL);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static class Info<T> implements ArgumentTypeInfo<ResourceOrTagKeyArgument<T>, ResourceOrTagKeyArgument.Info<T>.Template> {
      public void serializeToNetwork(ResourceOrTagKeyArgument.Info<T>.Template p_252211_, FriendlyByteBuf p_248784_) {
         p_248784_.writeResourceKey(p_252211_.registryKey);
      }

      public ResourceOrTagKeyArgument.Info<T>.Template deserializeFromNetwork(FriendlyByteBuf p_250656_) {
         return new ResourceOrTagKeyArgument.Info.Template(p_250656_.readRegistryKey());
      }

      public void serializeToJson(ResourceOrTagKeyArgument.Info<T>.Template p_250715_, JsonObject p_249208_) {
         p_249208_.addProperty("registry", p_250715_.registryKey.identifier().toString());
      }

      public ResourceOrTagKeyArgument.Info<T>.Template unpack(ResourceOrTagKeyArgument<T> p_250422_) {
         return new ResourceOrTagKeyArgument.Info.Template(p_250422_.registryKey);
      }

      public final class Template implements ArgumentTypeInfo.Template<ResourceOrTagKeyArgument<T>> {
         final ResourceKey<? extends Registry<T>> registryKey;

         Template(final ResourceKey<? extends Registry<T>> p_251992_) {
            this.registryKey = p_251992_;
         }

         public ResourceOrTagKeyArgument<T> instantiate(CommandBuildContext p_251559_) {
            return new ResourceOrTagKeyArgument<>(this.registryKey);
         }

         @Override
         public ArgumentTypeInfo<ResourceOrTagKeyArgument<T>, ?> type() {
            return Info.this;
         }
      }
   }

   record ResourceResult<T>(ResourceKey<T> key) implements ResourceOrTagKeyArgument.Result<T> {
      @Override
      public Either<ResourceKey<T>, TagKey<T>> unwrap() {
         return Either.left(this.key);
      }

      @Override
      public <E> Optional<ResourceOrTagKeyArgument.Result<E>> cast(ResourceKey<? extends Registry<E>> p_251369_) {
         return this.key.cast(p_251369_).map(ResourceOrTagKeyArgument.ResourceResult::new);
      }

      public boolean test(Holder<T> p_250257_) {
         return p_250257_.is(this.key);
      }

      @Override
      public String asPrintable() {
         return this.key.identifier().toString();
      }
   }

   public interface Result<T> extends Predicate<Holder<T>> {
      Either<ResourceKey<T>, TagKey<T>> unwrap();

      <E> Optional<ResourceOrTagKeyArgument.Result<E>> cast(ResourceKey<? extends Registry<E>> var1);

      String asPrintable();
   }

   record TagResult<T>(TagKey<T> key) implements ResourceOrTagKeyArgument.Result<T> {
      @Override
      public Either<ResourceKey<T>, TagKey<T>> unwrap() {
         return Either.right(this.key);
      }

      @Override
      public <E> Optional<ResourceOrTagKeyArgument.Result<E>> cast(ResourceKey<? extends Registry<E>> p_251833_) {
         return this.key.cast(p_251833_).map(ResourceOrTagKeyArgument.TagResult::new);
      }

      public boolean test(Holder<T> p_252238_) {
         return p_252238_.is(this.key);
      }

      @Override
      public String asPrintable() {
         return "#" + this.key.location();
      }
   }
}
