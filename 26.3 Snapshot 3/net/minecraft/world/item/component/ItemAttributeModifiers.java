package net.minecraft.world.item.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.apache.commons.lang3.function.TriConsumer;
import org.jspecify.annotations.Nullable;

public record ItemAttributeModifiers(List<ItemAttributeModifiers.Entry> modifiers) {
   public static final ItemAttributeModifiers EMPTY = new ItemAttributeModifiers(List.of());
   public static final Codec<ItemAttributeModifiers> CODEC = ItemAttributeModifiers.Entry.CODEC
      .listOf()
      .xmap(ItemAttributeModifiers::new, ItemAttributeModifiers::modifiers);
   public static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers> STREAM_CODEC = StreamCodec.composite(
      ItemAttributeModifiers.Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), ItemAttributeModifiers::modifiers, ItemAttributeModifiers::new
   );
   public static final DecimalFormat ATTRIBUTE_MODIFIER_FORMAT = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

   public static ItemAttributeModifiers.Builder builder() {
      return new ItemAttributeModifiers.Builder();
   }

   public ItemAttributeModifiers withModifierAdded(final Holder<Attribute> attribute, final AttributeModifier modifier, final EquipmentSlotGroup slot) {
      com.google.common.collect.ImmutableList.Builder<ItemAttributeModifiers.Entry> newModifiers = ImmutableList.builderWithExpectedSize(
         this.modifiers.size() + 1
      );

      for (ItemAttributeModifiers.Entry entry : this.modifiers) {
         if (!entry.matches(attribute, modifier.id())) {
            newModifiers.add(entry);
         }
      }

      newModifiers.add(new ItemAttributeModifiers.Entry(attribute, modifier, slot));
      return new ItemAttributeModifiers(newModifiers.build());
   }

   public void forEach(final EquipmentSlotGroup slot, final TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> consumer) {
      for (ItemAttributeModifiers.Entry entry : this.modifiers) {
         if (entry.slot.equals(slot)) {
            consumer.accept(entry.attribute, entry.modifier, entry.display);
         }
      }
   }

   public void forEach(final EquipmentSlotGroup slot, final BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
      for (ItemAttributeModifiers.Entry entry : this.modifiers) {
         if (entry.slot.equals(slot)) {
            consumer.accept(entry.attribute, entry.modifier);
         }
      }
   }

   public void forEach(final EquipmentSlot slot, final BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
      for (ItemAttributeModifiers.Entry entry : this.modifiers) {
         if (entry.slot.test(slot)) {
            consumer.accept(entry.attribute, entry.modifier);
         }
      }
   }

   public double compute(final Holder<Attribute> attribute, final double baseValue, final EquipmentSlot slot) {
      double value = baseValue;

      for (ItemAttributeModifiers.Entry entry : this.modifiers) {
         if (entry.slot.test(slot) && entry.attribute == attribute) {
            double amount = entry.modifier.amount();

            value += switch (entry.modifier.operation()) {
               case ADD_VALUE -> amount;
               case ADD_MULTIPLIED_BASE -> amount * baseValue;
               case ADD_MULTIPLIED_TOTAL -> amount * value;
            };
         }
      }

      return value;
   }

   public static class Builder {
      private final com.google.common.collect.ImmutableList.Builder<ItemAttributeModifiers.Entry> entries = ImmutableList.builder();

      private Builder() {
      }

      public ItemAttributeModifiers.Builder add(final Holder<Attribute> attribute, final AttributeModifier modifier, final EquipmentSlotGroup slot) {
         this.entries.add(new ItemAttributeModifiers.Entry(attribute, modifier, slot));
         return this;
      }

      public ItemAttributeModifiers.Builder add(
         final Holder<Attribute> attribute, final AttributeModifier modifier, final EquipmentSlotGroup slot, final ItemAttributeModifiers.Display display
      ) {
         this.entries.add(new ItemAttributeModifiers.Entry(attribute, modifier, slot, display));
         return this;
      }

      public ItemAttributeModifiers build() {
         return new ItemAttributeModifiers(this.entries.build());
      }
   }

   public interface Display {
      Codec<ItemAttributeModifiers.Display> CODEC = ItemAttributeModifiers.Display.Type.CODEC
         .dispatch("type", ItemAttributeModifiers.Display::type, type -> type.codec);
      StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Display> STREAM_CODEC = ItemAttributeModifiers.Display.Type.STREAM_CODEC
         .<RegistryFriendlyByteBuf>cast()
         .dispatch(ItemAttributeModifiers.Display::type, ItemAttributeModifiers.Display.Type::streamCodec);

      static ItemAttributeModifiers.Display attributeModifiers() {
         return ItemAttributeModifiers.Display.Default.INSTANCE;
      }

      static ItemAttributeModifiers.Display hidden() {
         return ItemAttributeModifiers.Display.Hidden.INSTANCE;
      }

      static ItemAttributeModifiers.Display override(final Component component) {
         return new ItemAttributeModifiers.Display.OverrideText(component);
      }

      ItemAttributeModifiers.Display.Type type();

      void apply(Consumer<Component> consumer, @Nullable Player player, Holder<Attribute> attribute, AttributeModifier modifier);

      record Default() implements ItemAttributeModifiers.Display {
         private static final ItemAttributeModifiers.Display.Default INSTANCE = new ItemAttributeModifiers.Display.Default();
         private static final MapCodec<ItemAttributeModifiers.Display.Default> CODEC = MapCodec.unit(INSTANCE);
         private static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Display.Default> STREAM_CODEC = StreamCodec.unit(INSTANCE);

         @Override
         public ItemAttributeModifiers.Display.Type type() {
            return ItemAttributeModifiers.Display.Type.DEFAULT;
         }

         @Override
         public void apply(
            final Consumer<Component> consumer, final @Nullable Player player, final Holder<Attribute> attribute, final AttributeModifier modifier
         ) {
            double amount = modifier.amount();
            boolean displayWithBase = false;
            if (player != null) {
               if (modifier.is(Item.BASE_ATTACK_DAMAGE_ID)) {
                  amount += player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
                  displayWithBase = true;
               } else if (modifier.is(Item.BASE_ATTACK_SPEED_ID)) {
                  amount += player.getAttributeBaseValue(Attributes.ATTACK_SPEED);
                  displayWithBase = true;
               }
            }

            double displayAmount;
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE
               || modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
               displayAmount = amount * 100.0;
            } else if (attribute.is(Attributes.KNOCKBACK_RESISTANCE)) {
               displayAmount = amount * 10.0;
            } else {
               displayAmount = amount;
            }

            if (displayWithBase) {
               consumer.accept(
                  CommonComponents.space()
                     .append(
                        Component.translatable(
                           "attribute.modifier.equals." + modifier.operation().id(),
                           ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(displayAmount),
                           Component.translatable(attribute.value().getDescriptionId())
                        )
                     )
                     .withStyle(ChatFormatting.DARK_GREEN)
               );
            } else if (amount > 0.0) {
               consumer.accept(
                  Component.translatable(
                        "attribute.modifier.plus." + modifier.operation().id(),
                        ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(displayAmount),
                        Component.translatable(attribute.value().getDescriptionId())
                     )
                     .withStyle(attribute.value().getStyle(true))
               );
            } else if (amount < 0.0) {
               consumer.accept(
                  Component.translatable(
                        "attribute.modifier.take." + modifier.operation().id(),
                        ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(-displayAmount),
                        Component.translatable(attribute.value().getDescriptionId())
                     )
                     .withStyle(attribute.value().getStyle(false))
               );
            }
         }
      }

      record Hidden() implements ItemAttributeModifiers.Display {
         private static final ItemAttributeModifiers.Display.Hidden INSTANCE = new ItemAttributeModifiers.Display.Hidden();
         private static final MapCodec<ItemAttributeModifiers.Display.Hidden> CODEC = MapCodec.unit(INSTANCE);
         private static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Display.Hidden> STREAM_CODEC = StreamCodec.unit(INSTANCE);

         @Override
         public ItemAttributeModifiers.Display.Type type() {
            return ItemAttributeModifiers.Display.Type.HIDDEN;
         }

         @Override
         public void apply(
            final Consumer<Component> consumer, final @Nullable Player player, final Holder<Attribute> attribute, final AttributeModifier modifier
         ) {
         }
      }

      record OverrideText(Component component) implements ItemAttributeModifiers.Display {
         private static final MapCodec<ItemAttributeModifiers.Display.OverrideText> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(ComponentSerialization.CODEC.fieldOf("value").forGetter(ItemAttributeModifiers.Display.OverrideText::component))
               .apply(i, ItemAttributeModifiers.Display.OverrideText::new)
         );
         private static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Display.OverrideText> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC, ItemAttributeModifiers.Display.OverrideText::component, ItemAttributeModifiers.Display.OverrideText::new
         );

         @Override
         public ItemAttributeModifiers.Display.Type type() {
            return ItemAttributeModifiers.Display.Type.OVERRIDE;
         }

         @Override
         public void apply(
            final Consumer<Component> consumer, final @Nullable Player player, final Holder<Attribute> attribute, final AttributeModifier modifier
         ) {
            consumer.accept(this.component);
         }
      }

      enum Type implements StringRepresentable {
         DEFAULT("default", 0, ItemAttributeModifiers.Display.Default.CODEC, ItemAttributeModifiers.Display.Default.STREAM_CODEC),
         HIDDEN("hidden", 1, ItemAttributeModifiers.Display.Hidden.CODEC, ItemAttributeModifiers.Display.Hidden.STREAM_CODEC),
         OVERRIDE("override", 2, ItemAttributeModifiers.Display.OverrideText.CODEC, ItemAttributeModifiers.Display.OverrideText.STREAM_CODEC);

         private static final Codec<ItemAttributeModifiers.Display.Type> CODEC = StringRepresentable.fromEnum(ItemAttributeModifiers.Display.Type::values);
         private static final IntFunction<ItemAttributeModifiers.Display.Type> BY_ID = ByIdMap.continuous(
            ItemAttributeModifiers.Display.Type::id, values(), ByIdMap.OutOfBoundsStrategy.ZERO
         );
         private static final StreamCodec<ByteBuf, ItemAttributeModifiers.Display.Type> STREAM_CODEC = ByteBufCodecs.idMapper(
            BY_ID, ItemAttributeModifiers.Display.Type::id
         );
         private final String name;
         private final int id;
         private final MapCodec<? extends ItemAttributeModifiers.Display> codec;
         private final StreamCodec<RegistryFriendlyByteBuf, ? extends ItemAttributeModifiers.Display> streamCodec;

         Type(
            final String name,
            final int id,
            final MapCodec<? extends ItemAttributeModifiers.Display> codec,
            final StreamCodec<RegistryFriendlyByteBuf, ? extends ItemAttributeModifiers.Display> streamCodec
         ) {
            this.name = name;
            this.id = id;
            this.codec = codec;
            this.streamCodec = streamCodec;
         }

         @Override
         public String getSerializedName() {
            return this.name;
         }

         private int id() {
            return this.id;
         }

         private StreamCodec<RegistryFriendlyByteBuf, ? extends ItemAttributeModifiers.Display> streamCodec() {
            return this.streamCodec;
         }
      }
   }

   public record Entry(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot, ItemAttributeModifiers.Display display) {
      public static final Codec<ItemAttributeModifiers.Entry> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Attribute.CODEC.fieldOf("type").forGetter(ItemAttributeModifiers.Entry::attribute),
               AttributeModifier.MAP_CODEC.forGetter(ItemAttributeModifiers.Entry::modifier),
               EquipmentSlotGroup.CODEC.optionalFieldOf("slot", EquipmentSlotGroup.ANY).forGetter(ItemAttributeModifiers.Entry::slot),
               ItemAttributeModifiers.Display.CODEC
                  .optionalFieldOf("display", ItemAttributeModifiers.Display.Default.INSTANCE)
                  .forGetter(ItemAttributeModifiers.Entry::display)
            )
            .apply(i, ItemAttributeModifiers.Entry::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Entry> STREAM_CODEC = StreamCodec.composite(
         Attribute.STREAM_CODEC,
         ItemAttributeModifiers.Entry::attribute,
         AttributeModifier.STREAM_CODEC,
         ItemAttributeModifiers.Entry::modifier,
         EquipmentSlotGroup.STREAM_CODEC,
         ItemAttributeModifiers.Entry::slot,
         ItemAttributeModifiers.Display.STREAM_CODEC,
         ItemAttributeModifiers.Entry::display,
         ItemAttributeModifiers.Entry::new
      );

      public Entry(final Holder<Attribute> attribute, final AttributeModifier modifier, final EquipmentSlotGroup slot) {
         this(attribute, modifier, slot, ItemAttributeModifiers.Display.attributeModifiers());
      }

      public boolean matches(final Holder<Attribute> attribute, final Identifier id) {
         return attribute.equals(this.attribute) && this.modifier.is(id);
      }
   }
}
