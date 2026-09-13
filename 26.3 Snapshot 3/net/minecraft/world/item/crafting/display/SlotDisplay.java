package net.minecraft.world.item.crafting.display;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.block.entity.FuelValues;

public interface SlotDisplay {
   Codec<SlotDisplay> CODEC = BuiltInRegistries.SLOT_DISPLAY.byNameCodec().dispatch(SlotDisplay::type, SlotDisplay.Type::codec);
   StreamCodec<RegistryFriendlyByteBuf, SlotDisplay> STREAM_CODEC = ByteBufCodecs.registry(Registries.SLOT_DISPLAY)
      .dispatch(SlotDisplay::type, SlotDisplay.Type::streamCodec);

   <T> Stream<T> resolve(ContextMap context, DisplayContentsFactory<T> builder);

   SlotDisplay.Type<? extends SlotDisplay> type();

   default boolean isEnabled(final FeatureFlagSet enabledFeatures) {
      return true;
   }

   default List<ItemStack> resolveForStacks(final ContextMap context) {
      return this.resolve(context, SlotDisplay.ItemStackContentsFactory.INSTANCE).toList();
   }

   default ItemStack resolveForFirstStack(final ContextMap context) {
      return this.resolve(context, SlotDisplay.ItemStackContentsFactory.INSTANCE).findFirst().orElse(ItemStack.EMPTY);
   }

   private static <T> Stream<T> applyDemoTransformation(
      final ContextMap context,
      final DisplayContentsFactory<T> factory,
      final SlotDisplay firstDisplay,
      final SlotDisplay secondDisplay,
      final RandomSource randomSource,
      final BinaryOperator<ItemStack> operation
   ) {
      if (factory instanceof DisplayContentsFactory.ForStacks<T> stacks) {
         List<ItemStack> firstItems = firstDisplay.resolveForStacks(context);
         if (firstItems.isEmpty()) {
            return Stream.empty();
         }

         List<ItemStack> secondItems = secondDisplay.resolveForStacks(context);
         return secondItems.isEmpty() ? Stream.empty() : Stream.<ItemStack>generate(() -> {
            ItemStack first = Util.getRandom(firstItems, randomSource);
            ItemStack second = Util.getRandom(secondItems, randomSource);
            return operation.apply(first, second);
         }).limit(256L).filter(s -> !s.isEmpty()).limit(16L).map(stacks::forStack);
      } else {
         return Stream.empty();
      }
   }

   private static <T> Stream<T> applyDemoTransformation(
      final ContextMap context,
      final DisplayContentsFactory<T> factory,
      final SlotDisplay firstDisplay,
      final SlotDisplay secondDisplay,
      final BinaryOperator<ItemStack> operation
   ) {
      if (factory instanceof DisplayContentsFactory.ForStacks<T> stacks) {
         List<ItemStack> firstItems = firstDisplay.resolveForStacks(context);
         if (firstItems.isEmpty()) {
            return Stream.empty();
         }

         List<ItemStack> secondItems = secondDisplay.resolveForStacks(context);
         if (secondItems.isEmpty()) {
            return Stream.empty();
         }

         int cycle = firstItems.size() * secondItems.size();
         return IntStream.range(0, cycle).mapToObj(index -> {
            int firstItemCount = firstItems.size();
            int firstItemIndex = index % firstItemCount;
            int secondItemIndex = index / firstItemCount;
            ItemStack first = firstItems.get(firstItemIndex);
            ItemStack second = secondItems.get(secondItemIndex);
            return operation.apply(first, second);
         }).filter(s -> !s.isEmpty()).limit(16L).map(stacks::forStack);
      } else {
         return Stream.empty();
      }
   }

   class AnyFuel implements SlotDisplay {
      public static final SlotDisplay.AnyFuel INSTANCE = new SlotDisplay.AnyFuel();
      public static final MapCodec<SlotDisplay.AnyFuel> MAP_CODEC = MapCodec.unit(INSTANCE);
      public static final StreamCodec<RegistryFriendlyByteBuf, SlotDisplay.AnyFuel> STREAM_CODEC = StreamCodec.unit(INSTANCE);
      public static final SlotDisplay.Type<SlotDisplay.AnyFuel> TYPE = new SlotDisplay.Type<>(MAP_CODEC, STREAM_CODEC);

