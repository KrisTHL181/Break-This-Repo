package net.minecraft.client.renderer.debug;

import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

public class HeightMapRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private static final int CHUNK_DIST = 2;
   private static final float BOX_HEIGHT = 0.09375F;

   public HeightMapRenderer(final Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   @Override
   public void emitGizmos(
      final double camX, final double camY, final double camZ, final DebugValueAccess debugValues, final Frustum frustum, final float partialTicks
   ) {
      LevelAccessor level = this.minecraft.level;
      BlockPos playerPos = BlockPos.containing(camX, 0.0, camZ);

      for (int chunkX = -2; chunkX <= 2; chunkX++) {
         for (int chunkZ = -2; chunkZ <= 2; chunkZ++) {
            ChunkAccess chunk = level.getChunk(playerPos.offset(chunkX * 16, 0, chunkZ * 16));

            for (Entry<Heightmap.Types, Heightmap> heightmapEntry : chunk.getHeightmaps()) {
               Heightmap.Types type = heightmapEntry.getKey();
               ChunkPos chunkPos = chunk.getPos();
               Vector3f color = this.getColor(type);

               for (int relativeX = 0; relativeX < 16; relativeX++) {
                  for (int relativeZ = 0; relativeZ < 16; relativeZ++) {
                     int xx = SectionPos.sectionToBlockCoord(chunkPos.x(), relativeX);
                     int zz = SectionPos.sectionToBlockCoord(chunkPos.z(), relativeZ);
                     float height = level.getHeight(type, xx, zz) + type.ordinal() * 0.09375F;
                     Gizmos.cuboid(
                        new AABB(xx + 0.25F, height, zz + 0.25F, xx + 0.75F, height + 0.09375F, zz + 0.75F),
                        GizmoStyle.fill(ARGB.colorFromFloat(1.0F, color.x(), color.y(), color.z()))
                     );
                  }
               }
            }
         }
      }
   }

   private Vector3f getColor(final Heightmap.Types type) {
      return switch (type) {
         case WORLD_SURFACE_WG -> new Vector3f(1.0F, 1.0F, 0.0F);
         case OCEAN_FLOOR_WG -> new Vector3f(1.0F, 0.0F, 1.0F);
         case WORLD_SURFACE -> new Vector3f(0.0F, 0.7F, 0.0F);
         case OCEAN_FLOOR -> new Vector3f(0.0F, 0.0F, 0.5F);
         case MOTION_BLOCKING -> new Vector3f(0.0F, 0.3F, 0.3F);
         case MOTION_BLOCKING_NO_LEAVES -> new Vector3f(0.0F, 0.5F, 0.5F);
      };
   }
}
