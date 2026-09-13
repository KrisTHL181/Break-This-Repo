package net.minecraft.world.food;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.ConsumableListener;
import net.minecraft.world.level.Level;

public record FoodProperties(int nutrition, float saturation, boolean canAlwaysEat) implements ConsumableListener {
   public static final Codec<FoodProperties> DIRECT_CODEC = RecordCodecBuilder.create(
      p_359368_ -> p_359368_.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("nutrition").forGetter(FoodProperties::nutrition),
            Codec.FLOAT.fieldOf("saturation").forGetter(FoodProperties::saturation),
            Codec.BOOL.optionalFieldOf("can_always_eat", false).forGetter(FoodProperties::canAlwaysEat)
         )
         .apply(p_359368_, FoodProperties::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, FoodProperties> DIRECT_STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      FoodProperties::nutrition,
      ByteBufCodecs.FLOAT,
      FoodProperties::saturation,
      ByteBufCodecs.BOOL,
      FoodProperties::canAlwaysEat,
      FoodProperties::new
   );

   @Override
   public void onConsume(Level p_369423_, LivingEntity p_368675_, ItemStack p_365501_, Consumable p_363411_) {
      RandomSource randomsource = p_368675_.getRandom();
      p_369423_.playSound(
         null, p_368675_.getX(), p_368675_.getY(), p_368675_.getZ(), p_363411_.sound().value(), SoundSource.NEUTRAL, 1.0F, randomsource.triangle(1.0F, 0.4F)
      );
      if (p_368675_ instanceof Player player) {
         player.getFoodData().eat(this);
         p_369423_.playSound(
            null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, Mth.randomBetween(randomsource, 0.9F, 1.0F)
         );
      }
   }

   public static class Builder {
      private int nutrition;
      private float saturationModifier;
      private boolean canAlwaysEat;

      public FoodProperties.Builder nutrition(int p_38761_) {
         this.nutrition = p_38761_;
         return this;
      }

      public FoodProperties.Builder saturationModifier(float p_38759_) {
         this.saturationModifier = p_38759_;
         return this;
      }

      public FoodProperties.Builder alwaysEdible() {
         this.canAlwaysEat = true;
         return this;
      }

      public FoodProperties build() {
         float f = FoodConstants.saturationByModifier(this.nutrition, this.saturationModifier);
         return new FoodProperties(this.nutrition, f, this.canAlwaysEat);
      }
   }
}