      private AnyFuel() {
      }

      @Override
      public SlotDisplay.Type<SlotDisplay.AnyFuel> type() {
         return TYPE;
      }

      @Override
      public String toString() {
         return "<any fuel>";
      }

      @Override
      public <T> Stream<T> resolve(final ContextMap context, final DisplayContentsFactory<T> factory) {
         if (factory instanceof DisplayContentsFactory.ForStacks<T> stacks) {
            FuelValues fuelValues = context.getOptional(SlotDisplayContext.FUEL_VALUES);
            if (fuelValues != null) {
               return fuelValues.fuelItems().stream().map(stacks::forStack);
            }
         }

         return Stream.empty();
      }
   }

   record Composite(List<SlotDisplay> contents) implements SlotDisplay {
      public static final MapCodec<SlotDisplay.Composite> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(SlotDisplay.CODEC.listOf().fieldOf("contents").forGetter(SlotDisplay.Composite::contents)).apply(i, SlotDisplay.Composite::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, SlotDisplay.Composite> STREAM_CODEC = StreamCodec.composite(
         SlotDisplay.STREAM_CODEC.apply(ByteBufCodecs.list()), SlotDisplay.Composite::contents, SlotDisplay.Composite::new
      );
      public static final SlotDisplay.Type<SlotDisplay.Composite> TYPE = new SlotDisplay.Type<>(MAP_CODEC, STREAM_CODEC);

      @Override
      public SlotDisplay.Type<SlotDisplay.Composite> type() {
         return TYPE;
      }

      @Override
      public <T> Stream<T> resolve(final ContextMap context, final DisplayContentsFactory<T> factory) {
         return this.contents.stream().flatMap(d -> d.resolve(context, factory));
      }

      @Override
      public boolean isEnabled(final FeatureFlagSet enabledFeatures) {
         return this.contents.stream().allMatch(c -> c.isEnabled(enabledFeatures));
      }
   }

   record DyedSlotDemo(SlotDisplay dye, SlotDisplay target) implements SlotDisplay {
      public static final MapCodec<SlotDisplay.DyedSlotDemo> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               SlotDisplay.CODEC.fieldOf("dye").forGetter(SlotDisplay.DyedSlotDemo::dye),
               SlotDisplay.CODEC.fieldOf("target").forGetter(SlotDisplay.DyedSlotDemo::target)
            )
            .apply(i, SlotDisplay.DyedSlotDemo::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, SlotDisplay.DyedSlotDemo> STREAM_CODEC = StreamCodec.composite(
         SlotDisplay.STREAM_CODEC, SlotDisplay.DyedSlotDemo::dye, SlotDisplay.STREAM_CODEC, SlotDisplay.DyedSlotDemo::target, SlotDisplay.DyedSlotDemo::new
      );
      public static final SlotDisplay.Type<SlotDisplay.DyedSlotDemo> TYPE = new SlotDisplay.Type<>(MAP_CODEC, STREAM_CODEC);

      @Override
      public SlotDisplay.Type<SlotDisplay.DyedSlotDemo> type() {
         return TYPE;
      }

      @Override
      public <T> Stream<T> resolve(final ContextMap context, final DisplayContentsFactory<T> factory) {
         BinaryOperator<ItemStack> transformation = (target, dye) -> {
            DyeColor dyeValue = dye.getOrDefault(DataComponents.DYE, DyeColor.WHITE);
            return DyedItemColor.applyDyes(target.copy(), List.of(dyeValue));
         };
         return SlotDisplay.applyDemoTransformation(context, factory, this.target, this.dye, transformation);
      }
   }

   class Empty implements SlotDisplay {
      public static final SlotDisplay.Empty INSTANCE = new SlotDisplay.Empty();
      public static final MapCodec<SlotDisplay.Empty> MAP_CODEC = MapCodec.unit(INSTANCE);
      public static final StreamCodec<RegistryFriendlyByteBuf, SlotDisplay.Empty> STREAM_CODEC = StreamCodec.unit(INSTANCE);
      public static final SlotDisplay.Type<SlotDisplay.Empty> TYPE = new SlotDisplay.Type<>(MAP_CODEC, STREAM_CODEC);

