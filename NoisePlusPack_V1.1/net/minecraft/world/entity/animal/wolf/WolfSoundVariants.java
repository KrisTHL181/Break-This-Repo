package net.minecraft.world.entity.animal.wolf;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;

public class WolfSoundVariants {
   public static final ResourceKey<WolfSoundVariant> CLASSIC = createKey(WolfSoundVariants.SoundSet.CLASSIC);
   public static final ResourceKey<WolfSoundVariant> PUGLIN = createKey(WolfSoundVariants.SoundSet.PUGLIN);
   public static final ResourceKey<WolfSoundVariant> SAD = createKey(WolfSoundVariants.SoundSet.SAD);
   public static final ResourceKey<WolfSoundVariant> ANGRY = createKey(WolfSoundVariants.SoundSet.ANGRY);
   public static final ResourceKey<WolfSoundVariant> GRUMPY = createKey(WolfSoundVariants.SoundSet.GRUMPY);
   public static final ResourceKey<WolfSoundVariant> BIG = createKey(WolfSoundVariants.SoundSet.BIG);
   public static final ResourceKey<WolfSoundVariant> CUTE = createKey(WolfSoundVariants.SoundSet.CUTE);

   private static ResourceKey<WolfSoundVariant> createKey(WolfSoundVariants.SoundSet p_392951_) {
      return ResourceKey.create(Registries.WOLF_SOUND_VARIANT, Identifier.withDefaultNamespace(p_392951_.getIdentifier()));
   }

   public static void bootstrap(BootstrapContext<WolfSoundVariant> p_392134_) {
      register(p_392134_, CLASSIC, WolfSoundVariants.SoundSet.CLASSIC);
      register(p_392134_, PUGLIN, WolfSoundVariants.SoundSet.PUGLIN);
      register(p_392134_, SAD, WolfSoundVariants.SoundSet.SAD);
      register(p_392134_, ANGRY, WolfSoundVariants.SoundSet.ANGRY);
      register(p_392134_, GRUMPY, WolfSoundVariants.SoundSet.GRUMPY);
      register(p_392134_, BIG, WolfSoundVariants.SoundSet.BIG);
      register(p_392134_, CUTE, WolfSoundVariants.SoundSet.CUTE);
   }

   private static void register(BootstrapContext<WolfSoundVariant> p_392299_, ResourceKey<WolfSoundVariant> p_394952_, WolfSoundVariants.SoundSet p_397964_) {
      p_392299_.register(p_394952_, SoundEvents.WOLF_SOUNDS.get(p_397964_));
   }

   public static Holder<WolfSoundVariant> pickRandomSoundVariant(RegistryAccess p_392552_, RandomSource p_392469_) {
      return p_392552_.lookupOrThrow(Registries.WOLF_SOUND_VARIANT).getRandom(p_392469_).orElseThrow();
   }

   public enum SoundSet {
      CLASSIC("classic", ""),
      PUGLIN("puglin", "_puglin"),
      SAD("sad", "_sad"),
      ANGRY("angry", "_angry"),
      GRUMPY("grumpy", "_grumpy"),
      BIG("big", "_big"),
      CUTE("cute", "_cute");

      private final String identifier;
      private final String soundEventSuffix;

      SoundSet(final String p_393467_, final String p_395756_) {
         this.identifier = p_393467_;
         this.soundEventSuffix = p_395756_;
      }

      public String getIdentifier() {
         return this.identifier;
      }

      public String getSoundEventSuffix() {
         return this.soundEventSuffix;
      }
   }
}
