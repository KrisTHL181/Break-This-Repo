package net.minecraft.world.entity.animal.wolf;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.sounds.SoundEvent;

public record WolfSoundVariant(WolfSoundVariant.WolfSoundSet adultSounds, WolfSoundVariant.WolfSoundSet babySounds) {
   public static final Codec<WolfSoundVariant> DIRECT_CODEC = getWolfSoundVariantCodec();
   public static final Codec<WolfSoundVariant> NETWORK_CODEC = getWolfSoundVariantCodec();
   public static final Codec<Holder<WolfSoundVariant>> CODEC = RegistryFixedCodec.create(Registries.WOLF_SOUND_VARIANT);
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<WolfSoundVariant>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.WOLF_SOUND_VARIANT);

   private static Codec<WolfSoundVariant> getWolfSoundVariantCodec() {
      return RecordCodecBuilder.create(
         i -> i.group(
               WolfSoundVariant.WolfSoundSet.CODEC.fieldOf("adult_sounds").forGetter(WolfSoundVariant::adultSounds),
               WolfSoundVariant.WolfSoundSet.CODEC.fieldOf("baby_sounds").forGetter(WolfSoundVariant::babySounds)
            )
            .apply(i, WolfSoundVariant::new)
      );
   }

   public record WolfSoundSet(
      Holder<SoundEvent> ambientSound,
      Holder<SoundEvent> deathSound,
      Holder<SoundEvent> growlSound,
      Holder<SoundEvent> hurtSound,
      Holder<SoundEvent> pantSound,
      Holder<SoundEvent> whineSound,
      Holder<SoundEvent> stepSound
   ) {
      public static final Codec<WolfSoundVariant.WolfSoundSet> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               SoundEvent.CODEC.fieldOf("ambient_sound").forGetter(WolfSoundVariant.WolfSoundSet::ambientSound),
               SoundEvent.CODEC.fieldOf("death_sound").forGetter(WolfSoundVariant.WolfSoundSet::deathSound),
               SoundEvent.CODEC.fieldOf("growl_sound").forGetter(WolfSoundVariant.WolfSoundSet::growlSound),
               SoundEvent.CODEC.fieldOf("hurt_sound").forGetter(WolfSoundVariant.WolfSoundSet::hurtSound),
               SoundEvent.CODEC.fieldOf("pant_sound").forGetter(WolfSoundVariant.WolfSoundSet::pantSound),
               SoundEvent.CODEC.fieldOf("whine_sound").forGetter(WolfSoundVariant.WolfSoundSet::whineSound),
               SoundEvent.CODEC.fieldOf("step_sound").forGetter(WolfSoundVariant.WolfSoundSet::stepSound)
            )
            .apply(i, WolfSoundVariant.WolfSoundSet::new)
      );
   }
}
