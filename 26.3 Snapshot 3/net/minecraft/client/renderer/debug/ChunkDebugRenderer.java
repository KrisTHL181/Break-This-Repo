package net.minecraft.client.renderer.debug;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ChunkDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private double lastUpdateTime = Double.MIN_VALUE;
   private final int radius = 12;
   private ChunkDebugRenderer.@Nullable ChunkData data;

   public ChunkDebugRenderer(final Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   @Override
   public void emitGizmos(
      final double camX, final double camY, final double camZ, final DebugValueAccess debugValues, final Frustum frustum, final float partialTicks
   ) {
      double time = Util.getNanos();
      if (time - this.lastUpdateTime > 3.0E9) {
         this.lastUpdateTime = time;
         IntegratedServer server = this.minecraft.getSingleplayerServer();
         if (server != null) {
            this.data = new ChunkDebugRenderer.ChunkData(server, camX, camZ);
         } else {
            this.data = null;
         }
      }

      if (this.data != null) {
         Map<ChunkPos, String> serverData = this.data.serverData.getNow(null);
         double y = this.minecraft.gameRenderer.mainCamera().position().y * 0.85;

         for (Entry<ChunkPos, String> entry : this.data.clientData.entrySet()) {
            ChunkPos pos = entry.getKey();
            String value = entry.getValue();
            if (serverData != null) {
               value = value + serverData.get(pos);
            }

            String[] parts = value.split("\n");
            int yOffset = 0;

            for (String part : parts) {
               Gizmos.billboardText(
                     part,
                     new Vec3(SectionPos.sectionToBlockCoord(pos.x(), 8), y + yOffset, SectionPos.sectionToBlockCoord(pos.z(), 8)),
                     TextGizmo.Style.whiteAndCentered().withScale(2.4F)
                  )
                  .setAlwaysOnTop();
               yOffset -= 2;
            }
         }
      }
   }

   private final class ChunkData {
      private final Map<ChunkPos, String> clientData;
      private final CompletableFuture<Map<ChunkPos, String>> serverData;

      private ChunkData(final IntegratedServer server, final double camX, final double camZ) {
         ClientLevel clientLevel = ChunkDebugRenderer.this.minecraft.level;
         ResourceKey<Level> dimension = clientLevel.dimension();
         int cx = SectionPos.posToSectionCoord(camX);
         int cz = SectionPos.posToSectionCoord(camZ);
         Builder<ChunkPos, String> builder = ImmutableMap.builder();
         ClientChunkCache clientChunkSource = clientLevel.getChunkSource();

         for (int x = cx - 12; x <= cx + 12; x++) {
            for (int z = cz - 12; z <= cz + 12; z++) {
               ChunkPos pos = new ChunkPos(x, z);
               String result = "";
               LevelChunk clientChunk = clientChunkSource.getChunk(x, z, false);
               result = result + "Client: ";
               if (clientChunk == null) {
                  result = result + "0n/a\n";
               } else {
                  result = result + (clientChunk.isEmpty() ? " E" : "");
                  result = result + "\n";
               }

               builder.put(pos, result);
            }
         }

         this.clientData = builder.build();
         this.serverData = server.submit(() -> {
            ServerLevel serverLevel = server.getLevel(dimension);
            if (serverLevel == null) {
               return ImmutableMap.of();
            }

            Builder<ChunkPos, String> serverBuilder = ImmutableMap.builder();
            ServerChunkCache serverChunkSource = serverLevel.getChunkSource();

            for (int xx = cx - 12; xx <= cx + 12; xx++) {
               for (int zx = cz - 12; zx <= cz + 12; zx++) {
                  ChunkPos posx = new ChunkPos(xx, zx);
                  serverBuilder.put(posx, "Server: " + serverChunkSource.getChunkDebugData(posx));
               }
            }

            return serverBuilder.build();
         });
      }
   }
}
