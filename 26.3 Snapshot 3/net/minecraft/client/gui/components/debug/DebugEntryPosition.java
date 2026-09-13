package net.minecraft.client.gui.components.debug;

import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public class DebugEntryPosition implements DebugScreenEntry {
   public static final Identifier GROUP = Identifier.withDefaultNamespace("position");

   @Override
   public void display(
      final DebugScreenDisplayer displayer,
      final @Nullable Level serverOrClientLevel,
      final @Nullable LevelChunk clientChunk,
      final @Nullable LevelChunk serverChunk
   ) {
      Minecraft minecraft = Minecraft.getInstance();
      Entity entity = minecraft.getCameraEntity();
      if (entity != null) {
         BlockPos feetPos = minecraft.getCameraEntity().blockPosition();
         ChunkPos chunkPos = ChunkPos.containing(feetPos);
         Direction direction = entity.getDirection();

         String faceString = switch (direction) {
            case NORTH -> "Towards negative Z";
            case SOUTH -> "Towards positive Z";
            case WEST -> "Towards negative X";
            case EAST -> "Towards positive X";
            default -> "Invalid";
         };
         LongSet chunks = (LongSet)(serverOrClientLevel instanceof ServerLevel serverLevel ? serverLevel.getForceLoadedChunks() : LongSets.EMPTY_SET);
         displayer.addToGroup(
            GROUP,
            List.of(
               String.format(
                  Locale.ROOT,
                  "XYZ: %.10f / %.12f / %.10f",
                  minecraft.getCameraEntity().getX(),
                  minecraft.getCameraEntity().getY(),
                  minecraft.getCameraEntity().getZ()
               ),
               String.format(Locale.ROOT, "Block: %d %d %d", feetPos.getX(), feetPos.getY(), feetPos.getZ()),
               String.format(
                  Locale.ROOT,
                  "Chunk: %d %d %d [%d %d in r.%d.%d.mca]",
                  chunkPos.x(),
                  SectionPos.blockToSectionCoord(feetPos.getY()),
                  chunkPos.z(),
                  chunkPos.getRegionLocalX(),
                  chunkPos.getRegionLocalZ(),
                  chunkPos.getRegionX(),
                  chunkPos.getRegionZ()
               ),
               String.format(
                  Locale.ROOT, "Facing: %s (%s) (%.1f / %.1f)", direction, faceString, Mth.wrapDegrees(entity.getYRot()), Mth.wrapDegrees(entity.getXRot())
               ),
               minecraft.level.dimension().identifier() + " FC: " + chunks.size()
            )
         );
      }
   }
}
