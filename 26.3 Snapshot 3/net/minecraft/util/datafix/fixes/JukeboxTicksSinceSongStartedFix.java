package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class JukeboxTicksSinceSongStartedFix extends NamedEntityFix {
   public JukeboxTicksSinceSongStartedFix(final Schema outputSchema) {
      super(outputSchema, false, "JukeboxTicksSinceSongStartedFix", References.BLOCK_ENTITY, "minecraft:jukebox");
   }

   public Dynamic<?> fixTag(final Dynamic<?> input) {
      long ticksSinceSongStarted = input.get("TickCount").asLong(0L) - input.get("RecordStartTick").asLong(0L);
      Dynamic<?> result = input.remove("IsPlaying").remove("TickCount").remove("RecordStartTick");
      return ticksSinceSongStarted > 0L ? result.set("ticks_since_song_started", input.createLong(ticksSinceSongStarted)) : result;
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), this::fixTag);
   }
}
