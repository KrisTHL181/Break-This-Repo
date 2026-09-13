package net.minecraft.world.item;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;

public interface Instruments {
   int GOAT_HORN_RANGE_BLOCKS = 256;
   float GOAT_HORN_DURATION = 7.0F;
   int GOAT_HORN_INSTRUMENT_DAMAGE = 0;
   ResourceKey<Instrument> PONDER_GOAT_HORN = create("ponder_goat_horn");
   ResourceKey<Instrument> SING_GOAT_HORN = create("sing_goat_horn");
   ResourceKey<Instrument> SEEK_GOAT_HORN = create("seek_goat_horn");
   ResourceKey<Instrument> FEEL_GOAT_HORN = create("feel_goat_horn");
   ResourceKey<Instrument> ADMIRE_GOAT_HORN = create("admire_goat_horn");
   ResourceKey<Instrument> CALL_GOAT_HORN = create("call_goat_horn");
   ResourceKey<Instrument> YEARN_GOAT_HORN = create("yearn_goat_horn");
   ResourceKey<Instrument> DREAM_GOAT_HORN = create("dream_goat_horn");

   private static ResourceKey<Instrument> create(final String id) {
      return ResourceKey.create(Registries.INSTRUMENT, Identifier.withDefaultNamespace(id));
   }

   static void bootstrap(final BootstrapContext<Instrument> context) {
      register(context, PONDER_GOAT_HORN, (Holder<SoundEvent>)SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0), 7.0F, 256.0F, 0);
      register(context, SING_GOAT_HORN, (Holder<SoundEvent>)SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(1), 7.0F, 256.0F, 0);
      register(context, SEEK_GOAT_HORN, (Holder<SoundEvent>)SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(2), 7.0F, 256.0F, 0);
      register(context, FEEL_GOAT_HORN, (Holder<SoundEvent>)SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(3), 7.0F, 256.0F, 0);
      register(context, ADMIRE_GOAT_HORN, (Holder<SoundEvent>)SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(4), 7.0F, 256.0F, 0);
      register(context, CALL_GOAT_HORN, (Holder<SoundEvent>)SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(5), 7.0F, 256.0F, 0);
      register(context, YEARN_GOAT_HORN, (Holder<SoundEvent>)SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(6), 7.0F, 256.0F, 0);
      register(context, DREAM_GOAT_HORN, (Holder<SoundEvent>)SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(7), 7.0F, 256.0F, 0);
   }

   static void register(
      final BootstrapContext<Instrument> context,
      final ResourceKey<Instrument> key,
      final Holder<SoundEvent> soundEvent,
      final float duration,
      final float range,
      final int durabilityDamage
   ) {
      MutableComponent description = Component.translatable(Util.makeDescriptionId("instrument", key.identifier()));
      context.register(key, new Instrument(soundEvent, duration, range, durabilityDamage, description));
   }
}
