package net.minecraft.util.profiling.jfr.event;

import jdk.jfr.Category;
import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

@Name("minecraft.StructureGeneration")
@Label("Structure Generation")
@Category({"Minecraft", "World Generation"})
@StackTrace(false)
@Enabled(false)
public class StructureGenerationEvent extends Event {
   public static final String EVENT_NAME = "minecraft.StructureGeneration";
   public static final EventType TYPE = EventType.getEventType(StructureGenerationEvent.class);
   @Name("chunkPosX")
   @Label("Chunk X Position")
   public final int chunkPosX;
   @Name("chunkPosZ")
   @Label("Chunk Z Position")
   public final int chunkPosZ;
   @Name("structure")
   @Label("Structure")
   public final String structure;
   @Name("level")
   @Label("Level")
   public final String level;
   @Name("success")
   @Label("Success")
   public boolean success;

   public StructureGenerationEvent(final ChunkPos sourceChunkPos, final Holder<Structure> structure, final ResourceKey<Level> level) {
      this.chunkPosX = sourceChunkPos.x();
      this.chunkPosZ = sourceChunkPos.z();
      this.structure = structure.getRegisteredName();
      this.level = level.identifier().toString();
   }

   public interface Fields {
      String CHUNK_POS_X = "chunkPosX";
      String CHUNK_POS_Z = "chunkPosZ";
      String STRUCTURE = "structure";
      String LEVEL = "level";
      String SUCCESS = "success";
   }
}
