package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class ResourceKeyArgument<T> implements ArgumentType<ResourceKey<T>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
    private static final DynamicCommandExceptionType ERROR_INVALID_FEATURE = new DynamicCommandExceptionType(
        value -> Component.translatableEscape("commands.place.feature.invalid", value)
    );
    private static final DynamicCommandExceptionType ERROR_INVALID_STRUCTURE = new DynamicCommandExceptionType(
        value -> Component.translatableEscape("commands.place.structure.invalid", value)
    );
    private static final DynamicCommandExceptionType ERROR_INVALID_TEMPLATE_POOL = new DynamicCommandExceptionType(
        value -> Component.translatableEscape("commands.place.jigsaw.invalid", value)
    );
    private static final DynamicCommandExceptionType ERROR_INVALID_RECIPE = new DynamicCommandExceptionType(
        value -> Component.translatableEscape("recipe.notFound", value)
    );
    private static final DynamicCommandExceptionType ERROR_INVALID_ADVANCEMENT = new DynamicCommandExceptionType(
        value -> Component.translatableEscape("advancement.advancementNotFound", value)
    );
    private final ResourceKey<? extends Registry<T>> registryKey;

    public ResourceKeyArgument(final ResourceKey<? extends Registry<T>> registryKey) {
        this.registryKey = registryKey;
    }

    public static <T> ResourceKeyArgument<T> key(final ResourceKey<? extends Registry<T>> key) {
        return new ResourceKeyArgument<>(key);
    }

    public static <T> ResourceKey<T> getRegistryKey(
        final CommandContext<CommandSourceStack> context,
        final String name,
        final ResourceKey<Registry<T>> registryKey,
        final DynamicCommandExceptionType exceptionType
    ) throws CommandSyntaxException {
        ResourceKey<?> argument = context.getArgument(name, ResourceKey.class);
        Optional<ResourceKey<T>> value = argument.cast(registryKey);
        return value.orElseThrow(() -> exceptionType.create(argument.identifier()));
    }

    private static <T> Registry<T> getRegistry(final CommandContext<CommandSourceStack> context, final ResourceKey<? extends Registry<T>> registryKey) {
        return context.getSource().getServer().registryAccess().lookupOrThrow(registryKey);
    }

    private static <T> Holder.Reference<T> resolveKey(
        final CommandContext<CommandSourceStack> context,
        final String name,
        final ResourceKey<Registry<T>> registryKey,
        final DynamicCommandExceptionType exception
    ) throws CommandSyntaxException {
        ResourceKey<T> key = getRegistryKey(context, name, registryKey, exception);
        return getRegistry(context, registryKey).get(key).orElseThrow(() -> exception.create(key.identifier()));
    }

    public static Holder.Reference<ConfiguredFeature<?, ?>> getConfiguredFeature(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
        return resolveKey(context, name, Registries.CONFIGURED_FEATURE, ERROR_INVALID_FEATURE);
    }

    public static Holder.Reference<Structure> getStructure(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
        return resolveKey(context, name, Registries.STRUCTURE, ERROR_INVALID_STRUCTURE);
    }

    public static Holder.Reference<StructureTemplatePool> getStructureTemplatePool(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
        return resolveKey(context, name, Registries.TEMPLATE_POOL, ERROR_INVALID_TEMPLATE_POOL);
    }

    public static RecipeHolder<?> getRecipe(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
        RecipeManager recipeManager = context.getSource().getServer().getRecipeManager();
        ResourceKey<Recipe<?>> key = getRegistryKey(context, name, Registries.RECIPE, ERROR_INVALID_RECIPE);
        return recipeManager.byKey(key).orElseThrow(() -> ERROR_INVALID_RECIPE.create(key.identifier()));
    }

    public static AdvancementHolder getAdvancement(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
        ResourceKey<Advancement> key = getRegistryKey(context, name, Registries.ADVANCEMENT, ERROR_INVALID_ADVANCEMENT);
        AdvancementHolder advancement = context.getSource().getServer().getAdvancements().get(key.identifier());
        if (advancement == null) {
            throw ERROR_INVALID_ADVANCEMENT.create(key.identifier());
        } else {
            return advancement;
        }
    }

    public ResourceKey<T> parse(final StringReader reader) throws CommandSyntaxException {
        Identifier resourceId = Identifier.read(reader);
        return ResourceKey.create(this.registryKey, resourceId);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.listSuggestions(context, builder, this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ELEMENTS);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class Info<T> implements ArgumentTypeInfo<ResourceKeyArgument<T>, ResourceKeyArgument.Info<T>.Template> {
        public void serializeToNetwork(final ResourceKeyArgument.Info<T>.Template template, final FriendlyByteBuf out) {
            out.writeResourceKey(template.registryKey);
        }

        public ResourceKeyArgument.Info<T>.Template deserializeFromNetwork(final FriendlyByteBuf in) {
            return new ResourceKeyArgument.Info.Template(in.readRegistryKey());
        }

        public void serializeToJson(final ResourceKeyArgument.Info<T>.Template template, final JsonObject out) {
            out.addProperty("registry", template.registryKey.identifier().toString());
        }

        public ResourceKeyArgument.Info<T>.Template unpack(final ResourceKeyArgument<T> argument) {
            return new ResourceKeyArgument.Info.Template(argument.registryKey);
        }

        public final class Template implements ArgumentTypeInfo.Template<ResourceKeyArgument<T>> {
            private final ResourceKey<? extends Registry<T>> registryKey;

            private Template(final ResourceKey<? extends Registry<T>> registryKey) {
                this.registryKey = registryKey;
            }

            public ResourceKeyArgument<T> instantiate(final CommandBuildContext context) {
                return new ResourceKeyArgument<>(this.registryKey);
            }

            @Override
            public ArgumentTypeInfo<ResourceKeyArgument<T>, ?> type() {
                return Info.this;
            }
        }
    }
}