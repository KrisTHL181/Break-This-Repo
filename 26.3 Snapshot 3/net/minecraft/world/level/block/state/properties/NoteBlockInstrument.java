package net.minecraft.world.level.block.state.properties;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringRepresentable;

public enum NoteBlockInstrument implements StringRepresentable {
   HARP("harp", SoundEvents.NOTE_BLOCK_HARP, NoteBlockInstrument.Type.BASE_BLOCK),
   BASEDRUM("basedrum", SoundEvents.NOTE_BLOCK_BASEDRUM, NoteBlockInstrument.Type.BASE_BLOCK),
   SNARE("snare", SoundEvents.NOTE_BLOCK_SNARE, NoteBlockInstrument.Type.BASE_BLOCK),
   HAT("hat", SoundEvents.NOTE_BLOCK_HAT, NoteBlockInstrument.Type.BASE_BLOCK),
   BASS("bass", SoundEvents.NOTE_BLOCK_BASS, NoteBlockInstrument.Type.BASE_BLOCK),
   FLUTE("flute", SoundEvents.NOTE_BLOCK_FLUTE, NoteBlockInstrument.Type.BASE_BLOCK),
   BELL("bell", SoundEvents.NOTE_BLOCK_BELL, NoteBlockInstrument.Type.BASE_BLOCK),
   GUITAR("guitar", SoundEvents.NOTE_BLOCK_GUITAR, NoteBlockInstrument.Type.BASE_BLOCK),
   CHIME("chime", SoundEvents.NOTE_BLOCK_CHIME, NoteBlockInstrument.Type.BASE_BLOCK),
   XYLOPHONE("xylophone", SoundEvents.NOTE_BLOCK_XYLOPHONE, NoteBlockInstrument.Type.BASE_BLOCK),
   IRON_XYLOPHONE("iron_xylophone", SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE, NoteBlockInstrument.Type.BASE_BLOCK),
   COW_BELL("cow_bell", SoundEvents.NOTE_BLOCK_COW_BELL, NoteBlockInstrument.Type.BASE_BLOCK),
   DIDGERIDOO("didgeridoo", SoundEvents.NOTE_BLOCK_DIDGERIDOO, NoteBlockInstrument.Type.BASE_BLOCK),
   BIT("bit", SoundEvents.NOTE_BLOCK_BIT, NoteBlockInstrument.Type.BASE_BLOCK),
   BANJO("banjo", SoundEvents.NOTE_BLOCK_BANJO, NoteBlockInstrument.Type.BASE_BLOCK),
   PLING("pling", SoundEvents.NOTE_BLOCK_PLING, NoteBlockInstrument.Type.BASE_BLOCK),
   TRUMPET("trumpet", SoundEvents.NOTE_BLOCK_TRUMPET, NoteBlockInstrument.Type.BASE_BLOCK),
   TRUMPET_EXPOSED("trumpet_exposed", SoundEvents.NOTE_BLOCK_TRUMPET_EXPOSED, NoteBlockInstrument.Type.BASE_BLOCK),
   TRUMPET_OXIDIZED("trumpet_oxidized", SoundEvents.NOTE_BLOCK_TRUMPET_OXIDIZED, NoteBlockInstrument.Type.BASE_BLOCK),
   TRUMPET_WEATHERED("trumpet_weathered", SoundEvents.NOTE_BLOCK_TRUMPET_WEATHERED, NoteBlockInstrument.Type.BASE_BLOCK),
   ZOMBIE("zombie", SoundEvents.NOTE_BLOCK_IMITATE_ZOMBIE, NoteBlockInstrument.Type.MOB_HEAD),
   SKELETON("skeleton", SoundEvents.NOTE_BLOCK_IMITATE_SKELETON, NoteBlockInstrument.Type.MOB_HEAD),
   CREEPER("creeper", SoundEvents.NOTE_BLOCK_IMITATE_CREEPER, NoteBlockInstrument.Type.MOB_HEAD),
   DRAGON("dragon", SoundEvents.NOTE_BLOCK_IMITATE_ENDER_DRAGON, NoteBlockInstrument.Type.MOB_HEAD),
   WITHER_SKELETON("wither_skeleton", SoundEvents.NOTE_BLOCK_IMITATE_WITHER_SKELETON, NoteBlockInstrument.Type.MOB_HEAD),
   PIGLIN("piglin", SoundEvents.NOTE_BLOCK_IMITATE_PIGLIN, NoteBlockInstrument.Type.MOB_HEAD),
   CUSTOM_HEAD("custom_head", SoundEvents.UI_BUTTON_CLICK, NoteBlockInstrument.Type.CUSTOM);

   private final String name;
   private final Holder<SoundEvent> soundEvent;
   private final NoteBlockInstrument.Type type;

   NoteBlockInstrument(final String name, final Holder<SoundEvent> soundEvent, final NoteBlockInstrument.Type type) {
      this.name = name;
      this.soundEvent = soundEvent;
      this.type = type;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   public Holder<SoundEvent> getSoundEvent() {
      return this.soundEvent;
   }

   public boolean isTunable() {
      return this.type == NoteBlockInstrument.Type.BASE_BLOCK;
   }

   public boolean hasCustomSound() {
      return this.type == NoteBlockInstrument.Type.CUSTOM;
   }

   public boolean worksAboveNoteBlock() {
      return this.type != NoteBlockInstrument.Type.BASE_BLOCK;
   }

   private enum Type {
      BASE_BLOCK,
      MOB_HEAD,
      CUSTOM;
   }
}