      private Empty() {
      }

      @Override
      public SlotDisplay.Type<SlotDisplay.Empty> type() {
         return TYPE;
      }

      @Override
      public String toString() {
         return "<empty>";
      }

      @Override
      public <T> Stream<T> resolve(final ContextMap context, final DisplayContentsFactory<T> factory) {
         return Stream.empty();
      }
   }

   record ItemSlotDisplay(Holder<Item> item) implements SlotDisplay {
      public static final MapCodec<SlotDisplay.ItemSlotDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(Item.CODEC.fieldOf("item").forGetter(SlotDisplay.ItemSlotDisplay::item)).apply(i, SlotDisplay.ItemSlotDisplay::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, SlotDisplay.ItemSlotDisplay> STREAM_CODEC = StreamCodec.composite(
         Item.STREAM_CODEC, SlotDisplay.ItemSlotDisplay::item, SlotDisplay.ItemSlotDisplay::new
      );
      public static final SlotDisplay.Type<SlotDisplay.ItemSlotDisplay> TYPE = new SlotDisplay.Type<>(MAP_CODEC, STREAM_CODEC);

      public ItemSlotDisplay(final Item item) {
         this(item.builtInRegistryHolder());
      }

      @Override
      public SlotDisplay.Type<SlotDisplay.ItemSlotDisplay> type() {
         return TYPE;
      }

      @Override
      public <T> Stream<T> resolve(final ContextMap context, final DisplayContentsFactory<T> factory) {
         return factory instanceof DisplayContentsFactory.ForStacks<T> stacks ? Stream.of(stacks.forStack(this.item)) : Stream.empty();
      }

      @Override
      public boolean isEnabled(final FeatureFlagSet enabledFeatures) {
         return this.item.value().isEnabled(enabledFeatures);
      }
   }

   class ItemStackContentsFactory implements DisplayContentsFactory.ForStacks<ItemStack> {
      public static final SlotDisplay.ItemStackContentsFactory INSTANCE = new SlotDisplay.ItemStackContentsFactory();

      public ItemStack forStack(final ItemStack stack) {
         return stack;
      }
   }

   record ItemStackSlotDisplay(ItemStackTemplate stack) implements SlotDisplay {
      public static final MapCodec<SlotDisplay.ItemStackSlotDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(ItemStackTemplate.CODEC.fieldOf("item").forGetter(SlotDisplay.ItemStackSlotDisplay::stack))
            .apply(i, SlotDisplay.ItemStackSlotDisplay::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, SlotDisplay.ItemStackSlotDisplay> STREAM_CODEC = StreamCodec.composite(
         ItemStackTemplate.STREAM_CODEC, SlotDisplay.ItemStackSlotDisplay::stack, SlotDisplay.ItemStackSlotDisplay::new
      );
      public static final SlotDisplay.Type<SlotDisplay.ItemStackSlotDisplay> TYPE = new SlotDisplay.Type<>(MAP_CODEC, STREAM_CODEC);

      @Override
      public SlotDisplay.Type<SlotDisplay.ItemStackSlotDisplay> type() {
         return TYPE;
      }

      @Override
      public <T> Stream<T> resolve(final ContextMap context, final DisplayContentsFactory<T> factory) {
         return factory instanceof DisplayContentsFactory.ForStacks<T> stacks ? Stream.of(stacks.forStack(this.stack.create())) : Stream.empty();
      }

      @Override
      public boolean isEnabled(final FeatureFlagSet enabledFeatures) {
         return this.stack.item().value().isEnabled(enabledFeatures);
      }
   }

