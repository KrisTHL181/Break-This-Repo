package net.minecraft.world.entity.animal.pig;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;

public class PigSoundVariants {
   public static final ResourceKey<PigSoundVariant> CLASSIC = createKey(PigSoundVariants.SoundSet.CLASSIC);
   public static final ResourceKey<PigSoundVariant> MINI = createKey(PigSoundVariants.SoundSet.MINI);
   public static final ResourceKey<PigSoundVariant> BIG = createKey(PigSoundVariants.SoundSet.BIG);

   private static ResourceKey<PigSoundVariant> createKey(final PigSoundVariants.SoundSet pigSoundVariant) {
      return ResourceKey.create(Registries.PIG_SOUND_VARIANT, Identifier.withDefaultNamespace(pigSoundVariant.getIdentifier()));
   }

   public static void bootstrap(final BootstrapContext<PigSoundVariant> context) {
      register(context, CLASSIC, PigSoundVariants.SoundSet.CLASSIC);
      register(context, BIG, PigSoundVariants.SoundSet.BIG);
      register(context, MINI, PigSoundVariants.SoundSet.MINI);
   }

   private static void register(
      final BootstrapContext<PigSoundVariant> context, final ResourceKey<PigSoundVariant> key, final PigSoundVariants.SoundSet PigSoundVariant
   ) {
      context.register(key, SoundEvents.PIG_SOUNDS.get(PigSoundVariant));
   }

   public static Holder<PigSoundVariant> pickRandomSoundVariant(final RegistryAccess registryAccess, final RandomSource random) {
      return registryAccess.lookupOrThrow(Registries.PIG_SOUND_VARIANT).getRandom(random).orElseThrow();
   }

   public enum SoundSet {
      CLASSIC("classic", "pig"),
      MINI("mini", "pig_mini"),
      BIG("big", "pig_big");

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
