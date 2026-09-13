package net.minecraft.world.item.consume_effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public record PlaySoundConsumeEffect(Holder<SoundEvent> sound) implements ConsumeEffect {
   public static final MapCodec<PlaySoundConsumeEffect> CODEC = RecordCodecBuilder.mapCodec(
      p_364850_ -> p_364850_.group(SoundEvent.CODEC.fieldOf("sound").forGetter(PlaySoundConsumeEffect::sound)).apply(p_364850_, PlaySoundConsumeEffect::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, PlaySoundConsumeEffect> STREAM_CODEC = StreamCodec.composite(
      SoundEvent.STREAM_CODEC, PlaySoundConsumeEffect::sound, PlaySoundConsumeEffect::new
   );

   @Override
   public ConsumeEffect.Type<PlaySoundConsumeEffect> getType() {
      return ConsumeEffect.Type.PLAY_SOUND;
   }

   @Override
   public boolean apply(Level p_363898_, ItemStack p_367416_, LivingEntity p_364772_) {
      p_363898_.playSound(null, p_364772_.blockPosition(), this.sound.value(), p_364772_.getSoundSource(), 1.0F, 1.0F);
      return true;
   }
}
