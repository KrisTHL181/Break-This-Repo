package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
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
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public class ResourceOrTagArgument<T> implements ArgumentType<ResourceOrTagArgument.Result<T>> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012", "#skeletons", "#minecraft:skeletons");
   private static final Dynamic2CommandExceptionType ERROR_UNKNOWN_TAG = new Dynamic2CommandExceptionType(
      (id, registry) -> Component.translatableEscape("argument.resource_tag.not_found", id, registry)
   );
   private static final Dynamic3CommandExceptionType ERROR_INVALID_TAG_TYPE = new Dynamic3CommandExceptionType(
      (id, actualRegistry, expectedRegistry) -> Component.translatableEscape("argument.resource_tag.invalid_type", id, actualRegistry, expectedRegistry)
   );
   private final HolderLookup<T> registryLookup;
   private final ResourceKey<? extends Registry<T>> registryKey;

   public ResourceOrTagArgument(final CommandBuildContext context, final ResourceKey<? extends Registry<T>> registryKey) {
      this.registryKey = registryKey;
      this.registryLookup = context.lookupOrThrow(registryKey);
   }

   public static <T> ResourceOrTagArgument<T> resourceOrTag(final CommandBuildContext context, final ResourceKey<? extends Registry<T>> key) {
      return new ResourceOrTagArgument<>(context, key);
   }

   public static <T> ResourceOrTagArgument.Result<T> getResourceOrTag(
      final CommandContext<CommandSourceStack> context, final String name, final ResourceKey<Registry<T>> registryKey
   ) throws CommandSyntaxException {
      ResourceOrTagArgument.Result<?> argument = (ResourceOrTagArgument.Result<?>)context.getArgument(name, ResourceOrTagArgument.Result.class);
      Optional<ResourceOrTagArgument.Result<T>> value = argument.cast(registryKey);
      return value.orElseThrow(() -> (CommandSyntaxException)argument.unwrap().map(element -> {
         ResourceKey<?> elementKey = element.key();
         return ResourceArgument.ERROR_INVALID_RESOURCE_TYPE.create(elementKey.identifier(), elementKey.registry(), registryKey.identifier());
      }, tag -> {
         TagKey<?> tagKey = tag.key();
         return ERROR_INVALID_TAG_TYPE.create(tagKey.location(), tagKey.registry(), registryKey.identifier());
      }));
   }

   public ResourceOrTagArgument.Result<T> parse(final StringReader reader) throws CommandSyntaxException {
      if (reader.canRead() && reader.peek() == '#') {
         int cursor = reader.getCursor();

         try {
            reader.skip();
            Identifier tagId = Identifier.read(reader);
            TagKey<T> tagKey = TagKey.create(this.registryKey, tagId);
            HolderSet.Named<T> holderSet = this.registryLookup
               .get(tagKey)
               .orElseThrow(() -> ERROR_UNKNOWN_TAG.createWithContext(reader, tagId, this.registryKey.identifier()));
            return new ResourceOrTagArgument.TagResult<>(holderSet);
         } catch (CommandSyntaxException e) {
            reader.setCursor(cursor);
            throw e;
         }
      } else {
         Identifier resourceId = Identifier.read(reader);
         ResourceKey<T> resourceKey = ResourceKey.create(this.registryKey, resourceId);
         Holder.Reference<T> holder = this.registryLookup
            .get(resourceKey)
            .orElseThrow(() -> ResourceArgument.ERROR_UNKNOWN_RESOURCE.createWithContext(reader, resourceId, this.registryKey.identifier()));
         return new ResourceOrTagArgument.ResourceResult<>(holder);
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
      return SharedSuggestionProvider.listSuggestions(context, builder, this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ALL);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static class Info<T> implements ArgumentTypeInfo<ResourceOrTagArgument<T>, ResourceOrTagArgument.Info<T>.Template> {
      public void serializeToNetwork(final ResourceOrTagArgument.Info<T>.Template template, final FriendlyByteBuf out) {
         out.writeResourceKey(template.registryKey);
      }

      public ResourceOrTagArgument.Info<T>.Template deserializeFromNetwork(final FriendlyByteBuf in) {
         return new ResourceOrTagArgument.Info.Template(in.readRegistryKey());
      }

      public void serializeToJson(final ResourceOrTagArgument.Info<T>.Template template, final JsonObject out) {
         out.addProperty("registry", template.registryKey.identifier().toString());
      }

      public ResourceOrTagArgument.Info<T>.Template unpack(final ResourceOrTagArgument<T> argument) {
         return new ResourceOrTagArgument.Info.Template(argument.registryKey);
      }

      public final class Template implements ArgumentTypeInfo.Template<ResourceOrTagArgument<T>> {
         private final ResourceKey<? extends Registry<T>> registryKey;

         private Template(final ResourceKey<? extends Registry<T>> registryKey) {
            this.registryKey = registryKey;
         }

         public ResourceOrTagArgument<T> instantiate(final CommandBuildContext context) {
            return new ResourceOrTagArgument<>(context, this.registryKey);
         }

         @Override
         public ArgumentTypeInfo<ResourceOrTagArgument<T>, ?> type() {
            return Info.this;
         }
      }
   }

   private record ResourceResult<T>(Holder.Reference<T> value) implements ResourceOrTagArgument.Result<T> {
      @Override
      public Either<Holder.Reference<T>, HolderSet.Named<T>> unwrap() {
         return Either.left(this.value);
      }

      @Override
      public <E> Optional<ResourceOrTagArgument.Result<E>> cast(final ResourceKey<? extends Registry<E>> registryKey) {
         return this.value.key().isFor(registryKey) ? Optional.of((ResourceOrTagArgument.Result<E>)this) : Optional.empty();
      }

      public boolean test(final Holder<T> holder) {
         return holder.equals(this.value);
      }

      @Override
      public String asPrintable() {
         return this.value.key().identifier().toString();
      }
   }

   public interface Result<T> extends Predicate<Holder<T>> {
      Either<Holder.Reference<T>, HolderSet.Named<T>> unwrap();

      <E> Optional<ResourceOrTagArgument.Result<E>> cast(final ResourceKey<? extends Registry<E>> registryKey);

      String asPrintable();
   }

   private record TagResult<T>(HolderSet.Named<T> tag) implements ResourceOrTagArgument.Result<T> {
      @Override
      public Either<Holder.Reference<T>, HolderSet.Named<T>> unwrap() {
         return Either.right(this.tag);
      }

      @Override
      public <E> Optional<ResourceOrTagArgument.Result<E>> cast(final ResourceKey<? extends Registry<E>> registryKey) {
         return this.tag.key().isFor(registryKey) ? Optional.of((ResourceOrTagArgument.Result<E>)this) : Optional.empty();
      }

      public boolean test(final Holder<T> holder) {
         return this.tag.contains(holder);
      }

      @Override
      public String asPrintable() {
         return "#" + this.tag.key().location();
      }
   }
}
