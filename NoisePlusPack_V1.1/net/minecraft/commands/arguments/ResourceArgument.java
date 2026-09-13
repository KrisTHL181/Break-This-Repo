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
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.Structure;

public class ResourceArgument<T> implements ArgumentType<Holder.Reference<T>> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
   private static final DynamicCommandExceptionType ERROR_NOT_SUMMONABLE_ENTITY = new DynamicCommandExceptionType(
      p_308361_ -> Component.translatableEscape("entity.not_summonable", p_308361_)
   );
   public static final Dynamic2CommandExceptionType ERROR_UNKNOWN_RESOURCE = new Dynamic2CommandExceptionType(
      (p_308359_, p_308360_) -> Component.translatableEscape("argument.resource.not_found", p_308359_, p_308360_)
   );
   public static final Dynamic3CommandExceptionType ERROR_INVALID_RESOURCE_TYPE = new Dynamic3CommandExceptionType(
      (p_308362_, p_308363_, p_308364_) -> Component.translatableEscape("argument.resource.invalid_type", p_308362_, p_308363_, p_308364_)
   );
   final ResourceKey<? extends Registry<T>> registryKey;
   private final HolderLookup<T> registryLookup;

   public ResourceArgument(CommandBuildContext p_248597_, ResourceKey<? extends Registry<T>> p_251778_) {
      this.registryKey = p_251778_;
      this.registryLookup = p_248597_.lookupOrThrow(p_251778_);
   }

   public static <T> ResourceArgument<T> resource(CommandBuildContext p_249973_, ResourceKey<? extends Registry<T>> p_251405_) {
      return new ResourceArgument<>(p_249973_, p_251405_);
   }

   public static <T> Holder.Reference<T> getResource(CommandContext<CommandSourceStack> p_251788_, String p_251996_, ResourceKey<Registry<T>> p_250077_) throws CommandSyntaxException {
      Holder.Reference<T> reference = (Holder.Reference<T>)p_251788_.getArgument(p_251996_, Holder.Reference.class);
      ResourceKey<?> resourcekey = reference.key();
      if (resourcekey.isFor(p_250077_)) {
         return reference;
      } else {
         throw ERROR_INVALID_RESOURCE_TYPE.create(resourcekey.identifier(), resourcekey.registry(), p_250077_.identifier());
      }
   }

   public static Holder.Reference<Attribute> getAttribute(CommandContext<CommandSourceStack> p_248753_, String p_251157_) throws CommandSyntaxException {
      return getResource(p_248753_, p_251157_, Registries.ATTRIBUTE);
   }

   public static Holder.Reference<ConfiguredFeature<?, ?>> getConfiguredFeature(CommandContext<CommandSourceStack> p_250819_, String p_252256_) throws CommandSyntaxException {
      return getResource(p_250819_, p_252256_, Registries.CONFIGURED_FEATURE);
   }

   public static Holder.Reference<Structure> getStructure(CommandContext<CommandSourceStack> p_250288_, String p_250856_) throws CommandSyntaxException {
      return getResource(p_250288_, p_250856_, Registries.STRUCTURE);
   }

   public static Holder.Reference<EntityType<?>> getEntityType(CommandContext<CommandSourceStack> p_251258_, String p_252322_) throws CommandSyntaxException {
      return getResource(p_251258_, p_252322_, Registries.ENTITY_TYPE);
   }

   public static Holder.Reference<EntityType<?>> getSummonableEntityType(CommandContext<CommandSourceStack> p_251880_, String p_250243_) throws CommandSyntaxException {
      Holder.Reference<EntityType<?>> reference = getResource(p_251880_, p_250243_, Registries.ENTITY_TYPE);
      if (!reference.value().canSummon()) {
         throw ERROR_NOT_SUMMONABLE_ENTITY.create(reference.key().identifier().toString());
      } else {
         return reference;
      }
   }

   public static Holder.Reference<MobEffect> getMobEffect(CommandContext<CommandSourceStack> p_250521_, String p_249927_) throws CommandSyntaxException {
      return getResource(p_250521_, p_249927_, Registries.MOB_EFFECT);
   }

   public static Holder.Reference<Enchantment> getEnchantment(CommandContext<CommandSourceStack> p_248656_, String p_248713_) throws CommandSyntaxException {
      return getResource(p_248656_, p_248713_, Registries.ENCHANTMENT);
   }

   public Holder.Reference<T> parse(StringReader p_250909_) throws CommandSyntaxException {
      Identifier identifier = Identifier.read(p_250909_);
      ResourceKey<T> resourcekey = ResourceKey.create(this.registryKey, identifier);
      return this.registryLookup
         .get(resourcekey)
         .orElseThrow(() -> ERROR_UNKNOWN_RESOURCE.createWithContext(p_250909_, identifier, this.registryKey.identifier()));
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_249391_, SuggestionsBuilder p_251197_) {
      return SharedSuggestionProvider.listSuggestions(p_249391_, p_251197_, this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ELEMENTS);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static class Info<T> implements ArgumentTypeInfo<ResourceArgument<T>, ResourceArgument.Info<T>.Template> {
      public void serializeToNetwork(ResourceArgument.Info<T>.Template p_250470_, FriendlyByteBuf p_248658_) {
         p_248658_.writeResourceKey(p_250470_.registryKey);
      }

      public ResourceArgument.Info<T>.Template deserializeFromNetwork(FriendlyByteBuf p_248958_) {
         return new ResourceArgument.Info.Template(p_248958_.readRegistryKey());
      }

      public void serializeToJson(ResourceArgument.Info<T>.Template p_251267_, JsonObject p_250142_) {
         p_250142_.addProperty("registry", p_251267_.registryKey.identifier().toString());
      }

      public ResourceArgument.Info<T>.Template unpack(ResourceArgument<T> p_250667_) {
         return new ResourceArgument.Info.Template(p_250667_.registryKey);
      }

      public final class Template implements ArgumentTypeInfo.Template<ResourceArgument<T>> {
         final ResourceKey<? extends Registry<T>> registryKey;

         Template(final ResourceKey<? extends Registry<T>> p_250598_) {
            this.registryKey = p_250598_;
         }

         public ResourceArgument<T> instantiate(CommandBuildContext p_251900_) {
            return new ResourceArgument<>(p_251900_, this.registryKey);
         }

         @Override
         public ArgumentTypeInfo<ResourceArgument<T>, ?> type() {
            return Info.this;
         }
      }
   }
}
