package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

public record PlaySoundEffect(List<Holder<SoundEvent>> soundEvents, FloatProvider volume, FloatProvider pitch) implements EnchantmentEntityEffect {
   public static final MapCodec<PlaySoundEffect> CODEC = RecordCodecBuilder.mapCodec(
      p_449869_ -> p_449869_.group(
            ExtraCodecs.compactListCodec(SoundEvent.CODEC, SoundEvent.CODEC.sizeLimitedListOf(255)).fieldOf("sound").forGetter(PlaySoundEffect::soundEvents),
            FloatProvider.codec(1.0E-5F, 10.0F).fieldOf("volume").forGetter(PlaySoundEffect::volume),
            FloatProvider.codec(1.0E-5F, 2.0F).fieldOf("pitch").forGetter(PlaySoundEffect::pitch)
         )
         .apply(p_449869_, PlaySoundEffect::new)
   );

   @Override
   public void apply(ServerLevel p_342634_, int p_344633_, EnchantedItemInUse p_344196_, Entity p_344269_, Vec3 p_343993_) {
      if (!p_344269_.isSilent()) {
         RandomSource randomsource = p_344269_.getRandom();
         int i = Mth.clamp(p_344633_ - 1, 0, this.soundEvents.size() - 1);
         p_342634_.playSound(
            null,
            p_343993_.x(),
            p_343993_.y(),
            p_343993_.z(),
            this.soundEvents.get(i),
            p_344269_.getSoundSource(),
            this.volume.sample(randomsource),
            this.pitch.sample(randomsource)
         );
      }
   }

   @Override
   public MapCodec<PlaySoundEffect> codec() {
      return CODEC;
   }
}
