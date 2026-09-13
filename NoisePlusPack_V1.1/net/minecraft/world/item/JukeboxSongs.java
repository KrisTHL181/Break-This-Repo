package net.minecraft.world.item;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;

public interface JukeboxSongs {
   ResourceKey<JukeboxSong> THIRTEEN = create("13");
   ResourceKey<JukeboxSong> CAT = create("cat");
   ResourceKey<JukeboxSong> BLOCKS = create("blocks");
   ResourceKey<JukeboxSong> CHIRP = create("chirp");
   ResourceKey<JukeboxSong> FAR = create("far");
   ResourceKey<JukeboxSong> MALL = create("mall");
   ResourceKey<JukeboxSong> MELLOHI = create("mellohi");
   ResourceKey<JukeboxSong> STAL = create("stal");
   ResourceKey<JukeboxSong> STRAD = create("strad");
   ResourceKey<JukeboxSong> WARD = create("ward");
   ResourceKey<JukeboxSong> ELEVEN = create("11");
   ResourceKey<JukeboxSong> WAIT = create("wait");
   ResourceKey<JukeboxSong> PIGSTEP = create("pigstep");
   ResourceKey<JukeboxSong> OTHERSIDE = create("otherside");
   ResourceKey<JukeboxSong> FIVE = create("5");
   ResourceKey<JukeboxSong> RELIC = create("relic");
   ResourceKey<JukeboxSong> PRECIPICE = create("precipice");
   ResourceKey<JukeboxSong> CREATOR = create("creator");
   ResourceKey<JukeboxSong> CREATOR_MUSIC_BOX = create("creator_music_box");
   ResourceKey<JukeboxSong> TEARS = create("tears");
   ResourceKey<JukeboxSong> LAVA_CHICKEN = create("lava_chicken");

   private static ResourceKey<JukeboxSong> create(String p_342453_) {
      return ResourceKey.create(Registries.JUKEBOX_SONG, Identifier.withDefaultNamespace(p_342453_));
   }

   private static void register(
      BootstrapContext<JukeboxSong> p_342526_, ResourceKey<JukeboxSong> p_344899_, Holder.Reference<SoundEvent> p_343874_, int p_343603_, int p_345058_
   ) {
      p_342526_.register(
         p_344899_, new JukeboxSong(p_343874_, Component.translatable(Util.makeDescriptionId("jukebox_song", p_344899_.identifier())), p_343603_, p_345058_)
      );
   }

   static void bootstrap(BootstrapContext<JukeboxSong> p_344061_) {
      register(p_344061_, THIRTEEN, SoundEvents.MUSIC_DISC_13, 178, 1);
      register(p_344061_, CAT, SoundEvents.MUSIC_DISC_CAT, 185, 2);
      register(p_344061_, BLOCKS, SoundEvents.MUSIC_DISC_BLOCKS, 345, 3);
      register(p_344061_, CHIRP, SoundEvents.MUSIC_DISC_CHIRP, 185, 4);
      register(p_344061_, FAR, SoundEvents.MUSIC_DISC_FAR, 174, 5);
      register(p_344061_, MALL, SoundEvents.MUSIC_DISC_MALL, 197, 6);
      register(p_344061_, MELLOHI, SoundEvents.MUSIC_DISC_MELLOHI, 96, 7);
      register(p_344061_, STAL, SoundEvents.MUSIC_DISC_STAL, 150, 8);
      register(p_344061_, STRAD, SoundEvents.MUSIC_DISC_STRAD, 188, 9);
      register(p_344061_, WARD, SoundEvents.MUSIC_DISC_WARD, 251, 10);
      register(p_344061_, ELEVEN, SoundEvents.MUSIC_DISC_11, 71, 11);
      register(p_344061_, WAIT, SoundEvents.MUSIC_DISC_WAIT, 238, 12);
      register(p_344061_, PIGSTEP, SoundEvents.MUSIC_DISC_PIGSTEP, 149, 13);
      register(p_344061_, OTHERSIDE, SoundEvents.MUSIC_DISC_OTHERSIDE, 195, 14);
      register(p_344061_, FIVE, SoundEvents.MUSIC_DISC_5, 178, 15);
      register(p_344061_, RELIC, SoundEvents.MUSIC_DISC_RELIC, 218, 14);
      register(p_344061_, PRECIPICE, SoundEvents.MUSIC_DISC_PRECIPICE, 299, 13);
      register(p_344061_, CREATOR, SoundEvents.MUSIC_DISC_CREATOR, 176, 12);
      register(p_344061_, CREATOR_MUSIC_BOX, SoundEvents.MUSIC_DISC_CREATOR_MUSIC_BOX, 73, 11);
      register(p_344061_, TEARS, SoundEvents.MUSIC_DISC_TEARS, 175, 10);
      register(p_344061_, LAVA_CHICKEN, SoundEvents.MUSIC_DISC_LAVA_CHICKEN, 134, 9);
   }
}
