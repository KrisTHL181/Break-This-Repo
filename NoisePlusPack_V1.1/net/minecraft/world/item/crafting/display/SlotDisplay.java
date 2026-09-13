package net.minecraft.world.item.crafting.display;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.block.entity.FuelValues;

public interface SlotDisplay {
   Codec<SlotDisplay> CODEC = BuiltInRegistries.SLOT_DISPLAY.byNameCodec().dispatch(SlotDisplay::type, SlotDisplay.Type::codec);
   StreamCodec<RegistryFriendlyByteBuf, SlotDisplay> STREAM_CODEC = ByteBufCodecs.registry(Registries.SLOT_DISPLAY)
      .dispatch(SlotDisplay::type, SlotDisplay.Type::streamCodec);

   <T> Stream<T> resolve(ContextMap var1, DisplayContentsFactory<T> var2);

   SlotDisplay.Type<? extends SlotDisplay> type();

   default boolean isEnabled(FeatureFlagSet p_363351_) {
      return true;
   }

   default List<ItemStack> resolveForStacks(ContextMap p_365710_) {
      return this.resolve(p_365710_, SlotDisplay.ItemStackContentsFactory.INSTANCE).toList();
   }

   default ItemStack resolveForFirstStack(ContextMap p_367736_) {
      return this.resolve(p_367736_, SlotDisplay.ItemStackContentsFactory.INSTANCE).findFirst().orElse(ItemStack.EMPTY);
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
      public <T> Stream<T> resolve(ContextMap p_364752_, DisplayContentsFactory<T> p_363029_) {
         if (p_363029_ instanceof DisplayContentsFactory.ForStacks<T> forstacks) {
            FuelValues fuelvalues = p_364752_.getOptional(SlotDisplayContext.FUEL_VALUES);
            if (fuelvalues != null) {
               return fuelvalues.fuelItems().stream().map(forstacks::forStack);
            }
         }

         return Stream.empty();
      }
   }

   record Composite(List<SlotDisplay> contents) implements SlotDisplay {
      public static final MapCodec<SlotDisplay.Composite> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_361407_ -> p_361407_.group(SlotDisplay.CODEC.listOf().fieldOf("contents").forGetter(SlotDisplay.Composite::contents))
            .apply(p_361407_, SlotDisplay.Composite::new)
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
      public <T> Stream<T> resolve(ContextMap p_367693_, DisplayContentsFactory<T> p_361280_) {
         return this.contents.stream().flatMap(p_369601_ -> p_369601_.resolve(p_367693_, p_361280_));
      }

      @Override
      public boolean isEnabled(FeatureFlagSet p_365716_) {
         return this.contents.stream().allMatch(p_367156_ -> p_367156_.isEnabled(p_365716_));
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
      public <T> Stream<T> resolve(ContextMap p_369517_, DisplayContentsFactory<T> p_367228_) {
         return Stream.empty();
      }
   }

   record ItemSlotDisplay(Holder<Item> item) implements SlotDisplay {
      public static final MapCodec<SlotDisplay.ItemSlotDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_363552_ -> p_363552_.group(Item.CODEC.fieldOf("item").forGetter(SlotDisplay.ItemSlotDisplay::item))
            .apply(p_363552_, SlotDisplay.ItemSlotDisplay::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, SlotDisplay.ItemSlotDisplay> STREAM_CODEC = StreamCodec.composite(
         Item.STREAM_CODEC, SlotDisplay.ItemSlotDisplay::item, SlotDisplay.ItemSlotDisplay::new
      );
      public static final SlotDisplay.Type<SlotDisplay.ItemSlotDisplay> TYPE = new SlotDisplay.Type<>(MAP_CODEC, STREAM_CODEC);

      public ItemSlotDisplay(Item p_369458_) {
         this(p_369458_.builtInRegistryHolder());
      }

      @Override
      public SlotDisplay.Type<SlotDisplay.ItemSlotDisplay> type() {
         return TYPE;
      }

      @Override
      public <T> Stream<T> resolve(ContextMap p_366441_, DisplayContentsFactory<T> p_362595_) {
         return p_362595_ instanceof DisplayContentsFactory.ForStacks<T> forstacks ? Stream.of(forstacks.forStack(this.item)) : Stream.empty();
      }

      @Override
      public boolean isEnabled(FeatureFlagSet p_363139_) {
         return this.item.value().isEnabled(p_363139_);
      }
   }

   class ItemStackContentsFactory implements DisplayContentsFactory.ForStacks<ItemStack> {
      public static final SlotDisplay.ItemStackContentsFactory INSTANCE = new SlotDisplay.ItemStackContentsFactory();

      public ItemStack forStack(ItemStack p_361456_) {
         return p_361456_;
      }
   }

