package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.consume_effects.PlaySoundConsumeEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public record Consumable(
   float consumeSeconds, ItemUseAnimation animation, Holder<SoundEvent> sound, boolean hasConsumeParticles, List<ConsumeEffect> onConsumeEffects
) {
   public static final float DEFAULT_CONSUME_SECONDS = 1.6F;
   private static final int CONSUME_EFFECTS_INTERVAL = 4;
   private static final float CONSUME_EFFECTS_START_FRACTION = 0.21875F;
   public static final Codec<Consumable> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            ExtraCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("consume_seconds", 1.6F).forGetter(Consumable::consumeSeconds),
            ItemUseAnimation.CODEC.optionalFieldOf("animation", ItemUseAnimation.EAT).forGetter(Consumable::animation),
            SoundEvent.CODEC.optionalFieldOf("sound", SoundEvents.GENERIC_EAT).forGetter(Consumable::sound),
            Codec.BOOL.optionalFieldOf("has_consume_particles", true).forGetter(Consumable::hasConsumeParticles),
            ConsumeEffect.CODEC.listOf().optionalFieldOf("on_consume_effects", List.of()).forGetter(Consumable::onConsumeEffects)
         )
         .apply(i, Consumable::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, Consumable> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.FLOAT,
      Consumable::consumeSeconds,
      ItemUseAnimation.STREAM_CODEC,
      Consumable::animation,
      SoundEvent.STREAM_CODEC,
      Consumable::sound,
      ByteBufCodecs.BOOL,
      Consumable::hasConsumeParticles,
      ConsumeEffect.STREAM_CODEC.apply(ByteBufCodecs.list()),
      Consumable::onConsumeEffects,
      Consumable::new
   );

   public InteractionResult startConsuming(final LivingEntity user, final ItemStack stack, final InteractionHand hand) {
      if (!this.canConsume(user, stack)) {
         return InteractionResult.FAIL;
      } else {
         boolean consumesOverTime = this.consumeTicks() > 0;
         if (consumesOverTime) {
            user.startUsingItem(hand);
            return InteractionResult.CONSUME;
         } else {
            ItemStack result = this.onConsume(user.level(), user, stack);
            return InteractionResult.CONSUME.heldItemTransformedTo(result);
         }
      }
   }

   public ItemStack onConsume(final Level level, final LivingEntity user, final ItemStack stack) {
      RandomSource random = user.getRandom();
      this.emitParticlesAndSounds(random, user, stack, 16);
      if (user instanceof ServerPlayer serverPlayer) {
         serverPlayer.awardStat(Stats.ITEM_USED.get(stack.getItem()));
         CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
      }

      stack.getAllOfType(ConsumableListener.class).forEach(component -> component.onConsume(level, user, stack, this));
      if (!level.isClientSide()) {
         this.onConsumeEffects.forEach(action -> action.apply(level, stack, user));
      }

      user.gameEvent(this.animation == ItemUseAnimation.DRINK ? GameEvent.DRINK : GameEvent.EAT);
      stack.consume(1, user);
      return stack;
   }

   public boolean canConsume(final LivingEntity user, final ItemStack stack) {
      FoodProperties foodProperties = stack.get(DataComponents.FOOD);
      return foodProperties != null && user instanceof Player player ? player.canEat(foodProperties.canAlwaysEat()) : true;
   }

   public int consumeTicks() {
      return (int)(this.consumeSeconds * 20.0F);
   }

   public void emitParticlesAndSounds(final RandomSource random, final LivingEntity user, final ItemStack itemStack, final int particleCount) {
      float eatVolume = random.nextBoolean() ? 0.5F : 1.0F;
      float eatPitch = random.triangle(1.0F, 0.2F);
      float drinkVolume = 0.5F;
      float drinkPitch = Mth.randomBetween(random, 0.9F, 1.0F);
      float consumableVolume = this.animation == ItemUseAnimation.DRINK ? 0.5F : eatVolume;
      float consumablePitch = this.animation == ItemUseAnimation.DRINK ? drinkPitch : eatPitch;
      if (this.hasConsumeParticles) {
         user.spawnItemParticles(itemStack, particleCount);
      }

      SoundEvent consumeSound = user instanceof Consumable.OverrideConsumeSound override ? override.getConsumeSound(itemStack) : this.sound.value();
      user.playSound(consumeSound, consumableVolume, consumablePitch);
   }

   public boolean shouldEmitParticlesAndSounds(final int useItemRemainingTicks) {
      int itemUsedForTicks = this.consumeTicks() - useItemRemainingTicks;
      int waitTicksBeforeUseEffects = (int)(this.consumeTicks() * 0.21875F);
      boolean isValidTime = itemUsedForTicks > waitTicksBeforeUseEffects;
      return isValidTime && useItemRemainingTicks % 4 == 0;
   }

   public static Consumable.Builder builder() {
      return new Consumable.Builder();
   }

   public static class Builder {
      private float consumeSeconds = 1.6F;
      private ItemUseAnimation animation = ItemUseAnimation.EAT;
      private Holder<SoundEvent> sound = SoundEvents.GENERIC_EAT;
      private boolean hasConsumeParticles = true;
      private final List<ConsumeEffect> onConsumeEffects = new ArrayList<>();

      private Builder() {
      }

      public Consumable.Builder consumeSeconds(final float consumeSeconds) {
         this.consumeSeconds = consumeSeconds;
         return this;
      }

      public Consumable.Builder animation(final ItemUseAnimation animation) {
         this.animation = animation;
         return this;
      }

      public Consumable.Builder sound(final Holder<SoundEvent> sound) {
         this.sound = sound;
         return this;
      }

      public Consumable.Builder soundAfterConsume(final Holder<SoundEvent> soundAfterConsume) {
         return this.onConsume(new PlaySoundConsumeEffect(soundAfterConsume));
      }

      public Consumable.Builder hasConsumeParticles(final boolean hasConsumeParticles) {
         this.hasConsumeParticles = hasConsumeParticles;
         return this;
      }

      public Consumable.Builder onConsume(final ConsumeEffect effect) {
         this.onConsumeEffects.add(effect);
         return this;
      }

      public Consumable build() {
         return new Consumable(this.consumeSeconds, this.animation, this.sound, this.hasConsumeParticles, this.onConsumeEffects);
      }
   }

   public interface OverrideConsumeSound {
      SoundEvent getConsumeSound(final ItemStack itemStack);
   }
}
