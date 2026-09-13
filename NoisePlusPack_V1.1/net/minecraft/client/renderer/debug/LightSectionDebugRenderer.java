package net.minecraft.client.renderer.debug;

import java.time.Duration;
import java.time.Instant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class LightSectionDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final Duration REFRESH_INTERVAL = Duration.ofMillis(500L);
   private static final int RADIUS = 10;
   private static final int LIGHT_AND_BLOCKS_COLOR = ARGB.colorFromFloat(0.25F, 1.0F, 1.0F, 0.0F);
   private static final int LIGHT_ONLY_COLOR = ARGB.colorFromFloat(0.125F, 0.25F, 0.125F, 0.0F);
   private final Minecraft minecraft;
   private final LightLayer lightLayer;
   private Instant lastUpdateTime = Instant.now();
   private LightSectionDebugRenderer.@Nullable SectionData data;

   public LightSectionDebugRenderer(Minecraft p_283340_, LightLayer p_283096_) {
      this.minecraft = p_283340_;
      this.lightLayer = p_283096_;
   }

   @Override
   public void emitGizmos(double p_459292_, double p_457733_, double p_451851_, DebugValueAccess p_455876_, Frustum p_457704_, float p_451260_) {
      Instant instant = Instant.now();
      if (this.data == null || Duration.between(this.lastUpdateTime, instant).compareTo(REFRESH_INTERVAL) > 0) {
         this.lastUpdateTime = instant;
         this.data = new LightSectionDebugRenderer.SectionData(
            this.minecraft.level.getLightEngine(), SectionPos.of(this.minecraft.player.blockPosition()), 10, this.lightLayer
         );
      }

      renderEdges(this.data.lightAndBlocksShape, this.data.minPos, LIGHT_AND_BLOCKS_COLOR);
      renderEdges(this.data.lightShape, this.data.minPos, LIGHT_ONLY_COLOR);
      renderFaces(this.data.lightAndBlocksShape, this.data.minPos, LIGHT_AND_BLOCKS_COLOR);
      renderFaces(this.data.lightShape, this.data.minPos, LIGHT_ONLY_COLOR);
   }

   private static void renderFaces(DiscreteVoxelShape p_281747_, SectionPos p_282941_, int p_459341_) {
      p_281747_.forAllFaces((p_448272_, p_448273_, p_448274_, p_448275_) -> {
         int i = p_448273_ + p_282941_.getX();
         int j = p_448274_ + p_282941_.getY();
         int k = p_448275_ + p_282941_.getZ();
         renderFace(p_448272_, i, j, k, p_459341_);
      });
   }

   private static void renderEdges(DiscreteVoxelShape p_282950_, SectionPos p_281925_, int p_451683_) {
      p_282950_.forAllEdges((p_448264_, p_448265_, p_448266_, p_448267_, p_448268_, p_448269_) -> {
         int i = p_448264_ + p_281925_.getX();
         int j = p_448265_ + p_281925_.getY();
         int k = p_448266_ + p_281925_.getZ();
         int l = p_448267_ + p_281925_.getX();
         int i1 = p_448268_ + p_281925_.getY();
         int j1 = p_448269_ + p_281925_.getZ();
         renderEdge(i, j, k, l, i1, j1, p_451683_);
      }, true);
   }

   private static void renderFace(Direction p_282340_, int p_282751_, int p_282270_, int p_282159_, int p_455595_) {
      Vec3 vec3 = new Vec3(SectionPos.sectionToBlockCoord(p_282751_), SectionPos.sectionToBlockCoord(p_282270_), SectionPos.sectionToBlockCoord(p_282159_));
      Vec3 vec31 = vec3.add(16.0, 16.0, 16.0);
      Gizmos.rect(vec3, vec31, p_282340_, GizmoStyle.fill(p_455595_));
   }

   private static void renderEdge(int p_281439_, int p_282106_, int p_282462_, int p_282216_, int p_281474_, int p_281542_, int p_455620_) {
      double d0 = SectionPos.sectionToBlockCoord(p_281439_);
      double d1 = SectionPos.sectionToBlockCoord(p_282106_);
      double d2 = SectionPos.sectionToBlockCoord(p_282462_);
      double d3 = SectionPos.sectionToBlockCoord(p_282216_);
      double d4 = SectionPos.sectionToBlockCoord(p_281474_);
      double d5 = SectionPos.sectionToBlockCoord(p_281542_);
      int i = ARGB.opaque(p_455620_);
      Gizmos.line(new Vec3(d0, d1, d2), new Vec3(d3, d4, d5), i);
   }

   @OnlyIn(Dist.CLIENT)
   static final class SectionData {
      final DiscreteVoxelShape lightAndBlocksShape;
      final DiscreteVoxelShape lightShape;
      final SectionPos minPos;

      SectionData(LevelLightEngine p_283220_, SectionPos p_282370_, int p_282804_, LightLayer p_283151_) {
         int i = p_282804_ * 2 + 1;
         this.lightAndBlocksShape = new BitSetDiscreteVoxelShape(i, i, i);
         this.lightShape = new BitSetDiscreteVoxelShape(i, i, i);

         for (int j = 0; j < i; j++) {
            for (int k = 0; k < i; k++) {
               for (int l = 0; l < i; l++) {
                  SectionPos sectionpos = SectionPos.of(p_282370_.x() + l - p_282804_, p_282370_.y() + k - p_282804_, p_282370_.z() + j - p_282804_);
                  LayerLightSectionStorage.SectionType layerlightsectionstorage$sectiontype = p_283220_.getDebugSectionType(p_283151_, sectionpos);
                  if (layerlightsectionstorage$sectiontype == LayerLightSectionStorage.SectionType.LIGHT_AND_DATA) {
                     this.lightAndBlocksShape.fill(l, k, j);
                     this.lightShape.fill(l, k, j);
                  } else if (layerlightsectionstorage$sectiontype == LayerLightSectionStorage.SectionType.LIGHT_ONLY) {
                     this.lightShape.fill(l, k, j);
                  }
               }
            }
         }

         this.minPos = SectionPos.of(p_282370_.x() - p_282804_, p_282370_.y() - p_282804_, p_282370_.z() - p_282804_);
      }
   }
}