   record OnlyWithComponent(SlotDisplay source, DataComponentType<?> component) implements SlotDisplay {
      public static final MapCodec<SlotDisplay.OnlyWithComponent> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               SlotDisplay.CODEC.fieldOf("contents").forGetter(SlotDisplay.OnlyWithComponent::source),
               DataComponentType.CODEC.fieldOf("component").forGetter(SlotDisplay.OnlyWithComponent::component)
            )
            .apply(i, SlotDisplay.OnlyWithComponent::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, SlotDisplay.OnlyWithComponent> STREAM_CODEC = StreamCodec.composite(
         SlotDisplay.STREAM_CODEC,
         SlotDisplay.OnlyWithComponent::source,
         DataComponentType.STREAM_CODEC,
         SlotDisplay.OnlyWithComponent::component,
         SlotDisplay.OnlyWithComponent::new
      );
      public static final SlotDisplay.Type<SlotDisplay.OnlyWithComponent> TYPE = new SlotDisplay.Type<>(MAP_CODEC, STREAM_CODEC);

      @Override
      public <T> Stream<T> resolve(final ContextMap context, final DisplayContentsFactory<T> builder) {
         return builder instanceof DisplayContentsFactory.ForStacks<T> stacks
            ? this.source.resolve(context, SlotDisplay.ItemStackContentsFactory.INSTANCE).filter(s -> s.has(this.component)).map(stacks::forStack)
            : Stream.empty();
      }

      @Override
      public SlotDisplay.Type<SlotDisplay.OnlyWithComponent> type() {
         return TYPE;
      }
   }

   record SmithingTrimDemoSlotDisplay(SlotDisplay base, SlotDisplay material, Holder<TrimPattern> pattern) implements SlotDisplay {
      public static final MapCodec<SlotDisplay.SmithingTrimDemoSlotDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               SlotDisplay.CODEC.fieldOf("base").forGetter(SlotDisplay.SmithingTrimDemoSlotDisplay::base),
               SlotDisplay.CODEC.fieldOf("material").forGetter(SlotDisplay.SmithingTrimDemoSlotDisplay::material),
               TrimPattern.CODEC.fieldOf("pattern").forGetter(SlotDisplay.SmithingTrimDemoSlotDisplay::pattern)
            )
            .apply(i, SlotDisplay.SmithingTrimDemoSlotDisplay::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, SlotDisplay.SmithingTrimDemoSlotDisplay> STREAM_CODEC = StreamCodec.composite(
         SlotDisplay.STREAM_CODEC,
         SlotDisplay.SmithingTrimDemoSlotDisplay::base,
         SlotDisplay.STREAM_CODEC,
         SlotDisplay.SmithingTrimDemoSlotDisplay::material,
         TrimPattern.STREAM_CODEC,
         SlotDisplay.SmithingTrimDemoSlotDisplay::pattern,
         SlotDisplay.SmithingTrimDemoSlotDisplay::new
      );
      public static final SlotDisplay.Type<SlotDisplay.SmithingTrimDemoSlotDisplay> TYPE = new SlotDisplay.Type<>(MAP_CODEC, STREAM_CODEC);

      @Override
      public SlotDisplay.Type<SlotDisplay.SmithingTrimDemoSlotDisplay> type() {
         return TYPE;
      }

      @Override
      public <T> Stream<T> resolve(final ContextMap context, final DisplayContentsFactory<T> factory) {
         RandomSource randomSource = RandomSource.createThreadLocalInstance(System.identityHashCode(this));
         BinaryOperator<ItemStack> transformation = (base, material) -> SmithingTrimRecipe.applyTrim(base, material, this.pattern);
         return SlotDisplay.applyDemoTransformation(context, factory, this.base, this.material, randomSource, transformation);
      }
   }

   record TagSlotDisplay(TagKey<Item> tag) implements SlotDisplay {
      public static final MapCodec<SlotDisplay.TagSlotDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(SlotDisplay.TagSlotDisplay::tag)).apply(i, SlotDisplay.TagSlotDisplay::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, SlotDisplay.TagSlotDisplay> STREAM_CODEC = StreamCodec.composite(
         TagKey.streamCodec(Registries.ITEM), SlotDisplay.TagSlotDisplay::tag, SlotDisplay.TagSlotDisplay::new
      );
      public static final SlotDisplay.Type<SlotDisplay.TagSlotDisplay> TYPE = new SlotDisplay.Type<>(MAP_CODEC, STREAM_CODEC);

      @Override
      public SlotDisplay.Type<SlotDisplay.TagSlotDisplay> type() {
         return TYPE;
      }

      @Override
      public <T> Stream<T> resolve(final ContextMap context, final DisplayContentsFactory<T> factory) {
         if (factory instanceof DisplayContentsFactory.ForStacks<T> stacks) {
            HolderLookup.Provider registries = context.getOptional(SlotDisplayContext.REGISTRIES);
            if (registries != null) {
               return registries.lookupOrThrow(Registries.ITEM).get(this.tag).map(t -> t.stream().map(stacks::forStack)).stream().flatMap(s -> s);
            }
         }

         return Stream.empty();
      }
   }

