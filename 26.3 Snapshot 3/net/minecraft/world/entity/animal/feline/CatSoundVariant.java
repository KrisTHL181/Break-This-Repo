package net.minecraft.world.entity.animal.feline;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.sounds.SoundEvent;

public record CatSoundVariant(CatSoundVariant.CatSoundSet adultSounds, CatSoundVariant.CatSoundSet babySounds) {
   public static final Codec<CatSoundVariant> DIRECT_CODEC = codec();
   public static final Codec<CatSoundVariant> NETWORK_CODEC = codec();
   public static final Codec<Holder<CatSoundVariant>> CODEC = RegistryFixedCodec.create(Registries.CAT_SOUND_VARIANT);
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<CatSoundVariant>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.CAT_SOUND_VARIANT);

   private static Codec<CatSoundVariant> codec() {
      return RecordCodecBuilder.create(
         i -> i.group(
               CatSoundVariant.CatSoundSet.CODEC.fieldOf("adult_sounds").forGetter(CatSoundVariant::adultSounds),
               CatSoundVariant.CatSoundSet.CODEC.fieldOf("baby_sounds").forGetter(CatSoundVariant::babySounds)
            )
            .apply(i, CatSoundVariant::new)
      );
   }

   public record CatSoundSet(
      Holder<SoundEvent> ambientSound,
      Holder<SoundEvent> strayAmbientSound,
      Holder<SoundEvent> hissSound,
      Holder<SoundEvent> hurtSound,
      Holder<SoundEvent> deathSound,
      Holder<SoundEvent> eatSound,
      Holder<SoundEvent> begForFoodSound,
      Holder<SoundEvent> purrSound,
      Holder<SoundEvent> purreowSound
   ) {
      private static final Codec<CatSoundVariant.CatSoundSet> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               SoundEvent.CODEC.fieldOf("ambient_sound").forGetter(CatSoundVariant.CatSoundSet::ambientSound),
               SoundEvent.CODEC.fieldOf("stray_ambient_sound").forGetter(CatSoundVariant.CatSoundSet::strayAmbientSound),
               SoundEvent.CODEC.fieldOf("hiss_sound").forGetter(CatSoundVariant.CatSoundSet::hissSound),
               SoundEvent.CODEC.fieldOf("hurt_sound").forGetter(CatSoundVariant.CatSoundSet::hurtSound),
               SoundEvent.CODEC.fieldOf("death_sound").forGetter(CatSoundVariant.CatSoundSet::deathSound),
               SoundEvent.CODEC.fieldOf("eat_sound").forGetter(CatSoundVariant.CatSoundSet::eatSound),
               SoundEvent.CODEC.fieldOf("beg_for_food_sound").forGetter(CatSoundVariant.CatSoundSet::begForFoodSound),
               SoundEvent.CODEC.fieldOf("purr_sound").forGetter(CatSoundVariant.CatSoundSet::purrSound),
               SoundEvent.CODEC.fieldOf("purreow_sound").forGetter(CatSoundVariant.CatSoundSet::purreowSound)
            )
            .apply(i, CatSoundVariant.CatSoundSet::new)
      );
   }
}
