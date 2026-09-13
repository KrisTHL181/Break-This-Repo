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
import net.minecraft.advancements.criterion.MinMaxBounds;
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
   static final DynamicCommandExceptionType ERROR_UNKNOWN_ITEM = new DynamicCommandExceptionType(
      p_325619_ -> Component.translatableEscape("argument.item.id.invalid", p_325619_)
   );
   static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType(
      p_325632_ -> Component.translatableEscape("arguments.item.tag.unknown", p_325632_)
   );
   static final DynamicCommandExceptionType ERROR_UNKNOWN_COMPONENT = new DynamicCommandExceptionType(
      p_325626_ -> Component.translatableEscape("arguments.item.component.unknown", p_325626_)
   );
   static final Dynamic2CommandExceptionType ERROR_MALFORMED_COMPONENT = new Dynamic2CommandExceptionType(
      (p_325624_, p_325625_) -> Component.translatableEscape("arguments.item.component.malformed", p_325624_, p_325625_)
   );
   static final DynamicCommandExceptionType ERROR_UNKNOWN_PREDICATE = new DynamicCommandExceptionType(
      p_325623_ -> Component.translatableEscape("arguments.item.predicate.unknown", p_325623_)
   );
   static final Dynamic2CommandExceptionType ERROR_MALFORMED_PREDICATE = new Dynamic2CommandExceptionType(
      (p_325617_, p_325618_) -> Component.translatableEscape("arguments.item.predicate.malformed", p_325617_, p_325618_)
   );
   private static final Identifier COUNT_ID = Identifier.withDefaultNamespace("count");
   static final Map<Identifier, ItemPredicateArgument.ComponentWrapper> PSEUDO_COMPONENTS = Stream.of(
         new ItemPredicateArgument.ComponentWrapper(
            COUNT_ID, p_325630_ -> true, MinMaxBounds.Ints.CODEC.map(p_458320_ -> p_448523_ -> p_458320_.matches(p_448523_.getCount()))
         )
      )
      .collect(Collectors.toUnmodifiableMap(ItemPredicateArgument.ComponentWrapper::id, p_325629_ -> (ItemPredicateArgument.ComponentWrapper)p_325629_));
   static final Map<Identifier, ItemPredicateArgument.PredicateWrapper> PSEUDO_PREDICATES = Stream.of(
         new ItemPredicateArgument.PredicateWrapper(COUNT_ID, MinMaxBounds.Ints.CODEC.map(p_451866_ -> p_448527_ -> p_451866_.matches(p_448527_.getCount())))
      )
      .collect(Collectors.toUnmodifiableMap(ItemPredicateArgument.PredicateWrapper::id, p_448529_ -> (ItemPredicateArgument.PredicateWrapper)p_448529_));

   private static ItemPredicateArgument.PredicateWrapper createComponentExistencePredicate(Holder.Reference<DataComponentType<?>> p_451757_) {
      Predicate<ItemStack> predicate = p_448525_ -> p_448525_.has(p_451757_.value());
      return new ItemPredicateArgument.PredicateWrapper(p_451757_.key().identifier(), Unit.CODEC.map(p_451172_ -> predicate));
   }

   public ItemPredicateArgument(CommandBuildContext p_235352_) {
      super(ComponentPredicateParser.createGrammar(new ItemPredicateArgument.Context(p_235352_)).mapResult(p_448528_ -> Util.allOf(p_448528_)::test));
   }

   public static ItemPredicateArgument itemPredicate(CommandBuildContext p_235354_) {
      return new ItemPredicateArgument(p_235354_);
   }

   public static ItemPredicateArgument.Result getItemPredicate(CommandContext<CommandSourceStack> p_121041_, String p_121042_) {
      return (ItemPredicateArgument.Result)p_121041_.getArgument(p_121042_, ItemPredicateArgument.Result.class);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   record ComponentWrapper(Identifier id, Predicate<ItemStack> presenceChecker, Decoder<? extends Predicate<ItemStack>> valueChecker) {
      public static <T> ItemPredicateArgument.ComponentWrapper create(ImmutableStringReader p_336159_, Identifier p_459661_, DataComponentType<T> p_331569_) throws CommandSyntaxException {
         Codec<T> codec = p_331569_.codec();
         if (codec == null) {
            throw ItemPredicateArgument.ERROR_UNKNOWN_COMPONENT.createWithContext(p_336159_, p_459661_);
         } else {
            return new ItemPredicateArgument.ComponentWrapper(p_459661_, p_389648_ -> p_389648_.has(p_331569_), codec.map(p_335085_ -> p_331446_ -> {
               T t = p_331446_.get(p_331569_);
               return Objects.equals(p_335085_, t);
            }));
         }
      }

      public Predicate<ItemStack> decode(ImmutableStringReader p_333508_, Dynamic<?> p_392903_) throws CommandSyntaxException {
         DataResult<? extends Predicate<ItemStack>> dataresult = this.valueChecker.parse(p_392903_);
         return (Predicate<ItemStack>)dataresult.getOrThrow(
            p_448531_ -> ItemPredicateArgument.ERROR_MALFORMED_COMPONENT.createWithContext(p_333508_, this.id.toString(), p_448531_)
         );
      }
   }

   static class Context
      implements ComponentPredicateParser.Context<Predicate<ItemStack>, ItemPredicateArgument.ComponentWrapper, ItemPredicateArgument.PredicateWrapper> {
      private final HolderLookup.Provider registries;
      private final HolderLookup.RegistryLookup<Item> items;
      private final HolderLookup.RegistryLookup<DataComponentType<?>> components;
      private final HolderLookup.RegistryLookup<DataComponentPredicate.Type<?>> predicates;

      Context(HolderLookup.Provider p_331757_) {
         this.registries = p_331757_;
         this.items = p_331757_.lookupOrThrow(Registries.ITEM);
         this.components = p_331757_.lookupOrThrow(Registries.DATA_COMPONENT_TYPE);
         this.predicates = p_331757_.lookupOrThrow(Registries.DATA_COMPONENT_PREDICATE_TYPE);
      }

      public Predicate<ItemStack> forElementType(ImmutableStringReader p_328916_, Identifier p_453851_) throws CommandSyntaxException {
         Holder.Reference<Item> reference = this.items
            .get(ResourceKey.create(Registries.ITEM, p_453851_))
            .orElseThrow(() -> ItemPredicateArgument.ERROR_UNKNOWN_ITEM.createWithContext(p_328916_, p_453851_));
         return p_333639_ -> p_333639_.is(reference);
      }

      public Predicate<ItemStack> forTagType(ImmutableStringReader p_327668_, Identifier p_458346_) throws CommandSyntaxException {
         HolderSet<Item> holderset = this.items
            .get(TagKey.create(Registries.ITEM, p_458346_))
            .orElseThrow(() -> ItemPredicateArgument.ERROR_UNKNOWN_TAG.createWithContext(p_327668_, p_458346_));
         return p_334213_ -> p_334213_.is(holderset);
      }

      public ItemPredicateArgument.ComponentWrapper lookupComponentType(ImmutableStringReader p_329300_, Identifier p_452802_) throws CommandSyntaxException {
         ItemPredicateArgument.ComponentWrapper itempredicateargument$componentwrapper = ItemPredicateArgument.PSEUDO_COMPONENTS.get(p_452802_);
         if (itempredicateargument$componentwrapper != null) {
            return itempredicateargument$componentwrapper;
         }

         DataComponentType<?> datacomponenttype = this.components
            .get(ResourceKey.create(Registries.DATA_COMPONENT_TYPE, p_452802_))
            .map(Holder::value)
            .orElseThrow(() -> ItemPredicateArgument.ERROR_UNKNOWN_COMPONENT.createWithContext(p_329300_, p_452802_));
         return ItemPredicateArgument.ComponentWrapper.create(p_329300_, p_452802_, datacomponenttype);
      }

      public Predicate<ItemStack> createComponentTest(ImmutableStringReader p_331947_, ItemPredicateArgument.ComponentWrapper p_395127_, Dynamic<?> p_392033_) throws CommandSyntaxException {
         return p_395127_.decode(p_331947_, RegistryOps.injectRegistryContext(p_392033_, this.registries));
      }

      public Predicate<ItemStack> createComponentTest(ImmutableStringReader p_330923_, ItemPredicateArgument.ComponentWrapper p_336299_) {
         return p_336299_.presenceChecker;
      }

      public ItemPredicateArgument.PredicateWrapper lookupPredicateType(ImmutableStringReader p_336060_, Identifier p_459126_) throws CommandSyntaxException {
         ItemPredicateArgument.PredicateWrapper itempredicateargument$predicatewrapper = ItemPredicateArgument.PSEUDO_PREDICATES.get(p_459126_);
         return itempredicateargument$predicatewrapper != null
            ? itempredicateargument$predicatewrapper
            : this.predicates
               .get(ResourceKey.create(Registries.DATA_COMPONENT_PREDICATE_TYPE, p_459126_))
               .map(ItemPredicateArgument.PredicateWrapper::new)
               .or(
                  () -> this.components
                     .get(ResourceKey.create(Registries.DATA_COMPONENT_TYPE, p_459126_))
                     .map(ItemPredicateArgument::createComponentExistencePredicate)
               )
               .orElseThrow(() -> ItemPredicateArgument.ERROR_UNKNOWN_PREDICATE.createWithContext(p_336060_, p_459126_));
      }

      public Predicate<ItemStack> createPredicateTest(ImmutableStringReader p_332241_, ItemPredicateArgument.PredicateWrapper p_335982_, Dynamic<?> p_394550_) throws CommandSyntaxException {
         return p_335982_.decode(p_332241_, RegistryOps.injectRegistryContext(p_394550_, this.registries));
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
            this.components.listElements().filter(p_334864_ -> !p_334864_.value().isTransient()).map(p_448533_ -> p_448533_.key().identifier())
         );
      }

      @Override
      public Stream<Identifier> listPredicateTypes() {
         return Stream.concat(ItemPredicateArgument.PSEUDO_PREDICATES.keySet().stream(), this.predicates.listElementIds().map(ResourceKey::identifier));
      }

      public Predicate<ItemStack> negate(Predicate<ItemStack> p_328753_) {
         return p_328753_.negate();
      }

      public Predicate<ItemStack> anyOf(List<Predicate<ItemStack>> p_329990_) {
         return Util.anyOf(p_329990_);
      }
   }

   record PredicateWrapper(Identifier id, Decoder<? extends Predicate<ItemStack>> type) {
      public PredicateWrapper(Holder.Reference<DataComponentPredicate.Type<?>> p_327901_) {
         this(p_327901_.key().identifier(), p_327901_.value().codec().map(p_394448_ -> p_394448_::matches));
      }

      public Predicate<ItemStack> decode(ImmutableStringReader p_335853_, Dynamic<?> p_391468_) throws CommandSyntaxException {
         DataResult<? extends Predicate<ItemStack>> dataresult = this.type.parse(p_391468_);
         return (Predicate<ItemStack>)dataresult.getOrThrow(
            p_448535_ -> ItemPredicateArgument.ERROR_MALFORMED_PREDICATE.createWithContext(p_335853_, this.id.toString(), p_448535_)
         );
      }
   }

   public interface Result extends Predicate<ItemStack> {
   }
}
