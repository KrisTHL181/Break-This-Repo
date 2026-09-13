package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.CriteriaTriggers;
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
      p_367547_ -> p_367547_.group(
            ExtraCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("consume_seconds", 1.6F).forGetter(Consumable::consumeSeconds),
            ItemUseAnimation.CODEC.optionalFieldOf("animation", ItemUseAnimation.EAT).forGetter(Consumable::animation),
            SoundEvent.CODEC.optionalFieldOf("sound", SoundEvents.GENERIC_EAT).forGetter(Consumable::sound),
            Codec.BOOL.optionalFieldOf("has_consume_particles", true).forGetter(Consumable::hasConsumeParticles),
            ConsumeEffect.CODEC.listOf().optionalFieldOf("on_consume_effects", List.of()).forGetter(Consumable::onConsumeEffects)
         )
         .apply(p_367547_, Consumable::new)
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

   public InteractionResult startConsuming(LivingEntity p_370227_, ItemStack p_368269_, InteractionHand p_364933_) {
      if (!this.canConsume(p_370227_, p_368269_)) {
         return InteractionResult.FAIL;
      } else {
         boolean flag = this.consumeTicks() > 0;
         if (flag) {
            p_370227_.startUsingItem(p_364933_);
            return InteractionResult.CONSUME;
         } else {
            ItemStack itemstack = this.onConsume(p_370227_.level(), p_370227_, p_368269_);
            return InteractionResult.CONSUME.heldItemTransformedTo(itemstack);
         }
      }
   }

   public ItemStack onConsume(Level p_363427_, LivingEntity p_363286_, ItemStack p_367304_) {
      RandomSource randomsource = p_363286_.getRandom();
      this.emitParticlesAndSounds(randomsource, p_363286_, p_367304_, 16);
      if (p_363286_ instanceof ServerPlayer serverplayer) {
         serverplayer.awardStat(Stats.ITEM_USED.get(p_367304_.getItem()));
         CriteriaTriggers.CONSUME_ITEM.trigger(serverplayer, p_367304_);
      }

      p_367304_.getAllOfType(ConsumableListener.class).forEach(p_363704_ -> p_363704_.onConsume(p_363427_, p_363286_, p_367304_, this));
      if (!p_363427_.isClientSide()) {
         this.onConsumeEffects.forEach(p_360884_ -> p_360884_.apply(p_363427_, p_367304_, p_363286_));
      }

      p_363286_.gameEvent(this.animation == ItemUseAnimation.DRINK ? GameEvent.DRINK : GameEvent.EAT);
      p_367304_.consume(1, p_363286_);
      return p_367304_;
   }

   public boolean canConsume(LivingEntity p_363940_, ItemStack p_367934_) {
      FoodProperties foodproperties = p_367934_.get(DataComponents.FOOD);
      return foodproperties != null && p_363940_ instanceof Player player ? player.canEat(foodproperties.canAlwaysEat()) : true;
   }

   public int consumeTicks() {
      return (int)(this.consumeSeconds * 20.0F);
   }

   public void emitParticlesAndSounds(RandomSource p_366546_, LivingEntity p_365515_, ItemStack p_366278_, int p_361912_) {
      float f = p_366546_.nextBoolean() ? 0.5F : 1.0F;
      float f1 = p_366546_.triangle(1.0F, 0.2F);
      float f2 = 0.5F;
      float f3 = Mth.randomBetween(p_366546_, 0.9F, 1.0F);
      float f4 = this.animation == ItemUseAnimation.DRINK ? 0.5F : f;
      float f5 = this.animation == ItemUseAnimation.DRINK ? f3 : f1;
      if (this.hasConsumeParticles) {
         p_365515_.spawnItemParticles(p_366278_, p_361912_);
      }

      SoundEvent soundevent = p_365515_ instanceof Consumable.OverrideConsumeSound consumable$overrideconsumesound
         ? consumable$overrideconsumesound.getConsumeSound(p_366278_)
         : this.sound.value();
      p_365515_.playSound(soundevent, f4, f5);
   }

   public boolean shouldEmitParticlesAndSounds(int p_366088_) {
      int i = this.consumeTicks() - p_366088_;
      int j = (int)(this.consumeTicks() * 0.21875F);
      boolean flag = i > j;
      return flag && p_366088_ % 4 == 0;
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

      Builder() {
      }

      public Consumable.Builder consumeSeconds(float p_362944_) {
         this.consumeSeconds = p_362944_;
         return this;
      }

      public Consumable.Builder animation(ItemUseAnimation p_369583_) {
         this.animation = p_369583_;
         return this;
      }

      public Consumable.Builder sound(Holder<SoundEvent> p_367289_) {
         this.sound = p_367289_;
         return this;
      }

      public Consumable.Builder soundAfterConsume(Holder<SoundEvent> p_367814_) {
         return this.onConsume(new PlaySoundConsumeEffect(p_367814_));
      }

      public Consumable.Builder hasConsumeParticles(boolean p_367235_) {
         this.hasConsumeParticles = p_367235_;
         return this;
      }

      public Consumable.Builder onConsume(ConsumeEffect p_362433_) {
         this.onConsumeEffects.add(p_362433_);
         return this;
      }

      public Consumable build() {
         return new Consumable(this.consumeSeconds, this.animation, this.sound, this.hasConsumeParticles, this.onConsumeEffects);
      }
   }

   public interface OverrideConsumeSound {
      SoundEvent getConsumeSound(ItemStack var1);
   }
}
