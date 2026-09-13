package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.SingleComponentItemPredicate;
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
      i -> i.group(RegistryCodecs.homogeneousList(Registries.JUKEBOX_SONG).optionalFieldOf("song").forGetter(JukeboxPlayablePredicate::song))
         .apply(i, JukeboxPlayablePredicate::new)
   );

   @Override
   public DataComponentType<JukeboxPlayable> componentType() {
      return DataComponents.JUKEBOX_PLAYABLE;
   }

   public boolean matches(final JukeboxPlayable value) {
      if (!this.song.isPresent()) {
         return true;
      }

      boolean songIsPresent = false;

      for (Holder<JukeboxSong> maybeSong : this.song.get()) {
         Optional<ResourceKey<JukeboxSong>> songId = maybeSong.unwrapKey();
         if (!songId.isEmpty() && songId.equals(value.song().unwrapKey())) {
            songIsPresent = true;
            break;
         }
      }

      return songIsPresent;
   }

   public static JukeboxPlayablePredicate any() {
      return new JukeboxPlayablePredicate(Optional.empty());
   }
}
