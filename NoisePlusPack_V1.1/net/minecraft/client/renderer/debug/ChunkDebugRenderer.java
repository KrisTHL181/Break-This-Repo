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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ChunkDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   final Minecraft minecraft;
   private double lastUpdateTime = Double.MIN_VALUE;
   private final int radius = 12;
   private ChunkDebugRenderer.@Nullable ChunkData data;

   public ChunkDebugRenderer(Minecraft p_113368_) {
      this.minecraft = p_113368_;
   }

   @Override
   public void emitGizmos(double p_458497_, double p_451985_, double p_457301_, DebugValueAccess p_452270_, Frustum p_452468_, float p_456987_) {
      double d0 = Util.getNanos();
      if (d0 - this.lastUpdateTime > 3.0E9) {
         this.lastUpdateTime = d0;
         IntegratedServer integratedserver = this.minecraft.getSingleplayerServer();
         if (integratedserver != null) {
            this.data = new ChunkDebugRenderer.ChunkData(integratedserver, p_458497_, p_457301_);
         } else {
            this.data = null;
         }
      }

      if (this.data != null) {
         Map<ChunkPos, String> map = this.data.serverData.getNow(null);
         double d1 = this.minecraft.gameRenderer.getMainCamera().position().y * 0.85;

         for (Entry<ChunkPos, String> entry : this.data.clientData.entrySet()) {
            ChunkPos chunkpos = entry.getKey();
            String s = entry.getValue();
            if (map != null) {
               s = s + map.get(chunkpos);
            }

            String[] astring = s.split("\n");
            int i = 0;

            for (String s1 : astring) {
               Gizmos.billboardText(
                     s1,
                     new Vec3(SectionPos.sectionToBlockCoord(chunkpos.x, 8), d1 + i, SectionPos.sectionToBlockCoord(chunkpos.z, 8)),
                     TextGizmo.Style.whiteAndCentered().withScale(2.4F)
                  )
                  .setAlwaysOnTop();
               i -= 2;
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   final class ChunkData {
      final Map<ChunkPos, String> clientData;
      final CompletableFuture<Map<ChunkPos, String>> serverData;

      ChunkData(final IntegratedServer p_113382_, final double p_113383_, final double p_113384_) {
         ClientLevel clientlevel = ChunkDebugRenderer.this.minecraft.level;
         ResourceKey<Level> resourcekey = clientlevel.dimension();
         int i = SectionPos.posToSectionCoord(p_113383_);
         int j = SectionPos.posToSectionCoord(p_113384_);
         Builder<ChunkPos, String> builder = ImmutableMap.builder();
         ClientChunkCache clientchunkcache = clientlevel.getChunkSource();

         for (int k = i - 12; k <= i + 12; k++) {
            for (int l = j - 12; l <= j + 12; l++) {
               ChunkPos chunkpos = new ChunkPos(k, l);
               String s = "";
               LevelChunk levelchunk = clientchunkcache.getChunk(k, l, false);
               s = s + "Client: ";
               if (levelchunk == null) {
                  s = s + "0n/a\n";
               } else {
                  s = s + (levelchunk.isEmpty() ? " E" : "");
                  s = s + "\n";
               }

               builder.put(chunkpos, s);
            }
         }

         this.clientData = builder.build();
         this.serverData = p_113382_.submit(() -> {
            ServerLevel serverlevel = p_113382_.getLevel(resourcekey);
            if (serverlevel == null) {
               return ImmutableMap.of();
            }

            Builder<ChunkPos, String> builder1 = ImmutableMap.builder();
            ServerChunkCache serverchunkcache = serverlevel.getChunkSource();

            for (int i1 = i - 12; i1 <= i + 12; i1++) {
               for (int j1 = j - 12; j1 <= j + 12; j1++) {
                  ChunkPos chunkpos1 = new ChunkPos(i1, j1);
                  builder1.put(chunkpos1, "Server: " + serverchunkcache.getChunkDebugData(chunkpos1));
               }
            }

            return builder1.build();
         });
      }
   }
}
