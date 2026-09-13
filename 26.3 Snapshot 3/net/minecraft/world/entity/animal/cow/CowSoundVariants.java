package net.minecraft.world.entity.animal.cow;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;

public class CowSoundVariants {
   public static final ResourceKey<CowSoundVariant> CLASSIC = createKey(CowSoundVariants.SoundSet.CLASSIC);
   public static final ResourceKey<CowSoundVariant> MOODY = createKey(CowSoundVariants.SoundSet.MOODY);

   private static ResourceKey<CowSoundVariant> createKey(final CowSoundVariants.SoundSet cowSoundVariant) {
      return ResourceKey.create(Registries.COW_SOUND_VARIANT, Identifier.withDefaultNamespace(cowSoundVariant.getIdentifier()));
   }

   public static void bootstrap(final BootstrapContext<CowSoundVariant> context) {
      register(context, CLASSIC, CowSoundVariants.SoundSet.CLASSIC);
      register(context, MOODY, CowSoundVariants.SoundSet.MOODY);
   }

   private static void register(
      final BootstrapContext<CowSoundVariant> context, final ResourceKey<CowSoundVariant> key, final CowSoundVariants.SoundSet CowSoundVariant
   ) {
      context.register(key, SoundEvents.COW_SOUNDS.get(CowSoundVariant));
   }

   public static Holder<CowSoundVariant> pickRandomSoundVariant(final RegistryAccess registryAccess, final RandomSource random) {
      return registryAccess.lookupOrThrow(Registries.COW_SOUND_VARIANT).getRandom(random).orElseThrow();
   }

   public enum SoundSet {
      CLASSIC("classic", "cow"),
      MOODY("moody", "cow_moody");

      private final String identifier;
      private final String soundEventIdentifier;

      SoundSet(final String identifier, final String soundEventIdentifier) {
         this.identifier = identifier;
         this.soundEventIdentifier = soundEventIdentifier;
      }

      public String getIdentifier() {
         return this.identifier;
      }

      public String getSoundEventIdentifier() {
         return this.soundEventIdentifier;
      }
   }
}
