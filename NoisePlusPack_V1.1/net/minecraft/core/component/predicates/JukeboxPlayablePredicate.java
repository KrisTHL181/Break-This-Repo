package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.criterion.SingleComponentItemPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;

public record JukeboxPlayablePredicate(Optional<HolderSet<JukeboxSong>> song) implements SingleComponentItemPredicate<JukeboxPlayable> {
   public static final Codec<JukeboxPlayablePredicate> CODEC = RecordCodecBuilder.create(
      p_395681_ -> p_395681_.group(RegistryCodecs.homogeneousList(Registries.JUKEBOX_SONG).optionalFieldOf("song").forGetter(JukeboxPlayablePredicate::song))
         .apply(p_395681_, JukeboxPlayablePredicate::new)
   );

   @Override
   public DataComponentType<JukeboxPlayable> componentType() {
      return DataComponents.JUKEBOX_PLAYABLE;
   }

   public boolean matches(JukeboxPlayable p_394249_) {
      if (!this.song.isPresent()) {
         return true;
      }

      boolean flag = false;

      for (Holder<JukeboxSong> holder : this.song.get()) {
         Optional<ResourceKey<JukeboxSong>> optional = holder.unwrapKey();
         if (!optional.isEmpty() && optional.equals(p_394249_.song().key())) {
            flag = true;
            break;
         }
      }

      return flag;
   }

   public static JukeboxPlayablePredicate any() {
      return new JukeboxPlayablePredicate(Optional.empty());
   }
}
