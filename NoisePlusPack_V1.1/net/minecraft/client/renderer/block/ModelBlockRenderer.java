package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import java.util.List;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class ModelBlockRenderer {
   private static final Direction[] DIRECTIONS = Direction.values();
   private final BlockColors blockColors;
   private static final int CACHE_SIZE = 100;
   static final ThreadLocal<ModelBlockRenderer.Cache> CACHE = ThreadLocal.withInitial(ModelBlockRenderer.Cache::new);

   public ModelBlockRenderer(BlockColors p_110999_) {
      this.blockColors = p_110999_;
   }

   public void tesselateBlock(
      BlockAndTintGetter p_234380_,
      List<BlockModelPart> p_393688_,
      BlockState p_234382_,
      BlockPos p_234383_,
      PoseStack p_234384_,
      VertexConsumer p_234385_,
      boolean p_234386_,
      int p_234389_
   ) {
      if (!p_393688_.isEmpty()) {
         boolean flag = Minecraft.useAmbientOcclusion() && p_234382_.getLightEmission() == 0 && p_393688_.getFirst().useAmbientOcclusion();
         p_234384_.translate(p_234382_.getOffset(p_234383_));

         try {
            if (flag) {
               this.tesselateWithAO(p_234380_, p_393688_, p_234382_, p_234383_, p_234384_, p_234385_, p_234386_, p_234389_);
            } else {
               this.tesselateWithoutAO(p_234380_, p_393688_, p_234382_, p_234383_, p_234384_, p_234385_, p_234386_, p_234389_);
            }
         } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Tesselating block model");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block model being tesselated");
            CrashReportCategory.populateBlockDetails(crashreportcategory, p_234380_, p_234383_, p_234382_);
            crashreportcategory.setDetail("Using AO", flag);
            throw new ReportedException(crashreport);
         }
      }
   }

   private static boolean shouldRenderFace(BlockAndTintGetter p_391703_, BlockState p_396742_, boolean p_394639_, Direction p_397078_, BlockPos p_395848_) {
      if (!p_394639_) {
         return true;
      }

      BlockState blockstate = p_391703_.getBlockState(p_395848_);
      return Block.shouldRenderFace(p_396742_, blockstate, p_397078_);
   }

   public void tesselateWithAO(
      BlockAndTintGetter p_234391_,
      List<BlockModelPart> p_395824_,
      BlockState p_234393_,
      BlockPos p_234394_,
      PoseStack p_234395_,
      VertexConsumer p_234396_,
      boolean p_234397_,
      int p_234400_
   ) {
      ModelBlockRenderer.AmbientOcclusionRenderStorage modelblockrenderer$ambientocclusionrenderstorage = new ModelBlockRenderer.AmbientOcclusionRenderStorage();
      int i = 0;
      int j = 0;

      for (BlockModelPart blockmodelpart : p_395824_) {
         for (Direction direction : DIRECTIONS) {
            int k = 1 << direction.ordinal();
            boolean flag = (i & k) == 1;
            boolean flag1 = (j & k) == 1;
            if (!flag || flag1) {
               List<BakedQuad> list = blockmodelpart.getQuads(direction);
               if (!list.isEmpty()) {
                  if (!flag) {
                     flag1 = shouldRenderFace(
                        p_234391_,
                        p_234393_,
                        p_234397_,
                        direction,
                        modelblockrenderer$ambientocclusionrenderstorage.scratchPos.setWithOffset(p_234394_, direction)
                     );
                     i |= k;
                     if (flag1) {
                        j |= k;
                     }
                  }

                  if (flag1) {
                     this.renderModelFaceAO(
                        p_234391_, p_234393_, p_234394_, p_234395_, p_234396_, list, modelblockrenderer$ambientocclusionrenderstorage, p_234400_
                     );
                  }
               }
            }
         }

         List<BakedQuad> list1 = blockmodelpart.getQuads(null);
         if (!list1.isEmpty()) {
            this.renderModelFaceAO(p_234391_, p_234393_, p_234394_, p_234395_, p_234396_, list1, modelblockrenderer$ambientocclusionrenderstorage, p_234400_);
         }
      }
   }

   public void tesselateWithoutAO(
      BlockAndTintGetter p_234402_,
      List<BlockModelPart> p_394148_,
      BlockState p_234404_,
      BlockPos p_234405_,
      PoseStack p_234406_,
      VertexConsumer p_234407_,
      boolean p_234408_,
      int p_234411_
   ) {
      ModelBlockRenderer.CommonRenderStorage modelblockrenderer$commonrenderstorage = new ModelBlockRenderer.CommonRenderStorage();
      int i = 0;
      int j = 0;

      for (BlockModelPart blockmodelpart : p_394148_) {
         for (Direction direction : DIRECTIONS) {
            int k = 1 << direction.ordinal();
            boolean flag = (i & k) == 1;
            boolean flag1 = (j & k) == 1;
            if (!flag || flag1) {
               List<BakedQuad> list = blockmodelpart.getQuads(direction);
               if (!list.isEmpty()) {
                  BlockPos blockpos = modelblockrenderer$commonrenderstorage.scratchPos.setWithOffset(p_234405_, direction);
                  if (!flag) {
                     flag1 = shouldRenderFace(p_234402_, p_234404_, p_234408_, direction, blockpos);
                     i |= k;
                     if (flag1) {
                        j |= k;
                     }
                  }

                  if (flag1) {
                     int l = modelblockrenderer$commonrenderstorage.cache.getLightColor(p_234404_, p_234402_, blockpos);
                     this.renderModelFaceFlat(
                        p_234402_, p_234404_, p_234405_, l, p_234411_, false, p_234406_, p_234407_, list, modelblockrenderer$commonrenderstorage
                     );
                  }
               }
            }
         }

         List<BakedQuad> list1 = blockmodelpart.getQuads(null);
         if (!list1.isEmpty()) {
            this.renderModelFaceFlat(p_234402_, p_234404_, p_234405_, -1, p_234411_, true, p_234406_, p_234407_, list1, modelblockrenderer$commonrenderstorage);
         }
      }
   }

   private void renderModelFaceAO(
      BlockAndTintGetter p_111013_,
      BlockState p_111014_,
      BlockPos p_111015_,
      PoseStack p_111016_,
      VertexConsumer p_111017_,
      List<BakedQuad> p_111018_,
      ModelBlockRenderer.AmbientOcclusionRenderStorage p_397799_,
      int p_111022_
   ) {
      for (BakedQuad bakedquad : p_111018_) {
         calculateShape(p_111013_, p_111014_, p_111015_, bakedquad, p_397799_);
         p_397799_.calculate(p_111013_, p_111014_, p_111015_, bakedquad.direction(), bakedquad.shade());
         this.putQuadData(p_111013_, p_111014_, p_111015_, p_111017_, p_111016_.last(), bakedquad, p_397799_, p_111022_);
      }
   }

   private void putQuadData(
      BlockAndTintGetter p_111024_,
      BlockState p_111025_,
      BlockPos p_111026_,
      VertexConsumer p_111027_,
      PoseStack.Pose p_111028_,
      BakedQuad p_111029_,
      ModelBlockRenderer.CommonRenderStorage p_391833_,
      int p_111034_
   ) {
      int i = p_111029_.tintIndex();
      float f;
      float f1;
      float f2;
      if (i != -1) {
         int j;
         if (p_391833_.tintCacheIndex == i) {
            j = p_391833_.tintCacheValue;
         } else {
            j = this.blockColors.getColor(p_111025_, p_111024_, p_111026_, i);
            p_391833_.tintCacheIndex = i;
            p_391833_.tintCacheValue = j;
         }

         f = ARGB.redFloat(j);
         f1 = ARGB.greenFloat(j);
         f2 = ARGB.blueFloat(j);
      } else {
         f = 1.0F;
         f1 = 1.0F;
         f2 = 1.0F;
      }

      p_111027_.putBulkData(p_111028_, p_111029_, p_391833_.brightness, f, f1, f2, 1.0F, p_391833_.lightmap, p_111034_);
   }

   private static void calculateShape(
      BlockAndTintGetter p_111040_, BlockState p_111041_, BlockPos p_111042_, BakedQuad p_454292_, ModelBlockRenderer.CommonRenderStorage p_394767_
   ) {
      float f = 32.0F;
      float f1 = 32.0F;
      float f2 = 32.0F;
      float f3 = -32.0F;
      float f4 = -32.0F;
      float f5 = -32.0F;

      for (int i = 0; i < 4; i++) {
         Vector3fc vector3fc = p_454292_.position(i);
         float f6 = vector3fc.x();
         float f7 = vector3fc.y();
         float f8 = vector3fc.z();
         f = Math.min(f, f6);
         f1 = Math.min(f1, f7);
         f2 = Math.min(f2, f8);
         f3 = Math.max(f3, f6);
         f4 = Math.max(f4, f7);
         f5 = Math.max(f5, f8);
      }

      if (p_394767_ instanceof ModelBlockRenderer.AmbientOcclusionRenderStorage modelblockrenderer$ambientocclusionrenderstorage) {
         modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.WEST.index] = f;
         modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.EAST.index] = f3;
         modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.DOWN.index] = f1;
         modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.UP.index] = f4;
         modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.NORTH.index] = f2;
         modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.SOUTH.index] = f5;
         modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_WEST.index] = 1.0F - f;
         modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_EAST.index] = 1.0F - f3;
         modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_DOWN.index] = 1.0F - f1;
         modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_UP.index] = 1.0F - f4;
         modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_NORTH.index] = 1.0F - f2;
         modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_SOUTH.index] = 1.0F - f5;
      }

      float f9 = 1.0E-4F;
      float f10 = 0.9999F;

      p_394767_.facePartial = switch (p_454292_.direction()) {
         case DOWN, UP -> f >= 1.0E-4F || f2 >= 1.0E-4F || f3 <= 0.9999F || f5 <= 0.9999F;
         case NORTH, SOUTH -> f >= 1.0E-4F || f1 >= 1.0E-4F || f3 <= 0.9999F || f4 <= 0.9999F;
         case WEST, EAST -> f1 >= 1.0E-4F || f2 >= 1.0E-4F || f4 <= 0.9999F || f5 <= 0.9999F;
      };

      p_394767_.faceCubic = switch (p_454292_.direction()) {
         case DOWN -> f1 == f4 && (f1 < 1.0E-4F || p_111041_.isCollisionShapeFullBlock(p_111040_, p_111042_));
         case UP -> f1 == f4 && (f4 > 0.9999F || p_111041_.isCollisionShapeFullBlock(p_111040_, p_111042_));
         case NORTH -> f2 == f5 && (f2 < 1.0E-4F || p_111041_.isCollisionShapeFullBlock(p_111040_, p_111042_));
         case SOUTH -> f2 == f5 && (f5 > 0.9999F || p_111041_.isCollisionShapeFullBlock(p_111040_, p_111042_));
         case WEST -> f == f3 && (f < 1.0E-4F || p_111041_.isCollisionShapeFullBlock(p_111040_, p_111042_));
         case EAST -> f == f3 && (f3 > 0.9999F || p_111041_.isCollisionShapeFullBlock(p_111040_, p_111042_));
      };
   }

   private void renderModelFaceFlat(
      BlockAndTintGetter p_111002_,
      BlockState p_111003_,
      BlockPos p_111004_,
      int p_111005_,
      int p_111006_,
      boolean p_111007_,
      PoseStack p_111008_,
      VertexConsumer p_111009_,
      List<BakedQuad> p_111010_,
      ModelBlockRenderer.CommonRenderStorage p_396196_
   ) {
      for (BakedQuad bakedquad : p_111010_) {
         if (p_111007_) {
            calculateShape(p_111002_, p_111003_, p_111004_, bakedquad, p_396196_);
            BlockPos blockpos = p_396196_.faceCubic ? p_396196_.scratchPos.setWithOffset(p_111004_, bakedquad.direction()) : p_111004_;
            p_111005_ = p_396196_.cache.getLightColor(p_111003_, p_111002_, blockpos);
         }

         float f = p_111002_.getShade(bakedquad.direction(), bakedquad.shade());
         p_396196_.brightness[0] = f;
         p_396196_.brightness[1] = f;
         p_396196_.brightness[2] = f;
         p_396196_.brightness[3] = f;
         p_396196_.lightmap[0] = p_111005_;
         p_396196_.lightmap[1] = p_111005_;
         p_396196_.lightmap[2] = p_111005_;
         p_396196_.lightmap[3] = p_111005_;
         this.putQuadData(p_111002_, p_111003_, p_111004_, p_111009_, p_111008_.last(), bakedquad, p_396196_, p_111006_);
      }
   }

   public static void renderModel(
      PoseStack.Pose p_111068_,
      VertexConsumer p_111069_,
      BlockStateModel p_397754_,
      float p_111072_,
      float p_111073_,
      float p_111074_,
      int p_111075_,
      int p_111076_
   ) {
      for (BlockModelPart blockmodelpart : p_397754_.collectParts(RandomSource.create(42L))) {
         for (Direction direction : DIRECTIONS) {
            renderQuadList(p_111068_, p_111069_, p_111072_, p_111073_, p_111074_, blockmodelpart.getQuads(direction), p_111075_, p_111076_);
         }

         renderQuadList(p_111068_, p_111069_, p_111072_, p_111073_, p_111074_, blockmodelpart.getQuads(null), p_111075_, p_111076_);
      }
   }

   private static void renderQuadList(
      PoseStack.Pose p_111059_,
      VertexConsumer p_111060_,
      float p_111061_,
      float p_111062_,
      float p_111063_,
      List<BakedQuad> p_111064_,
      int p_111065_,
      int p_111066_
   ) {
      for (BakedQuad bakedquad : p_111064_) {
         float f;
         float f1;
         float f2;
         if (bakedquad.isTinted()) {
            f = Mth.clamp(p_111061_, 0.0F, 1.0F);
            f1 = Mth.clamp(p_111062_, 0.0F, 1.0F);
            f2 = Mth.clamp(p_111063_, 0.0F, 1.0F);
         } else {
            f = 1.0F;
            f1 = 1.0F;
            f2 = 1.0F;
         }

         p_111060_.putBulkData(p_111059_, bakedquad, f, f1, f2, 1.0F, p_111065_, p_111066_);
      }
   }

   public static void enableCaching() {
      CACHE.get().enable();
   }

   public static void clearCache() {
      CACHE.get().disable();
   }

   @OnlyIn(Dist.CLIENT)
   protected enum AdjacencyInfo {
      DOWN(
         new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH},
         0.5F,
         true,
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.SOUTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.NORTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.NORTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.SOUTH
         }
      ),
      UP(
         new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH},
         1.0F,
         true,
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.SOUTH,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.SOUTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.NORTH,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.NORTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.NORTH,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.NORTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.SOUTH,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.SOUTH
         }
      ),
      NORTH(
         new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST},
         0.8F,
         true,
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.FLIP_WEST
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.FLIP_EAST
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_EAST
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_WEST
         }
      ),
      SOUTH(
         new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP},
         0.8F,
         true,
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.WEST
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_WEST,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.WEST,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.WEST
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.EAST
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.FLIP_EAST,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.EAST,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.EAST
         }
      ),
      WEST(
         new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH},
         0.6F,
         true,
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.SOUTH,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.SOUTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.NORTH,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.NORTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.NORTH,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.NORTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.SOUTH,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.SOUTH
         }
      ),
      EAST(
         new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH},
         0.6F,
         true,
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.SOUTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.DOWN,
            ModelBlockRenderer.SizeInfo.NORTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.NORTH,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.FLIP_NORTH,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.NORTH
         },
         new ModelBlockRenderer.SizeInfo[]{
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.SOUTH,
            ModelBlockRenderer.SizeInfo.FLIP_UP,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
            ModelBlockRenderer.SizeInfo.UP,
            ModelBlockRenderer.SizeInfo.SOUTH
         }
      );

      final Direction[] corners;
      final boolean doNonCubicWeight;
      final ModelBlockRenderer.SizeInfo[] vert0Weights;
      final ModelBlockRenderer.SizeInfo[] vert1Weights;
      final ModelBlockRenderer.SizeInfo[] vert2Weights;
      final ModelBlockRenderer.SizeInfo[] vert3Weights;
      private static final ModelBlockRenderer.AdjacencyInfo[] BY_FACING = Util.make(new ModelBlockRenderer.AdjacencyInfo[6], p_111134_ -> {
         p_111134_[Direction.DOWN.get3DDataValue()] = DOWN;
         p_111134_[Direction.UP.get3DDataValue()] = UP;
         p_111134_[Direction.NORTH.get3DDataValue()] = NORTH;
         p_111134_[Direction.SOUTH.get3DDataValue()] = SOUTH;
         p_111134_[Direction.WEST.get3DDataValue()] = WEST;
         p_111134_[Direction.EAST.get3DDataValue()] = EAST;
      });

      AdjacencyInfo(
         final Direction[] p_111122_,
         final float p_111123_,
         final boolean p_111124_,
         final ModelBlockRenderer.SizeInfo[] p_111125_,
         final ModelBlockRenderer.SizeInfo[] p_111126_,
         final ModelBlockRenderer.SizeInfo[] p_111127_,
         final ModelBlockRenderer.SizeInfo[] p_111128_
      ) {
         this.corners = p_111122_;
         this.doNonCubicWeight = p_111124_;
         this.vert0Weights = p_111125_;
         this.vert1Weights = p_111126_;
         this.vert2Weights = p_111127_;
         this.vert3Weights = p_111128_;
      }

      public static ModelBlockRenderer.AdjacencyInfo fromFacing(Direction p_111132_) {
         return BY_FACING[p_111132_.get3DDataValue()];
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class AmbientOcclusionRenderStorage extends ModelBlockRenderer.CommonRenderStorage {
      final float[] faceShape = new float[ModelBlockRenderer.SizeInfo.COUNT];

      public AmbientOcclusionRenderStorage() {
      }

      public void calculate(BlockAndTintGetter p_392472_, BlockState p_392545_, BlockPos p_393020_, Direction p_392793_, boolean p_394796_) {
         BlockPos blockpos = this.faceCubic ? p_393020_.relative(p_392793_) : p_393020_;
         ModelBlockRenderer.AdjacencyInfo modelblockrenderer$adjacencyinfo = ModelBlockRenderer.AdjacencyInfo.fromFacing(p_392793_);
         BlockPos.MutableBlockPos blockpos$mutableblockpos = this.scratchPos;
         blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[0]);
         BlockState blockstate = p_392472_.getBlockState(blockpos$mutableblockpos);
         int i = this.cache.getLightColor(blockstate, p_392472_, blockpos$mutableblockpos);
         float f = this.cache.getShadeBrightness(blockstate, p_392472_, blockpos$mutableblockpos);
         blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[1]);
         BlockState blockstate1 = p_392472_.getBlockState(blockpos$mutableblockpos);
         int j = this.cache.getLightColor(blockstate1, p_392472_, blockpos$mutableblockpos);
         float f1 = this.cache.getShadeBrightness(blockstate1, p_392472_, blockpos$mutableblockpos);
         blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[2]);
         BlockState blockstate2 = p_392472_.getBlockState(blockpos$mutableblockpos);
         int k = this.cache.getLightColor(blockstate2, p_392472_, blockpos$mutableblockpos);
         float f2 = this.cache.getShadeBrightness(blockstate2, p_392472_, blockpos$mutableblockpos);
         blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[3]);
         BlockState blockstate3 = p_392472_.getBlockState(blockpos$mutableblockpos);
         int l = this.cache.getLightColor(blockstate3, p_392472_, blockpos$mutableblockpos);
         float f3 = this.cache.getShadeBrightness(blockstate3, p_392472_, blockpos$mutableblockpos);
         BlockState blockstate4 = p_392472_.getBlockState(
            blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[0]).move(p_392793_)
         );
         boolean flag = !blockstate4.isViewBlocking(p_392472_, blockpos$mutableblockpos) || blockstate4.getLightBlock() == 0;
         BlockState blockstate5 = p_392472_.getBlockState(
            blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[1]).move(p_392793_)
         );
         boolean flag1 = !blockstate5.isViewBlocking(p_392472_, blockpos$mutableblockpos) || blockstate5.getLightBlock() == 0;
         BlockState blockstate6 = p_392472_.getBlockState(
            blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[2]).move(p_392793_)
         );
         boolean flag2 = !blockstate6.isViewBlocking(p_392472_, blockpos$mutableblockpos) || blockstate6.getLightBlock() == 0;
         BlockState blockstate7 = p_392472_.getBlockState(
            blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[3]).move(p_392793_)
         );
         boolean flag3 = !blockstate7.isViewBlocking(p_392472_, blockpos$mutableblockpos) || blockstate7.getLightBlock() == 0;
         float f4;
         int i1;
         if (!flag2 && !flag) {
            f4 = f;
            i1 = i;
         } else {
            blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[0]).move(modelblockrenderer$adjacencyinfo.corners[2]);
            BlockState blockstate8 = p_392472_.getBlockState(blockpos$mutableblockpos);
            f4 = this.cache.getShadeBrightness(blockstate8, p_392472_, blockpos$mutableblockpos);
            i1 = this.cache.getLightColor(blockstate8, p_392472_, blockpos$mutableblockpos);
         }

         int j1;
         float f5;
         if (!flag3 && !flag) {
            f5 = f;
            j1 = i;
         } else {
            blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[0]).move(modelblockrenderer$adjacencyinfo.corners[3]);
            BlockState blockstate10 = p_392472_.getBlockState(blockpos$mutableblockpos);
            f5 = this.cache.getShadeBrightness(blockstate10, p_392472_, blockpos$mutableblockpos);
            j1 = this.cache.getLightColor(blockstate10, p_392472_, blockpos$mutableblockpos);
         }

         int k1;
         float f6;
         if (!flag2 && !flag1) {
            f6 = f;
            k1 = i;
         } else {
            blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[1]).move(modelblockrenderer$adjacencyinfo.corners[2]);
            BlockState blockstate11 = p_392472_.getBlockState(blockpos$mutableblockpos);
            f6 = this.cache.getShadeBrightness(blockstate11, p_392472_, blockpos$mutableblockpos);
            k1 = this.cache.getLightColor(blockstate11, p_392472_, blockpos$mutableblockpos);
         }

         int l1;
         float f7;
         if (!flag3 && !flag1) {
            f7 = f;
            l1 = i;
         } else {
            blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[1]).move(modelblockrenderer$adjacencyinfo.corners[3]);
            BlockState blockstate12 = p_392472_.getBlockState(blockpos$mutableblockpos);
            f7 = this.cache.getShadeBrightness(blockstate12, p_392472_, blockpos$mutableblockpos);
            l1 = this.cache.getLightColor(blockstate12, p_392472_, blockpos$mutableblockpos);
         }

         int i3 = this.cache.getLightColor(p_392545_, p_392472_, p_393020_);
         blockpos$mutableblockpos.setWithOffset(p_393020_, p_392793_);
         BlockState blockstate9 = p_392472_.getBlockState(blockpos$mutableblockpos);
         if (this.faceCubic || !blockstate9.isSolidRender()) {
            i3 = this.cache.getLightColor(blockstate9, p_392472_, blockpos$mutableblockpos);
         }

         float f8 = this.faceCubic
            ? this.cache.getShadeBrightness(p_392472_.getBlockState(blockpos), p_392472_, blockpos)
            : this.cache.getShadeBrightness(p_392472_.getBlockState(p_393020_), p_392472_, p_393020_);
         ModelBlockRenderer.AmbientVertexRemap modelblockrenderer$ambientvertexremap = ModelBlockRenderer.AmbientVertexRemap.fromFacing(p_392793_);
         if (this.facePartial && modelblockrenderer$adjacencyinfo.doNonCubicWeight) {
            float f29 = (f3 + f + f5 + f8) * 0.25F;
            float f31 = (f2 + f + f4 + f8) * 0.25F;
            float f32 = (f2 + f1 + f6 + f8) * 0.25F;
            float f33 = (f3 + f1 + f7 + f8) * 0.25F;
            float f13 = this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[0].index]
               * this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[1].index];
            float f14 = this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[2].index]
               * this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[3].index];
            float f15 = this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[4].index]
               * this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[5].index];
            float f16 = this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[6].index]
               * this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[7].index];
            float f17 = this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[0].index]
               * this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[1].index];
            float f18 = this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[2].index]
               * this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[3].index];
            float f19 = this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[4].index]
               * this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[5].index];
            float f20 = this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[6].index]
               * this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[7].index];
            float f21 = this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[0].index]
               * this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[1].index];
            float f22 = this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[2].index]
               * this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[3].index];
            float f23 = this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[4].index]
               * this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[5].index];
            float f24 = this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[6].index]
               * this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[7].index];
            float f25 = this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[0].index]
               * this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[1].index];
            float f26 = this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[2].index]
               * this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[3].index];
            float f27 = this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[4].index]
               * this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[5].index];
            float f28 = this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[6].index]
               * this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[7].index];
            this.brightness[modelblockrenderer$ambientvertexremap.vert0] = Math.clamp(f29 * f13 + f31 * f14 + f32 * f15 + f33 * f16, 0.0F, 1.0F);
            this.brightness[modelblockrenderer$ambientvertexremap.vert1] = Math.clamp(f29 * f17 + f31 * f18 + f32 * f19 + f33 * f20, 0.0F, 1.0F);
            this.brightness[modelblockrenderer$ambientvertexremap.vert2] = Math.clamp(f29 * f21 + f31 * f22 + f32 * f23 + f33 * f24, 0.0F, 1.0F);
            this.brightness[modelblockrenderer$ambientvertexremap.vert3] = Math.clamp(f29 * f25 + f31 * f26 + f32 * f27 + f33 * f28, 0.0F, 1.0F);
            int i2 = blend(l, i, j1, i3);
            int j2 = blend(k, i, i1, i3);
            int k2 = blend(k, j, k1, i3);
            int l2 = blend(l, j, l1, i3);
            this.lightmap[modelblockrenderer$ambientvertexremap.vert0] = blend(i2, j2, k2, l2, f13, f14, f15, f16);
            this.lightmap[modelblockrenderer$ambientvertexremap.vert1] = blend(i2, j2, k2, l2, f17, f18, f19, f20);
            this.lightmap[modelblockrenderer$ambientvertexremap.vert2] = blend(i2, j2, k2, l2, f21, f22, f23, f24);
            this.lightmap[modelblockrenderer$ambientvertexremap.vert3] = blend(i2, j2, k2, l2, f25, f26, f27, f28);
         } else {
            float f9 = (f3 + f + f5 + f8) * 0.25F;
            float f10 = (f2 + f + f4 + f8) * 0.25F;
            float f11 = (f2 + f1 + f6 + f8) * 0.25F;
            float f12 = (f3 + f1 + f7 + f8) * 0.25F;
            this.lightmap[modelblockrenderer$ambientvertexremap.vert0] = blend(l, i, j1, i3);
            this.lightmap[modelblockrenderer$ambientvertexremap.vert1] = blend(k, i, i1, i3);
            this.lightmap[modelblockrenderer$ambientvertexremap.vert2] = blend(k, j, k1, i3);
            this.lightmap[modelblockrenderer$ambientvertexremap.vert3] = blend(l, j, l1, i3);
            this.brightness[modelblockrenderer$ambientvertexremap.vert0] = f9;
            this.brightness[modelblockrenderer$ambientvertexremap.vert1] = f10;
            this.brightness[modelblockrenderer$ambientvertexremap.vert2] = f11;
            this.brightness[modelblockrenderer$ambientvertexremap.vert3] = f12;
         }

         float f30 = p_392472_.getShade(p_392793_, p_394796_);

         for (int j3 = 0; j3 < this.brightness.length; j3++) {
            this.brightness[j3] = this.brightness[j3] * f30;
         }
      }

      private static int blend(int p_394213_, int p_397905_, int p_392318_, int p_397150_) {
         if (p_394213_ == 0) {
            p_394213_ = p_397150_;
         }

         if (p_397905_ == 0) {
            p_397905_ = p_397150_;
         }

         if (p_392318_ == 0) {
            p_392318_ = p_397150_;
         }

         return p_394213_ + p_397905_ + p_392318_ + p_397150_ >> 2 & 16711935;
      }

      private static int blend(int p_395262_, int p_396930_, int p_395695_, int p_393923_, float p_397544_, float p_391464_, float p_398036_, float p_397919_) {
         int i = (int)(
               (p_395262_ >> 16 & 0xFF) * p_397544_
                  + (p_396930_ >> 16 & 0xFF) * p_391464_
                  + (p_395695_ >> 16 & 0xFF) * p_398036_
                  + (p_393923_ >> 16 & 0xFF) * p_397919_
            )
            & 0xFF;
         int j = (int)((p_395262_ & 0xFF) * p_397544_ + (p_396930_ & 0xFF) * p_391464_ + (p_395695_ & 0xFF) * p_398036_ + (p_393923_ & 0xFF) * p_397919_)
            & 0xFF;
         return i << 16 | j;
      }
   }

   @OnlyIn(Dist.CLIENT)
   enum AmbientVertexRemap {
      DOWN(0, 1, 2, 3),
      UP(2, 3, 0, 1),
      NORTH(3, 0, 1, 2),
      SOUTH(0, 1, 2, 3),
      WEST(3, 0, 1, 2),
      EAST(1, 2, 3, 0);

      final int vert0;
      final int vert1;
      final int vert2;
      final int vert3;
      private static final ModelBlockRenderer.AmbientVertexRemap[] BY_FACING = Util.make(new ModelBlockRenderer.AmbientVertexRemap[6], p_111204_ -> {
         p_111204_[Direction.DOWN.get3DDataValue()] = DOWN;
         p_111204_[Direction.UP.get3DDataValue()] = UP;
         p_111204_[Direction.NORTH.get3DDataValue()] = NORTH;
         p_111204_[Direction.SOUTH.get3DDataValue()] = SOUTH;
         p_111204_[Direction.WEST.get3DDataValue()] = WEST;
         p_111204_[Direction.EAST.get3DDataValue()] = EAST;
      });

      AmbientVertexRemap(final int p_111195_, final int p_111196_, final int p_111197_, final int p_111198_) {
         this.vert0 = p_111195_;
         this.vert1 = p_111196_;
         this.vert2 = p_111197_;
         this.vert3 = p_111198_;
      }

      public static ModelBlockRenderer.AmbientVertexRemap fromFacing(Direction p_111202_) {
         return BY_FACING[p_111202_.get3DDataValue()];
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class Cache {
      private boolean enabled;
      private final Long2IntLinkedOpenHashMap colorCache = Util.make(() -> {
         Long2IntLinkedOpenHashMap long2intlinkedopenhashmap = new Long2IntLinkedOpenHashMap(100, 0.25F) {
            protected void rehash(int p_111238_) {
            }
         };
         long2intlinkedopenhashmap.defaultReturnValue(Integer.MAX_VALUE);
         return long2intlinkedopenhashmap;
      });
      private final Long2FloatLinkedOpenHashMap brightnessCache = Util.make(() -> {
         Long2FloatLinkedOpenHashMap long2floatlinkedopenhashmap = new Long2FloatLinkedOpenHashMap(100, 0.25F) {
            protected void rehash(int p_111245_) {
            }
         };
         long2floatlinkedopenhashmap.defaultReturnValue(Float.NaN);
         return long2floatlinkedopenhashmap;
      });
      private final LevelRenderer.BrightnessGetter cachedBrightnessGetter = (p_398128_, p_398129_) -> {
         long i = p_398129_.asLong();
         int j = this.colorCache.get(i);
         if (j != Integer.MAX_VALUE) {
            return j;
         }

         int k = LevelRenderer.BrightnessGetter.DEFAULT.packedBrightness(p_398128_, p_398129_);
         if (this.colorCache.size() == 100) {
            this.colorCache.removeFirstInt();
         }

         this.colorCache.put(i, k);
         return k;
      };

      private Cache() {
      }

      public void enable() {
         this.enabled = true;
      }

      public void disable() {
         this.enabled = false;
         this.colorCache.clear();
         this.brightnessCache.clear();
      }

      public int getLightColor(BlockState p_111222_, BlockAndTintGetter p_111223_, BlockPos p_111224_) {
         return LevelRenderer.getLightColor(
            this.enabled ? this.cachedBrightnessGetter : LevelRenderer.BrightnessGetter.DEFAULT, p_111223_, p_111222_, p_111224_
         );
      }

      public float getShadeBrightness(BlockState p_111227_, BlockAndTintGetter p_111228_, BlockPos p_111229_) {
         long i = p_111229_.asLong();
         if (this.enabled) {
            float f = this.brightnessCache.get(i);
            if (!Float.isNaN(f)) {
               return f;
            }
         }

         float f1 = p_111227_.getShadeBrightness(p_111228_, p_111229_);
         if (this.enabled) {
            if (this.brightnessCache.size() == 100) {
               this.brightnessCache.removeFirstFloat();
            }

            this.brightnessCache.put(i, f1);
         }

         return f1;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class CommonRenderStorage {
      public final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();
      public boolean faceCubic;
      public boolean facePartial;
      public final float[] brightness = new float[4];
      public final int[] lightmap = new int[4];
      public int tintCacheIndex = -1;
      public int tintCacheValue;
      public final ModelBlockRenderer.Cache cache = ModelBlockRenderer.CACHE.get();
   }

   @OnlyIn(Dist.CLIENT)
   protected enum SizeInfo {
      DOWN(0),
      UP(1),
      NORTH(2),
      SOUTH(3),
      WEST(4),
      EAST(5),
      FLIP_DOWN(6),
      FLIP_UP(7),
      FLIP_NORTH(8),
      FLIP_SOUTH(9),
      FLIP_WEST(10),
      FLIP_EAST(11);

      public static final int COUNT = values().length;
      final int index;

      SizeInfo(final int p_396212_) {
         this.index = p_396212_;
      }
   }
}
