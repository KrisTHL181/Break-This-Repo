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
   private final ResourceKey<? extends Registry<T>> registryKey;

   public ResourceOrTagKeyArgument(final ResourceKey<? extends Registry<T>> registryKey) {
      this.registryKey = registryKey;
   }

   public static <T> ResourceOrTagKeyArgument<T> resourceOrTagKey(final ResourceKey<? extends Registry<T>> key) {
      return new ResourceOrTagKeyArgument<>(key);
   }

   public static <T> ResourceOrTagKeyArgument.Result<T> getResourceOrTagKey(
      final CommandContext<CommandSourceStack> context,
      final String name,
      final ResourceKey<Registry<T>> registryKey,
      final DynamicCommandExceptionType exceptionType
   ) throws CommandSyntaxException {
      ResourceOrTagKeyArgument.Result<?> argument = (ResourceOrTagKeyArgument.Result<?>)context.getArgument(name, ResourceOrTagKeyArgument.Result.class);
      Optional<ResourceOrTagKeyArgument.Result<T>> value = argument.cast(registryKey);
      return value.orElseThrow(() -> exceptionType.create(argument));
   }

   public ResourceOrTagKeyArgument.Result<T> parse(final StringReader reader) throws CommandSyntaxException {
      if (reader.canRead() && reader.peek() == '#') {
         int cursor = reader.getCursor();

         try {
            reader.skip();
            Identifier tagId = Identifier.read(reader);
            return new ResourceOrTagKeyArgument.TagResult<>(TagKey.create(this.registryKey, tagId));
         } catch (CommandSyntaxException e) {
            reader.setCursor(cursor);
            throw e;
         }
      } else {
         Identifier resourceId = Identifier.read(reader);
         return new ResourceOrTagKeyArgument.ResourceResult<>(ResourceKey.create(this.registryKey, resourceId));
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
      return SharedSuggestionProvider.listSuggestions(context, builder, this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ALL);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static class Info<T> implements ArgumentTypeInfo<ResourceOrTagKeyArgument<T>, ResourceOrTagKeyArgument.Info<T>.Template> {
      public void serializeToNetwork(final ResourceOrTagKeyArgument.Info<T>.Template template, final FriendlyByteBuf out) {
         out.writeResourceKey(template.registryKey);
      }

      public ResourceOrTagKeyArgument.Info<T>.Template deserializeFromNetwork(final FriendlyByteBuf in) {
         return new ResourceOrTagKeyArgument.Info.Template(in.readRegistryKey());
      }

      public void serializeToJson(final ResourceOrTagKeyArgument.Info<T>.Template template, final JsonObject out) {
         out.addProperty("registry", template.registryKey.identifier().toString());
      }

      public ResourceOrTagKeyArgument.Info<T>.Template unpack(final ResourceOrTagKeyArgument<T> argument) {
         return new ResourceOrTagKeyArgument.Info.Template(argument.registryKey);
      }

      public final class Template implements ArgumentTypeInfo.Template<ResourceOrTagKeyArgument<T>> {
         private final ResourceKey<? extends Registry<T>> registryKey;

         private Template(final ResourceKey<? extends Registry<T>> registryKey) {
            this.registryKey = registryKey;
         }

         public ResourceOrTagKeyArgument<T> instantiate(final CommandBuildContext context) {
            return new ResourceOrTagKeyArgument<>(this.registryKey);
         }

         @Override
         public ArgumentTypeInfo<ResourceOrTagKeyArgument<T>, ?> type() {
            return Info.this;
         }
      }
   }

   private record ResourceResult<T>(ResourceKey<T> key) implements ResourceOrTagKeyArgument.Result<T> {
      @Override
      public Either<ResourceKey<T>, TagKey<T>> unwrap() {
         return Either.left(this.key);
      }

      @Override
      public <E> Optional<ResourceOrTagKeyArgument.Result<E>> cast(final ResourceKey<? extends Registry<E>> registryKey) {
         return this.key.cast(registryKey).map(ResourceOrTagKeyArgument.ResourceResult::new);
      }

      public boolean test(final Holder<T> holder) {
         return holder.is(this.key);
      }

      @Override
      public String asPrintable() {
         return this.key.identifier().toString();
      }
   }

   public interface Result<T> extends Predicate<Holder<T>> {
      Either<ResourceKey<T>, TagKey<T>> unwrap();

      <E> Optional<ResourceOrTagKeyArgument.Result<E>> cast(final ResourceKey<? extends Registry<E>> registryKey);

      String asPrintable();
   }

   private record TagResult<T>(TagKey<T> key) implements ResourceOrTagKeyArgument.Result<T> {
      @Override
      public Either<ResourceKey<T>, TagKey<T>> unwrap() {
         return Either.right(this.key);
      }

      @Override
      public <E> Optional<ResourceOrTagKeyArgument.Result<E>> cast(final ResourceKey<? extends Registry<E>> registryKey) {
         return this.key.cast(registryKey).map(ResourceOrTagKeyArgument.TagResult::new);
      }

      public boolean test(final Holder<T> holder) {
         return holder.is(this.key);
      }

      @Override
      public String asPrintable() {
         return "#" + this.key.location();
      }
   }
}