   record Type<T extends SlotDisplay>(MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
   }

   record WithAnyPotion(SlotDisplay display) implements SlotDisplay {
      public static final MapCodec<SlotDisplay.WithAnyPotion> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(SlotDisplay.CODEC.fieldOf("contents").forGetter(SlotDisplay.WithAnyPotion::display)).apply(i, SlotDisplay.WithAnyPotion::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, SlotDisplay.WithAnyPotion> STREAM_CODEC = StreamCodec.composite(
         SlotDisplay.STREAM_CODEC, SlotDisplay.WithAnyPotion::display, SlotDisplay.WithAnyPotion::new
      );
      public static final SlotDisplay.Type<SlotDisplay.WithAnyPotion> TYPE = new SlotDisplay.Type<>(MAP_CODEC, STREAM_CODEC);

      @Override
      public SlotDisplay.Type<SlotDisplay.WithAnyPotion> type() {
         return TYPE;
      }

      @Override
      public <T> Stream<T> resolve(final ContextMap context, final DisplayContentsFactory<T> factory) {
         if (factory instanceof DisplayContentsFactory.ForStacks<T> stacks) {
            List<ItemStack> displayItems = this.display.resolveForStacks(context);
            Optional<? extends HolderLookup.RegistryLookup<Potion>> potions = Optional.ofNullable(context.getOptional(SlotDisplayContext.REGISTRIES))
               .flatMap(r -> r.lookup(Registries.POTION));
            return potions.stream().flatMap(HolderLookup::listElements).flatMap(potion -> {
               PotionContents potionContents = new PotionContents(potion);
               return displayItems.stream().map(item -> {
                  ItemStack itemCopy = item.copy();
                  itemCopy.set(DataComponents.POTION_CONTENTS, potionContents);
                  return stacks.forStack(itemCopy);
               });
            });
         } else {
            return Stream.empty();
         }
      }
   }

   record WithRemainder(SlotDisplay input, SlotDisplay remainder) implements SlotDisplay {
      public static final MapCodec<SlotDisplay.WithRemainder> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               SlotDisplay.CODEC.fieldOf("input").forGetter(SlotDisplay.WithRemainder::input),
               SlotDisplay.CODEC.fieldOf("remainder").forGetter(SlotDisplay.WithRemainder::remainder)
            )
            .apply(i, SlotDisplay.WithRemainder::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, SlotDisplay.WithRemainder> STREAM_CODEC = StreamCodec.composite(
         SlotDisplay.STREAM_CODEC,
         SlotDisplay.WithRemainder::input,
         SlotDisplay.STREAM_CODEC,
         SlotDisplay.WithRemainder::remainder,
         SlotDisplay.WithRemainder::new
      );
      public static final SlotDisplay.Type<SlotDisplay.WithRemainder> TYPE = new SlotDisplay.Type<>(MAP_CODEC, STREAM_CODEC);

      @Override
      public SlotDisplay.Type<SlotDisplay.WithRemainder> type() {
         return TYPE;
      }

      @Override
      public <T> Stream<T> resolve(final ContextMap context, final DisplayContentsFactory<T> factory) {
         if (factory instanceof DisplayContentsFactory.ForRemainders<T> remainders) {
            List<T> resolvedRemainders = this.remainder.resolve(context, factory).toList();
            return this.input.resolve(context, factory).map(input -> remainders.addRemainder((T)input, resolvedRemainders));
         } else {
            return this.input.resolve(context, factory);
         }
      }

      @Override
      public boolean isEnabled(final FeatureFlagSet enabledFeatures) {
         return this.input.isEnabled(enabledFeatures) && this.remainder.isEnabled(enabledFeatures);
      }
   }
}
