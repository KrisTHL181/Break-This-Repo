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
import net.minecraft.util.valueproviders.FloatProviders;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

public record PlaySoundEffect(List<Holder<SoundEvent>> soundEvents, FloatProvider volume, FloatProvider pitch) implements EnchantmentEntityEffect {
   public static final MapCodec<PlaySoundEffect> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            ExtraCodecs.compactListCodec(SoundEvent.CODEC, SoundEvent.CODEC.sizeLimitedListOf(255)).fieldOf("sound").forGetter(PlaySoundEffect::soundEvents),
            FloatProviders.codec(1.0E-5F, 10.0F).fieldOf("volume").forGetter(PlaySoundEffect::volume),
            FloatProviders.codec(1.0E-5F, 2.0F).fieldOf("pitch").forGetter(PlaySoundEffect::pitch)
         )
         .apply(i, PlaySoundEffect::new)
   );

   @Override
   public void apply(final ServerLevel serverLevel, final int enchantmentLevel, final EnchantedItemInUse item, final Entity entity, final Vec3 position) {
      if (!entity.isSilent()) {
         RandomSource random = entity.getRandom();
         int index = Mth.clamp(enchantmentLevel - 1, 0, this.soundEvents.size() - 1);
         serverLevel.playSound(
            null,
            position.x(),
            position.y(),
            position.z(),
            this.soundEvents.get(index),
            entity.getSoundSource(),
            this.volume.sample(random),
            this.pitch.sample(random)
         );
      }
   }

   @Override
   public MapCodec<PlaySoundEffect> codec() {
      return CODEC;
   }
}
