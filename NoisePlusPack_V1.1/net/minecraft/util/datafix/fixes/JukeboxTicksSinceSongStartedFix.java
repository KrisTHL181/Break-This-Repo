package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class JukeboxTicksSinceSongStartedFix extends NamedEntityFix {
   public JukeboxTicksSinceSongStartedFix(Schema p_342295_) {
      super(p_342295_, false, "JukeboxTicksSinceSongStartedFix", References.BLOCK_ENTITY, "minecraft:jukebox");
   }

   public Dynamic<?> fixTag(Dynamic<?> p_344094_) {
      long i = p_344094_.get("TickCount").asLong(0L) - p_344094_.get("RecordStartTick").asLong(0L);
      Dynamic<?> dynamic = p_344094_.remove("IsPlaying").remove("TickCount").remove("RecordStartTick");
      return i > 0L ? dynamic.set("ticks_since_song_started", p_344094_.createLong(i)) : dynamic;
   }

   @Override
   protected Typed<?> fix(Typed<?> p_344432_) {
      return p_344432_.update(DSL.remainderFinder(), this::fixTag);
   }
}
