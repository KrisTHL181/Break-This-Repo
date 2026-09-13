package net.minecraft.world.entity.animal.chicken;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;

public class ChickenSoundVariants {
   public static final ResourceKey<ChickenSoundVariant> CLASSIC = createKey(ChickenSoundVariants.SoundSet.CLASSIC);
   public static final ResourceKey<ChickenSoundVariant> PICKY = createKey(ChickenSoundVariants.SoundSet.PICKY);

   private static ResourceKey<ChickenSoundVariant> createKey(final ChickenSoundVariants.SoundSet chickenSoundVariant) {
      return ResourceKey.create(Registries.CHICKEN_SOUND_VARIANT, Identifier.withDefaultNamespace(chickenSoundVariant.getIdentifier()));
   }

   public static void bootstrap(final BootstrapContext<ChickenSoundVariant> context) {
      register(context, CLASSIC, ChickenSoundVariants.SoundSet.CLASSIC);
      register(context, PICKY, ChickenSoundVariants.SoundSet.PICKY);
   }

   private static void register(
      final BootstrapContext<ChickenSoundVariant> context, final ResourceKey<ChickenSoundVariant> key, final ChickenSoundVariants.SoundSet ChickenSoundVariant
   ) {
      context.register(key, SoundEvents.CHICKEN_SOUNDS.get(ChickenSoundVariant));
   }

   public static Holder<ChickenSoundVariant> pickRandomSoundVariant(final RegistryAccess registryAccess, final RandomSource random) {
      return registryAccess.lookupOrThrow(Registries.CHICKEN_SOUND_VARIANT).getRandom(random).orElseThrow();
   }

   public enum SoundSet {
      CLASSIC("classic", "chicken"),
      PICKY("picky", "chicken_picky");

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
