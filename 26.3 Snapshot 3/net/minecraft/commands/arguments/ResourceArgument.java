package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.timeline.Timeline;

public class ResourceArgument<T> implements ArgumentType<Holder.Reference<T>> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
   private static final DynamicCommandExceptionType ERROR_NOT_SUMMONABLE_ENTITY = new DynamicCommandExceptionType(
      value -> Component.translatableEscape("entity.not_summonable", value)
   );
   public static final Dynamic2CommandExceptionType ERROR_UNKNOWN_RESOURCE = new Dynamic2CommandExceptionType(
      (id, registry) -> Component.translatableEscape("argument.resource.not_found", id, registry)
   );
   public static final Dynamic3CommandExceptionType ERROR_INVALID_RESOURCE_TYPE = new Dynamic3CommandExceptionType(
      (id, actualRegistry, expectedRegistry) -> Component.translatableEscape("argument.resource.invalid_type", id, actualRegistry, expectedRegistry)
   );
   private final ResourceKey<? extends Registry<T>> registryKey;
   private final HolderLookup<T> registryLookup;

   public ResourceArgument(final CommandBuildContext context, final ResourceKey<? extends Registry<T>> registryKey) {
      this.registryKey = registryKey;
      this.registryLookup = context.lookupOrThrow(registryKey);
   }

   public static <T> ResourceArgument<T> resource(final CommandBuildContext context, final ResourceKey<? extends Registry<T>> key) {
      return new ResourceArgument<>(context, key);
   }

   public static <T> Holder.Reference<T> getResource(
      final CommandContext<CommandSourceStack> context, final String name, final ResourceKey<Registry<T>> registryKey
   ) throws CommandSyntaxException {
      Holder.Reference<T> argument = (Holder.Reference<T>)context.getArgument(name, Holder.Reference.class);
      ResourceKey<?> argumentKey = argument.key();
      if (argumentKey.isFor(registryKey)) {
         return argument;
      } else {
         throw ERROR_INVALID_RESOURCE_TYPE.create(argumentKey.identifier(), argumentKey.registry(), registryKey.identifier());
      }
   }

   public static Holder.Reference<Attribute> getAttribute(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      return getResource(context, name, Registries.ATTRIBUTE);
   }

   public static Holder.Reference<EntityType<?>> getSummonableEntityType(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      Holder.Reference<EntityType<?>> result = getResource(context, name, Registries.ENTITY_TYPE);
      if (!result.value().canSummon()) {
         throw ERROR_NOT_SUMMONABLE_ENTITY.create(result.key().identifier().toString());
      } else {
         return result;
      }
   }

   public static Holder.Reference<MobEffect> getMobEffect(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      return getResource(context, name, Registries.MOB_EFFECT);
   }

   public static Holder.Reference<Enchantment> getEnchantment(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      return getResource(context, name, Registries.ENCHANTMENT);
   }

   public static Holder.Reference<WorldClock> getClock(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      return getResource(context, name, Registries.WORLD_CLOCK);
   }

   public static Holder.Reference<Timeline> getTimeline(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      return getResource(context, name, Registries.TIMELINE);
   }

   public Holder.Reference<T> parse(final StringReader reader) throws CommandSyntaxException {
      Identifier resourceId = Identifier.read(reader);
      ResourceKey<T> keyInRegistry = ResourceKey.create(this.registryKey, resourceId);
      return this.registryLookup
         .get(keyInRegistry)
         .orElseThrow(() -> ERROR_UNKNOWN_RESOURCE.createWithContext(reader, resourceId, this.registryKey.identifier()));
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
      return SharedSuggestionProvider.listSuggestions(context, builder, this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ELEMENTS);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static class Info<T> implements ArgumentTypeInfo<ResourceArgument<T>, ResourceArgument.Info<T>.Template> {
      public void serializeToNetwork(final ResourceArgument.Info<T>.Template template, final FriendlyByteBuf out) {
         out.writeResourceKey(template.registryKey);
      }

      public ResourceArgument.Info<T>.Template deserializeFromNetwork(final FriendlyByteBuf in) {
         return new ResourceArgument.Info.Template(in.readRegistryKey());
      }

      public void serializeToJson(final ResourceArgument.Info<T>.Template template, final JsonObject out) {
         out.addProperty("registry", template.registryKey.identifier().toString());
      }

      public ResourceArgument.Info<T>.Template unpack(final ResourceArgument<T> argument) {
         return new ResourceArgument.Info.Template(argument.registryKey);
      }

      public final class Template implements ArgumentTypeInfo.Template<ResourceArgument<T>> {
         private final ResourceKey<? extends Registry<T>> registryKey;

         private Template(final ResourceKey<? extends Registry<T>> registryKey) {
            this.registryKey = registryKey;
         }

         public ResourceArgument<T> instantiate(final CommandBuildContext context) {
            return new ResourceArgument<>(context, this.registryKey);
         }

         @Override
         public ArgumentTypeInfo<ResourceArgument<T>, ?> type() {
            return Info.this;
         }
      }
   }
}
