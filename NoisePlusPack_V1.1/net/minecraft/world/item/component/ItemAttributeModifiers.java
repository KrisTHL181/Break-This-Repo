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

   public ItemAttributeModifiers withModifierAdded(Holder<Attribute> p_335092_, AttributeModifier p_327974_, EquipmentSlotGroup p_328449_) {
      com.google.common.collect.ImmutableList.Builder<ItemAttributeModifiers.Entry> builder = ImmutableList.builderWithExpectedSize(this.modifiers.size() + 1);

      for (ItemAttributeModifiers.Entry itemattributemodifiers$entry : this.modifiers) {
         if (!itemattributemodifiers$entry.matches(p_335092_, p_327974_.id())) {
            builder.add(itemattributemodifiers$entry);
         }
      }

      builder.add(new ItemAttributeModifiers.Entry(p_335092_, p_327974_, p_328449_));
      return new ItemAttributeModifiers(builder.build());
   }

   public void forEach(EquipmentSlotGroup p_408452_, TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> p_408487_) {
      for (ItemAttributeModifiers.Entry itemattributemodifiers$entry : this.modifiers) {
         if (itemattributemodifiers$entry.slot.equals(p_408452_)) {
            p_408487_.accept(itemattributemodifiers$entry.attribute, itemattributemodifiers$entry.modifier, itemattributemodifiers$entry.display);
         }
      }
   }

   public void forEach(EquipmentSlotGroup p_343586_, BiConsumer<Holder<Attribute>, AttributeModifier> p_344914_) {
      for (ItemAttributeModifiers.Entry itemattributemodifiers$entry : this.modifiers) {
         if (itemattributemodifiers$entry.slot.equals(p_343586_)) {
            p_344914_.accept(itemattributemodifiers$entry.attribute, itemattributemodifiers$entry.modifier);
         }
      }
   }

   public void forEach(EquipmentSlot p_334753_, BiConsumer<Holder<Attribute>, AttributeModifier> p_331767_) {
      for (ItemAttributeModifiers.Entry itemattributemodifiers$entry : this.modifiers) {
         if (itemattributemodifiers$entry.slot.test(p_334753_)) {
            p_331767_.accept(itemattributemodifiers$entry.attribute, itemattributemodifiers$entry.modifier);
         }
      }
   }

   public double compute(Holder<Attribute> p_458939_, double p_332865_, EquipmentSlot p_329615_) {
      double d0 = p_332865_;

      for (ItemAttributeModifiers.Entry itemattributemodifiers$entry : this.modifiers) {
         if (itemattributemodifiers$entry.slot.test(p_329615_) && itemattributemodifiers$entry.attribute == p_458939_) {
            double d1 = itemattributemodifiers$entry.modifier.amount();

            d0 += switch (itemattributemodifiers$entry.modifier.operation()) {
               case ADD_VALUE -> d1;
               case ADD_MULTIPLIED_BASE -> d1 * p_332865_;
               case ADD_MULTIPLIED_TOTAL -> d1 * d0;
            };
         }
      }

      return d0;
   }

   public static class Builder {
      private final com.google.common.collect.ImmutableList.Builder<ItemAttributeModifiers.Entry> entries = ImmutableList.builder();

      Builder() {
      }

      public ItemAttributeModifiers.Builder add(Holder<Attribute> p_330104_, AttributeModifier p_333549_, EquipmentSlotGroup p_332621_) {
         this.entries.add(new ItemAttributeModifiers.Entry(p_330104_, p_333549_, p_332621_));
         return this;
      }

      public ItemAttributeModifiers.Builder add(
         Holder<Attribute> p_408753_, AttributeModifier p_408047_, EquipmentSlotGroup p_410456_, ItemAttributeModifiers.Display p_405978_
      ) {
         this.entries.add(new ItemAttributeModifiers.Entry(p_408753_, p_408047_, p_410456_, p_405978_));
         return this;
      }

      public ItemAttributeModifiers build() {
         return new ItemAttributeModifiers(this.entries.build());
      }
   }

   public interface Display {
      Codec<ItemAttributeModifiers.Display> CODEC = ItemAttributeModifiers.Display.Type.CODEC
         .dispatch("type", ItemAttributeModifiers.Display::type, p_409403_ -> p_409403_.codec);
      StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Display> STREAM_CODEC = ItemAttributeModifiers.Display.Type.STREAM_CODEC
         .<RegistryFriendlyByteBuf>cast()
         .dispatch(ItemAttributeModifiers.Display::type, ItemAttributeModifiers.Display.Type::streamCodec);

      static ItemAttributeModifiers.Display attributeModifiers() {
         return ItemAttributeModifiers.Display.Default.INSTANCE;
      }

      static ItemAttributeModifiers.Display hidden() {
         return ItemAttributeModifiers.Display.Hidden.INSTANCE;
      }

      static ItemAttributeModifiers.Display override(Component p_410632_) {
         return new ItemAttributeModifiers.Display.OverrideText(p_410632_);
      }

      ItemAttributeModifiers.Display.Type type();

      void apply(Consumer<Component> var1, @Nullable Player var2, Holder<Attribute> var3, AttributeModifier var4);

      record Default() implements ItemAttributeModifiers.Display {
         static final ItemAttributeModifiers.Display.Default INSTANCE = new ItemAttributeModifiers.Display.Default();
         static final MapCodec<ItemAttributeModifiers.Display.Default> CODEC = MapCodec.unit(INSTANCE);
         static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Display.Default> STREAM_CODEC = StreamCodec.unit(INSTANCE);

         @Override
         public ItemAttributeModifiers.Display.Type type() {
            return ItemAttributeModifiers.Display.Type.DEFAULT;
         }

         @Override
         public void apply(Consumer<Component> p_406087_, @Nullable Player p_409823_, Holder<Attribute> p_408648_, AttributeModifier p_406254_) {
            double d0 = p_406254_.amount();
            boolean flag = false;
            if (p_409823_ != null) {
               if (p_406254_.is(Item.BASE_ATTACK_DAMAGE_ID)) {
                  d0 += p_409823_.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
                  flag = true;
               } else if (p_406254_.is(Item.BASE_ATTACK_SPEED_ID)) {
                  d0 += p_409823_.getAttributeBaseValue(Attributes.ATTACK_SPEED);
                  flag = true;
               }
            }

            double d1;
            if (p_406254_.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE
               || p_406254_.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
               d1 = d0 * 100.0;
            } else if (p_408648_.is(Attributes.KNOCKBACK_RESISTANCE)) {
               d1 = d0 * 10.0;
            } else {
               d1 = d0;
            }

            if (flag) {
               p_406087_.accept(
                  CommonComponents.space()
                     .append(
                        Component.translatable(
                           "attribute.modifier.equals." + p_406254_.operation().id(),
                           ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(d1),
                           Component.translatable(p_408648_.value().getDescriptionId())
                        )
                     )
                     .withStyle(ChatFormatting.DARK_GREEN)
               );
            } else if (d0 > 0.0) {
               p_406087_.accept(
                  Component.translatable(
                        "attribute.modifier.plus." + p_406254_.operation().id(),
                        ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(d1),
                        Component.translatable(p_408648_.value().getDescriptionId())
                     )
                     .withStyle(p_408648_.value().getStyle(true))
               );
            } else if (d0 < 0.0) {
               p_406087_.accept(
                  Component.translatable(
                        "attribute.modifier.take." + p_406254_.operation().id(),
                        ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(-d1),
                        Component.translatable(p_408648_.value().getDescriptionId())
                     )
                     .withStyle(p_408648_.value().getStyle(false))
               );
            }
         }
      }

      record Hidden() implements ItemAttributeModifiers.Display {
         static final ItemAttributeModifiers.Display.Hidden INSTANCE = new ItemAttributeModifiers.Display.Hidden();
         static final MapCodec<ItemAttributeModifiers.Display.Hidden> CODEC = MapCodec.unit(INSTANCE);
         static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Display.Hidden> STREAM_CODEC = StreamCodec.unit(INSTANCE);

         @Override
         public ItemAttributeModifiers.Display.Type type() {
            return ItemAttributeModifiers.Display.Type.HIDDEN;
         }

         @Override
         public void apply(Consumer<Component> p_407940_, @Nullable Player p_406085_, Holder<Attribute> p_408160_, AttributeModifier p_407914_) {
         }
      }

      record OverrideText(Component component) implements ItemAttributeModifiers.Display {
         static final MapCodec<ItemAttributeModifiers.Display.OverrideText> CODEC = RecordCodecBuilder.mapCodec(
            p_408964_ -> p_408964_.group(ComponentSerialization.CODEC.fieldOf("value").forGetter(ItemAttributeModifiers.Display.OverrideText::component))
               .apply(p_408964_, ItemAttributeModifiers.Display.OverrideText::new)
         );
         static final StreamCodec<RegistryFriendlyByteBuf, ItemAttributeModifiers.Display.OverrideText> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC, ItemAttributeModifiers.Display.OverrideText::component, ItemAttributeModifiers.Display.OverrideText::new
         );

         @Override
         public ItemAttributeModifiers.Display.Type type() {
            return ItemAttributeModifiers.Display.Type.OVERRIDE;
         }

         @Override
         public void apply(Consumer<Component> p_408852_, @Nullable Player p_406427_, Holder<Attribute> p_407789_, AttributeModifier p_410336_) {
            p_408852_.accept(this.component);
         }
      }

      enum Type implements StringRepresentable {
         DEFAULT("default", 0, ItemAttributeModifiers.Display.Default.CODEC, ItemAttributeModifiers.Display.Default.STREAM_CODEC),
         HIDDEN("hidden", 1, ItemAttributeModifiers.Display.Hidden.CODEC, ItemAttributeModifiers.Display.Hidden.STREAM_CODEC),
         OVERRIDE("override", 2, ItemAttributeModifiers.Display.OverrideText.CODEC, ItemAttributeModifiers.Display.OverrideText.STREAM_CODEC);

         static final Codec<ItemAttributeModifiers.Display.Type> CODEC = StringRepresentable.fromEnum(ItemAttributeModifiers.Display.Type::values);
         private static final IntFunction<ItemAttributeModifiers.Display.Type> BY_ID = ByIdMap.continuous(
            ItemAttributeModifiers.Display.Type::id, values(), ByIdMap.OutOfBoundsStrategy.ZERO
         );
         static final StreamCodec<ByteBuf, ItemAttributeModifiers.Display.Type> STREAM_CODEC = ByteBufCodecs.idMapper(
            BY_ID, ItemAttributeModifiers.Display.Type::id
         );
         private final String name;
         private final int id;
         final MapCodec<? extends ItemAttributeModifiers.Display> codec;
         private final StreamCodec<RegistryFriendlyByteBuf, ? extends ItemAttributeModifiers.Display> streamCodec;

         Type(
            final String p_408456_,
            final int p_408404_,
            final MapCodec<? extends ItemAttributeModifiers.Display> p_409375_,
            final StreamCodec<RegistryFriendlyByteBuf, ? extends ItemAttributeModifiers.Display> p_406767_
         ) {
            this.name = p_408456_;
            this.id = p_408404_;
            this.codec = p_409375_;
            this.streamCodec = p_406767_;
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
         p_405642_ -> p_405642_.group(
               Attribute.CODEC.fieldOf("type").forGetter(ItemAttributeModifiers.Entry::attribute),
               AttributeModifier.MAP_CODEC.forGetter(ItemAttributeModifiers.Entry::modifier),
               EquipmentSlotGroup.CODEC.optionalFieldOf("slot", EquipmentSlotGroup.ANY).forGetter(ItemAttributeModifiers.Entry::slot),
               ItemAttributeModifiers.Display.CODEC
                  .optionalFieldOf("display", ItemAttributeModifiers.Display.Default.INSTANCE)
                  .forGetter(ItemAttributeModifiers.Entry::display)
            )
            .apply(p_405642_, ItemAttributeModifiers.Entry::new)
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

      public Entry(Holder<Attribute> p_330352_, AttributeModifier p_330812_, EquipmentSlotGroup p_329718_) {
         this(p_330352_, p_330812_, p_329718_, ItemAttributeModifiers.Display.attributeModifiers());
      }

      public boolean matches(Holder<Attribute> p_344464_, Identifier p_459889_) {
         return p_344464_.equals(this.attribute) && this.modifier.is(p_459889_);
      }
   }
}