   record ItemStackSlotDisplay(ItemStack stack) implements SlotDisplay {
      public static final MapCodec<SlotDisplay.ItemStackSlotDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_366145_ -> p_366145_.group(ItemStack.STRICT_CODEC.fieldOf("item").forGetter(SlotDisplay.ItemStackSlotDisplay::stack))
            .apply(p_366145_, SlotDisplay.ItemStackSlotDisplay::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, SlotDisplay.ItemStackSlotDisplay> STREAM_CODEC = StreamCodec.composite(
         ItemStack.STREAM_CODEC, SlotDisplay.ItemStackSlotDisplay::stack, SlotDisplay.ItemStackSlotDisplay::new
      );
      public static final SlotDisplay.Type<SlotDisplay.ItemStackSlotDisplay> TYPE = new SlotDisplay.Type<>(MAP_CODEC, STREAM_CODEC);

      @Override
      public SlotDisplay.Type<SlotDisplay.ItemStackSlotDisplay> type() {
         return TYPE;
      }

      @Override
      public <T> Stream<T> resolve(ContextMap p_365106_, DisplayContentsFactory<T> p_368232_) {
         return p_368232_ instanceof DisplayContentsFactory.ForStacks<T> forstacks ? Stream.of(forstacks.forStack(this.stack)) : Stream.empty();
      }

      @Override
      public boolean equals(Object p_366031_) {
         return this == p_366031_
            || p_366031_ instanceof SlotDisplay.ItemStackSlotDisplay slotdisplay$itemstackslotdisplay
               && ItemStack.matches(this.stack, slotdisplay$itemstackslotdisplay.stack);
      }

      @Override
      public boolean isEnabled(FeatureFlagSet p_370071_) {
         return this.stack.getItem().isEnabled(p_370071_);
      }
   }

   record SmithingTrimDemoSlotDisplay(SlotDisplay base, SlotDisplay material, Holder<TrimPattern> pattern) implements SlotDisplay {
      public static final MapCodec<SlotDisplay.SmithingTrimDemoSlotDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_390842_ -> p_390842_.group(
               SlotDisplay.CODEC.fieldOf("base").forGetter(SlotDisplay.SmithingTrimDemoSlotDisplay::base),
               SlotDisplay.CODEC.fieldOf("material").forGetter(SlotDisplay.SmithingTrimDemoSlotDisplay::material),
               TrimPattern.CODEC.fieldOf("pattern").forGetter(SlotDisplay.SmithingTrimDemoSlotDisplay::pattern)
            )
            .apply(p_390842_, SlotDisplay.SmithingTrimDemoSlotDisplay::new)
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
      public <T> Stream<T> resolve(ContextMap p_360959_, DisplayContentsFactory<T> p_368141_) {
         if (p_368141_ instanceof DisplayContentsFactory.ForStacks<T> forstacks) {
            HolderLookup.Provider holderlookup$provider = p_360959_.getOptional(SlotDisplayContext.REGISTRIES);
            if (holderlookup$provider != null) {
               RandomSource randomsource = RandomSource.create(System.identityHashCode(this));
               List<ItemStack> list = this.base.resolveForStacks(p_360959_);
               if (list.isEmpty()) {
                  return Stream.empty();
               }

               List<ItemStack> list1 = this.material.resolveForStacks(p_360959_);
               if (list1.isEmpty()) {
                  return Stream.empty();
               }

               return Stream.<ItemStack>generate(() -> {
                  ItemStack itemstack = Util.getRandom(list, randomsource);
                  ItemStack itemstack1 = Util.getRandom(list1, randomsource);
                  return SmithingTrimRecipe.applyTrim(holderlookup$provider, itemstack, itemstack1, this.pattern);
               }).limit(256L).filter(p_369984_ -> !p_369984_.isEmpty()).limit(16L).map(forstacks::forStack);
            }
         }

         return Stream.empty();
      }
   }

   record TagSlotDisplay(TagKey<Item> tag) implements SlotDisplay {
      public static final MapCodec<SlotDisplay.TagSlotDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_369789_ -> p_369789_.group(TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(SlotDisplay.TagSlotDisplay::tag))
            .apply(p_369789_, SlotDisplay.TagSlotDisplay::new)
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
      public <T> Stream<T> resolve(ContextMap p_363468_, DisplayContentsFactory<T> p_369163_) {
         if (p_369163_ instanceof DisplayContentsFactory.ForStacks<T> forstacks) {
            HolderLookup.Provider holderlookup$provider = p_363468_.getOptional(SlotDisplayContext.REGISTRIES);
            if (holderlookup$provider != null) {
               return holderlookup$provider.lookupOrThrow(Registries.ITEM)
                  .get(this.tag)
                  .map(p_364699_ -> p_364699_.stream().map(forstacks::forStack))
                  .stream()
                  .flatMap(p_367543_ -> p_367543_);
            }
         }

         return Stream.empty();
      }
   }

   record Type<T extends SlotDisplay>(MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
   }

   record WithRemainder(SlotDisplay input, SlotDisplay remainder) implements SlotDisplay {
      public static final MapCodec<SlotDisplay.WithRemainder> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_364390_ -> p_364390_.group(
               SlotDisplay.CODEC.fieldOf("input").forGetter(SlotDisplay.WithRemainder::input),
               SlotDisplay.CODEC.fieldOf("remainder").forGetter(SlotDisplay.WithRemainder::remainder)
            )
            .apply(p_364390_, SlotDisplay.WithRemainder::new)
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
      public <T> Stream<T> resolve(ContextMap p_364782_, DisplayContentsFactory<T> p_360890_) {
         if (p_360890_ instanceof DisplayContentsFactory.ForRemainders<T> forremainders) {
            List<T> list = this.remainder.resolve(p_364782_, p_360890_).toList();
            return this.input.resolve(p_364782_, p_360890_).map(p_361305_ -> forremainders.addRemainder((T)p_361305_, list));
         } else {
            return this.input.resolve(p_364782_, p_360890_);
         }
      }

      @Override
      public boolean isEnabled(FeatureFlagSet p_366883_) {
         return this.input.isEnabled(p_366883_) && this.remainder.isEnabled(p_366883_);
      }
   }
}
