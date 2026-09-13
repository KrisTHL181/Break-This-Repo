package net.minecraft.util.profiling.jfr.event;

import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;

@Name("minecraft.ChunkRegionRead")
@Label("Region File Read")
public class ChunkRegionReadEvent extends ChunkRegionIoEvent {
   public static final String EVENT_NAME = "minecraft.ChunkRegionRead";
   public static final EventType TYPE = EventType.getEventType(ChunkRegionReadEvent.class);

   public ChunkRegionReadEvent(final RegionStorageInfo info, final ChunkPos chunkPos, final RegionFileVersion version, final int bytes) {
      super(info, chunkPos, version, bytes);
   }
}
