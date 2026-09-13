package net.minecraft.world.item.alchemy;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.ConsumableListener;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.Level;

public record PotionContents(Optional<Holder<Potion>> potion, Optional<Integer> customColor, List<MobEffectInstance> customEffects, Optional<String> customName)
   implements ConsumableListener,
   TooltipProvider {
   public static final PotionContents EMPTY = new PotionContents(Optional.empty(), Optional.empty(), List.of(), Optional.empty());
   private static final Component NO_EFFECT = Component.translatable("effect.none").withStyle(ChatFormatting.GRAY);
   public static final int BASE_POTION_COLOR = -13083194;
   private static final Codec<PotionContents> FULL_CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Potion.CODEC.optionalFieldOf("potion").forGetter(PotionContents::potion),
            Codec.INT.optionalFieldOf("custom_color").forGetter(PotionContents::customColor),
            MobEffectInstance.CODEC.listOf().optionalFieldOf("custom_effects", List.of()).forGetter(PotionContents::customEffects),
            Codec.STRING.optionalFieldOf("custom_name").forGetter(PotionContents::customName)
         )
         .apply(i, PotionContents::new)
   );
   public static final Codec<PotionContents> CODEC = Codec.withAlternative(FULL_CODEC, Potion.CODEC, PotionContents::new);
   public static final StreamCodec<RegistryFriendlyByteBuf, PotionContents> STREAM_CODEC = StreamCodec.composite(
      Potion.STREAM_CODEC.apply(ByteBufCodecs::optional),
      PotionContents::potion,
      ByteBufCodecs.INT.apply(ByteBufCodecs::optional),
      PotionContents::customColor,
      MobEffectInstance.STREAM_CODEC.apply(ByteBufCodecs.list()),
      PotionContents::customEffects,
      ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs::optional),
      PotionContents::customName,
      PotionContents::new
   );

   public PotionContents(final Holder<Potion> potion) {
      this(Optional.of(potion), Optional.empty(), List.of(), Optional.empty());
   }

   public static ItemStack createItemStack(final Item item, final Holder<Potion> potion) {
      ItemStack itemStack = new ItemStack(item);
      itemStack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
      return itemStack;
   }

   public boolean is(final Holder<Potion> potion) {
      return this.potion.isPresent() && this.potion.get().is(potion) && this.customEffects.isEmpty();
   }

   public Iterable<MobEffectInstance> getAllEffects() {
      if (this.potion.isEmpty()) {
         return this.customEffects;
      } else {
         return this.customEffects.isEmpty()
            ? this.potion.get().value().getEffects()
            : Iterables.concat(this.potion.get().value().getEffects(), this.customEffects);
      }
   }

   public void forEachEffect(final Consumer<MobEffectInstance> consumer, final float durationScale) {
      if (this.potion.isPresent()) {
         for (MobEffectInstance effect : this.potion.get().value().getEffects()) {
            consumer.accept(effect.withScaledDuration(durationScale));
         }
      }

      for (MobEffectInstance effect : this.customEffects) {
         consumer.accept(effect.withScaledDuration(durationScale));
      }
   }

   public PotionContents withPotion(final Holder<Potion> potion) {
      return new PotionContents(Optional.of(potion), this.customColor, this.customEffects, this.customName);
   }

   public PotionContents withEffectAdded(final MobEffectInstance effect) {
      return new PotionContents(this.potion, this.customColor, Util.copyAndAdd(this.customEffects, effect), this.customName);
   }

   public int getColor() {
      return this.getColorOr(-13083194);
   }

   public int getColorOr(final int defaultColor) {
      return this.customColor.isPresent() ? this.customColor.get() : getColorOptional(this.getAllEffects()).orElse(defaultColor);
   }

   public Component getName(final String prefix) {
      String suffix = this.customName.or(() -> this.potion.map(p -> p.value().name())).orElse("empty");
      return Component.translatable(prefix + suffix);
   }

   public static OptionalInt getColorOptional(final Iterable<MobEffectInstance> effects) {
      int red = 0;
      int green = 0;
      int blue = 0;
      int totalWeight = 0;

      for (MobEffectInstance effect : effects) {
         if (effect.isVisible()) {
            int color = effect.getEffect().value().getColor();
            int amplifier = effect.getAmplifier() + 1;
            red += amplifier * ARGB.red(color);
            green += amplifier * ARGB.green(color);
            blue += amplifier * ARGB.blue(color);
            totalWeight += amplifier;
         }
      }

      return totalWeight == 0 ? OptionalInt.empty() : OptionalInt.of(ARGB.color(red / totalWeight, green / totalWeight, blue / totalWeight));
   }

   public boolean hasEffects() {
      return !this.customEffects.isEmpty() ? true : this.potion.isPresent() && !this.potion.get().value().getEffects().isEmpty();
   }

   public List<MobEffectInstance> customEffects() {
      return Lists.transform(this.customEffects, MobEffectInstance::new);
   }

   public void applyToLivingEntity(final LivingEntity entity, final float durationScale) {
      if (entity.level() instanceof ServerLevel serverLevel) {
         Player player = entity instanceof Player playerEntity ? playerEntity : null;
         this.forEachEffect(effect -> {
            if (effect.getEffect().value().isInstantaneous()) {
               effect.getEffect().value().applyInstantaneousEffect(serverLevel, player, player, entity, effect.getAmplifier(), 1.0);
            } else {
               entity.addEffect(effect);
            }
         }, durationScale);
      }
   }

   public static void addPotionTooltip(
      final Iterable<MobEffectInstance> effects, final Consumer<Component> lines, final float durationScale, final float tickrate
   ) {
      List<Pair<Holder<Attribute>, AttributeModifier>> modifiers = Lists.newArrayList();
      boolean noEffects = true;

      for (MobEffectInstance effect : effects) {
         noEffects = false;
         Holder<MobEffect> mobEffect = effect.getEffect();
         int amplifier = effect.getAmplifier();
         mobEffect.value().createModifiers(amplifier, (attribute, modifierx) -> modifiers.add(new Pair(attribute, modifierx)));
         MutableComponent line = getPotionDescription(mobEffect, amplifier);
         if (!effect.endsWithin(20)) {
            line = Component.translatable("potion.withDuration", line, MobEffectUtil.formatDuration(effect, durationScale, tickrate));
         }

         lines.accept(line.withStyle(mobEffect.value().getCategory().getTooltipFormatting()));
      }

      if (noEffects) {
         lines.accept(NO_EFFECT);
      }

      if (!modifiers.isEmpty()) {
         lines.accept(CommonComponents.EMPTY);
         lines.accept(Component.translatable("potion.whenDrank").withStyle(ChatFormatting.DARK_PURPLE));

         for (Pair<Holder<Attribute>, AttributeModifier> entry : modifiers) {
            AttributeModifier modifier = (AttributeModifier)entry.getSecond();
            double amount = modifier.amount();
            double displayAmount;
            if (modifier.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_BASE
               && modifier.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
               displayAmount = modifier.amount();
            } else {
               displayAmount = modifier.amount() * 100.0;
            }

            if (amount > 0.0) {
               lines.accept(
                  Component.translatable(
                        "attribute.modifier.plus." + modifier.operation().id(),
                        ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(displayAmount),
                        Component.translatable(((Attribute)((Holder)entry.getFirst()).value()).getDescriptionId())
                     )
                     .withStyle(ChatFormatting.BLUE)
               );
            } else if (amount < 0.0) {
               displayAmount *= -1.0;
               lines.accept(
                  Component.translatable(
                        "attribute.modifier.take." + modifier.operation().id(),
                        ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(displayAmount),
                        Component.translatable(((Attribute)((Holder)entry.getFirst()).value()).getDescriptionId())
                     )
                     .withStyle(ChatFormatting.RED)
               );
            }
         }
      }
   }

   public static MutableComponent getPotionDescription(final Holder<MobEffect> mobEffect, final int amplifier) {
      MutableComponent line = Component.translatable(mobEffect.value().getDescriptionId());
      return amplifier > 0 ? Component.translatable("potion.withAmplifier", line, Component.translatable("potion.potency." + amplifier)) : line;
   }

   @Override
   public void onConsume(final Level level, final LivingEntity user, final ItemStack stack, final Consumable consumable) {
      this.applyToLivingEntity(user, stack.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F));
   }

   @Override
   public void addToTooltip(final Item.TooltipContext context, final Consumer<Component> consumer, final TooltipFlag flag, final DataComponentGetter components) {
      addPotionTooltip(this.getAllEffects(), consumer, components.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F), context.tickRate());
   }
}
