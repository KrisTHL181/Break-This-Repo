package net.minecraft.util.profiling.jfr.event;

import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;

@Name("minecraft.ChunkRegionWrite")
@Label("Region File Write")
public class ChunkRegionWriteEvent extends ChunkRegionIoEvent {
   public static final String EVENT_NAME = "minecraft.ChunkRegionWrite";
   public static final EventType TYPE = EventType.getEventType(ChunkRegionWriteEvent.class);

   public ChunkRegionWriteEvent(final RegionStorageInfo info, final ChunkPos chunkPos, final RegionFileVersion version, final int bytes) {
      super(info, chunkPos, version, bytes);
   }
}
