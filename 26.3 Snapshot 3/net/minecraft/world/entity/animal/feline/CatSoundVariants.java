package net.minecraft.world.entity.animal.feline;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;

public class CatSoundVariants {
   public static final ResourceKey<CatSoundVariant> CLASSIC = createKey(CatSoundVariants.SoundSet.CLASSIC);
   public static final ResourceKey<CatSoundVariant> ROYAL = createKey(CatSoundVariants.SoundSet.ROYAL);

   private static ResourceKey<CatSoundVariant> createKey(final CatSoundVariants.SoundSet catSoundVariant) {
      return ResourceKey.create(Registries.CAT_SOUND_VARIANT, Identifier.withDefaultNamespace(catSoundVariant.getIdentifier()));
   }

   public static void bootstrap(final BootstrapContext<CatSoundVariant> context) {
      register(context, CLASSIC, CatSoundVariants.SoundSet.CLASSIC);
      register(context, ROYAL, CatSoundVariants.SoundSet.ROYAL);
   }

   private static void register(
      final BootstrapContext<CatSoundVariant> context, final ResourceKey<CatSoundVariant> key, final CatSoundVariants.SoundSet CatSoundVariant
   ) {
      context.register(key, SoundEvents.CAT_SOUNDS.get(CatSoundVariant));
   }

   public static Holder<CatSoundVariant> pickRandomSoundVariant(final RegistryAccess registryAccess, final RandomSource random) {
      return registryAccess.lookupOrThrow(Registries.CAT_SOUND_VARIANT).getRandom(random).orElseThrow();
   }

   public enum SoundSet {
      CLASSIC("classic", "cat"),
      ROYAL("royal", "cat_royal");

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
