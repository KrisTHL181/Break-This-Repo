package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.parsing.packrat.commands.ParserBasedArgument;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemPredicateArgument extends ParserBasedArgument<ItemPredicateArgument.Result> {
   private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "#stick", "#stick{foo:'bar'}");
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_ITEM = new DynamicCommandExceptionType(
      id -> Component.translatableEscape("argument.item.id.invalid", id)
   );
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType(
      tag -> Component.translatableEscape("arguments.item.tag.unknown", tag)
   );
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_COMPONENT = new DynamicCommandExceptionType(
      id -> Component.translatableEscape("arguments.item.component.unknown", id)
   );
   private static final Dynamic2CommandExceptionType ERROR_MALFORMED_COMPONENT = new Dynamic2CommandExceptionType(
      (type, message) -> Component.translatableEscape("arguments.item.component.malformed", type, message)
   );
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_PREDICATE = new DynamicCommandExceptionType(
      id -> Component.translatableEscape("arguments.item.predicate.unknown", id)
   );
   private static final Dynamic2CommandExceptionType ERROR_MALFORMED_PREDICATE = new Dynamic2CommandExceptionType(
      (type, message) -> Component.translatableEscape("arguments.item.predicate.malformed", type, message)
   );
   private static final Identifier COUNT_ID = Identifier.withDefaultNamespace("count");
   private static final Map<Identifier, ItemPredicateArgument.ComponentWrapper> PSEUDO_COMPONENTS = Stream.of(
         new ItemPredicateArgument.ComponentWrapper(
            COUNT_ID, itemStack -> true, MinMaxBounds.Ints.CODEC.map(range -> itemStack -> range.matches(itemStack.getCount()))
         )
      )
      .collect(Collectors.toUnmodifiableMap(ItemPredicateArgument.ComponentWrapper::id, e -> (ItemPredicateArgument.ComponentWrapper)e));
   private static final Map<Identifier, ItemPredicateArgument.PredicateWrapper> PSEUDO_PREDICATES = Stream.of(
         new ItemPredicateArgument.PredicateWrapper(COUNT_ID, MinMaxBounds.Ints.CODEC.map(range -> itemStack -> range.matches(itemStack.getCount())))
      )
      .collect(Collectors.toUnmodifiableMap(ItemPredicateArgument.PredicateWrapper::id, e -> (ItemPredicateArgument.PredicateWrapper)e));

   private static ItemPredicateArgument.PredicateWrapper createComponentExistencePredicate(final Holder.Reference<DataComponentType<?>> componentId) {
      Predicate<ItemStack> componentExists = itemStack -> itemStack.has(componentId.value());
      return new ItemPredicateArgument.PredicateWrapper(componentId.key().identifier(), Unit.CODEC.map(unit -> componentExists));
   }

   public ItemPredicateArgument(final CommandBuildContext registries) {
      super(ComponentPredicateParser.createGrammar(new ItemPredicateArgument.Context(registries)).mapResult(predicates -> Util.allOf(predicates)::test));
   }

   public static ItemPredicateArgument itemPredicate(final CommandBuildContext context) {
      return new ItemPredicateArgument(context);
   }

   public static ItemPredicateArgument.Result getItemPredicate(final CommandContext<CommandSourceStack> context, final String name) {
      return (ItemPredicateArgument.Result)context.getArgument(name, ItemPredicateArgument.Result.class);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   private record ComponentWrapper(Identifier id, Predicate<ItemStack> presenceChecker, Decoder<? extends Predicate<ItemStack>> valueChecker) {
      public static <T> ItemPredicateArgument.ComponentWrapper create(final ImmutableStringReader reader, final Identifier id, final DataComponentType<T> type) throws CommandSyntaxException {
         Codec<T> codec = type.codec();
         if (codec == null) {
            throw ItemPredicateArgument.ERROR_UNKNOWN_COMPONENT.createWithContext(reader, id);
         } else {
            return new ItemPredicateArgument.ComponentWrapper(id, itemStack -> itemStack.has(type), codec.map(expected -> itemStack -> {
               T actual = itemStack.get(type);
               return Objects.equals(expected, actual);
            }));
         }
      }

      public Predicate<ItemStack> decode(final ImmutableStringReader reader, final Dynamic<?> value) throws CommandSyntaxException {
         DataResult<? extends Predicate<ItemStack>> result = this.valueChecker.parse(value);
         return (Predicate<ItemStack>)result.getOrThrow(
            message -> ItemPredicateArgument.ERROR_MALFORMED_COMPONENT.createWithContext(reader, this.id.toString(), message)
         );
      }
   }

   private static class Context
      implements ComponentPredicateParser.Context<Predicate<ItemStack>, ItemPredicateArgument.ComponentWrapper, ItemPredicateArgument.PredicateWrapper> {
      private final HolderLookup.Provider registries;
      private final HolderLookup.RegistryLookup<Item> items;
      private final HolderLookup.RegistryLookup<DataComponentType<?>> components;
      private final HolderLookup.RegistryLookup<DataComponentPredicate.Type<?>> predicates;

      private Context(final HolderLookup.Provider registries) {
         this.registries = registries;
         this.items = registries.lookupOrThrow(Registries.ITEM);
         this.components = registries.lookupOrThrow(Registries.DATA_COMPONENT_TYPE);
         this.predicates = registries.lookupOrThrow(Registries.DATA_COMPONENT_PREDICATE_TYPE);
      }

      public Predicate<ItemStack> forElementType(final ImmutableStringReader reader, final Identifier id) throws CommandSyntaxException {
         Holder.Reference<Item> item = this.items
            .get(ResourceKey.create(Registries.ITEM, id))
            .orElseThrow(() -> ItemPredicateArgument.ERROR_UNKNOWN_ITEM.createWithContext(reader, id));
         return itemStack -> itemStack.is(item);
      }

      public Predicate<ItemStack> forTagType(final ImmutableStringReader reader, final Identifier id) throws CommandSyntaxException {
         HolderSet<Item> tag = this.items
            .get(TagKey.create(Registries.ITEM, id))
            .orElseThrow(() -> ItemPredicateArgument.ERROR_UNKNOWN_TAG.createWithContext(reader, id));
         return itemStack -> itemStack.is(tag);
      }

      public ItemPredicateArgument.ComponentWrapper lookupComponentType(final ImmutableStringReader reader, final Identifier componentId) throws CommandSyntaxException {
         ItemPredicateArgument.ComponentWrapper wrapper = ItemPredicateArgument.PSEUDO_COMPONENTS.get(componentId);
         if (wrapper != null) {
            return wrapper;
         }

         DataComponentType<?> componentType = this.components
            .get(ResourceKey.create(Registries.DATA_COMPONENT_TYPE, componentId))
            .map(Holder::value)
            .orElseThrow(() -> ItemPredicateArgument.ERROR_UNKNOWN_COMPONENT.createWithContext(reader, componentId));
         return ItemPredicateArgument.ComponentWrapper.create(reader, componentId, componentType);
      }

      public Predicate<ItemStack> createComponentTest(
         final ImmutableStringReader reader, final ItemPredicateArgument.ComponentWrapper componentType, final Dynamic<?> value
      ) throws CommandSyntaxException {
         return componentType.decode(reader, RegistryOps.injectRegistryContext(value, this.registries));
      }

      public Predicate<ItemStack> createComponentTest(final ImmutableStringReader reader, final ItemPredicateArgument.ComponentWrapper componentType) {
         return componentType.presenceChecker;
      }

      public ItemPredicateArgument.PredicateWrapper lookupPredicateType(final ImmutableStringReader reader, final Identifier componentId) throws CommandSyntaxException {
         ItemPredicateArgument.PredicateWrapper wrapper = ItemPredicateArgument.PSEUDO_PREDICATES.get(componentId);
         return wrapper != null
            ? wrapper
            : this.predicates
               .get(ResourceKey.create(Registries.DATA_COMPONENT_PREDICATE_TYPE, componentId))
               .map(ItemPredicateArgument.PredicateWrapper::new)
               .or(
                  () -> this.components
                     .get(ResourceKey.create(Registries.DATA_COMPONENT_TYPE, componentId))
                     .map(ItemPredicateArgument::createComponentExistencePredicate)
               )
               .orElseThrow(() -> ItemPredicateArgument.ERROR_UNKNOWN_PREDICATE.createWithContext(reader, componentId));
      }

      public Predicate<ItemStack> createPredicateTest(
         final ImmutableStringReader reader, final ItemPredicateArgument.PredicateWrapper predicateType, final Dynamic<?> value
      ) throws CommandSyntaxException {
         return predicateType.decode(reader, RegistryOps.injectRegistryContext(value, this.registries));
      }

      @Override
      public Stream<Identifier> listElementTypes() {
         return this.items.listElementIds().map(ResourceKey::identifier);
      }

      @Override
      public Stream<Identifier> listTagTypes() {
         return this.items.listTagIds().map(TagKey::location);
      }

      @Override
      public Stream<Identifier> listComponentTypes() {
         return Stream.concat(
            ItemPredicateArgument.PSEUDO_COMPONENTS.keySet().stream(),
            this.components.listElements().filter(e -> !e.value().isTransient()).map(e -> e.key().identifier())
         );
      }

      @Override
      public Stream<Identifier> listPredicateTypes() {
         return Stream.concat(ItemPredicateArgument.PSEUDO_PREDICATES.keySet().stream(), this.predicates.listElementIds().map(ResourceKey::identifier));
      }

      public Predicate<ItemStack> negate(final Predicate<ItemStack> value) {
         return value.negate();
      }

      public Predicate<ItemStack> anyOf(final List<Predicate<ItemStack>> alternatives) {
         return Util.anyOf(alternatives);
      }
   }

   private record PredicateWrapper(Identifier id, Decoder<? extends Predicate<ItemStack>> type) {
      public PredicateWrapper(final Holder.Reference<DataComponentPredicate.Type<?>> holder) {
         this(holder.key().identifier(), holder.value().codec().map(v -> v::matches));
      }

      public Predicate<ItemStack> decode(final ImmutableStringReader reader, final Dynamic<?> value) throws CommandSyntaxException {
         DataResult<? extends Predicate<ItemStack>> result = this.type.parse(value);
         return (Predicate<ItemStack>)result.getOrThrow(
            message -> ItemPredicateArgument.ERROR_MALFORMED_PREDICATE.createWithContext(reader, this.id.toString(), message)
         );
      }
   }

   public interface Result extends Predicate<ItemStack> {
   }
}
