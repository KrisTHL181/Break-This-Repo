package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LiquidBlockRenderer {
   private static final float MAX_FLUID_HEIGHT = 0.8888889F;
   private final TextureAtlasSprite lavaStill;
   private final TextureAtlasSprite lavaFlowing;
   private final TextureAtlasSprite waterStill;
   private final TextureAtlasSprite waterFlowing;
   private final TextureAtlasSprite waterOverlay;

   public LiquidBlockRenderer(MaterialSet p_451072_) {
      this.lavaStill = p_451072_.get(ModelBakery.LAVA_STILL);
      this.lavaFlowing = p_451072_.get(ModelBakery.LAVA_FLOW);
      this.waterStill = p_451072_.get(ModelBakery.WATER_STILL);
      this.waterFlowing = p_451072_.get(ModelBakery.WATER_FLOW);
      this.waterOverlay = p_451072_.get(ModelBakery.WATER_OVERLAY);
   }

   private static boolean isNeighborSameFluid(FluidState p_203186_, FluidState p_203187_) {
      return p_203187_.getType().isSame(p_203186_.getType());
   }

   private static boolean isFaceOccludedByState(Direction p_110980_, float p_110981_, BlockState p_110983_) {
      VoxelShape voxelshape = p_110983_.getFaceOcclusionShape(p_110980_.getOpposite());
      if (voxelshape == Shapes.empty()) {
         return false;
      } else if (voxelshape == Shapes.block()) {
         boolean flag = p_110981_ == 1.0F;
         return p_110980_ != Direction.UP || flag;
      } else {
         VoxelShape voxelshape1 = Shapes.box(0.0, 0.0, 0.0, 1.0, p_110981_, 1.0);
         return Shapes.blockOccludes(voxelshape1, voxelshape, p_110980_);
      }
   }

   private static boolean isFaceOccludedByNeighbor(Direction p_203182_, float p_203183_, BlockState p_203184_) {
      return isFaceOccludedByState(p_203182_, p_203183_, p_203184_);
   }

   private static boolean isFaceOccludedBySelf(BlockState p_110962_, Direction p_110963_) {
      return isFaceOccludedByState(p_110963_.getOpposite(), 1.0F, p_110962_);
   }

   public static boolean shouldRenderFace(FluidState p_203169_, BlockState p_203170_, Direction p_203171_, FluidState p_203172_) {
      return !isFaceOccludedBySelf(p_203170_, p_203171_) && !isNeighborSameFluid(p_203169_, p_203172_);
   }

   public void tesselate(BlockAndTintGetter p_234370_, BlockPos p_234371_, VertexConsumer p_234372_, BlockState p_234373_, FluidState p_234374_) {
      boolean flag = p_234374_.is(FluidTags.LAVA);
      TextureAtlasSprite textureatlassprite = flag ? this.lavaStill : this.waterStill;
      TextureAtlasSprite textureatlassprite1 = flag ? this.lavaFlowing : this.waterFlowing;
      int i = flag ? 16777215 : BiomeColors.getAverageWaterColor(p_234370_, p_234371_);
      float f = (i >> 16 & 0xFF) / 255.0F;
      float f1 = (i >> 8 & 0xFF) / 255.0F;
      float f2 = (i & 0xFF) / 255.0F;
      BlockState blockstate = p_234370_.getBlockState(p_234371_.relative(Direction.DOWN));
      FluidState fluidstate = blockstate.getFluidState();
      BlockState blockstate1 = p_234370_.getBlockState(p_234371_.relative(Direction.UP));
      FluidState fluidstate1 = blockstate1.getFluidState();
      BlockState blockstate2 = p_234370_.getBlockState(p_234371_.relative(Direction.NORTH));
      FluidState fluidstate2 = blockstate2.getFluidState();
      BlockState blockstate3 = p_234370_.getBlockState(p_234371_.relative(Direction.SOUTH));
      FluidState fluidstate3 = blockstate3.getFluidState();
      BlockState blockstate4 = p_234370_.getBlockState(p_234371_.relative(Direction.WEST));
      FluidState fluidstate4 = blockstate4.getFluidState();
      BlockState blockstate5 = p_234370_.getBlockState(p_234371_.relative(Direction.EAST));
      FluidState fluidstate5 = blockstate5.getFluidState();
      boolean flag1 = !isNeighborSameFluid(p_234374_, fluidstate1);
      boolean flag2 = shouldRenderFace(p_234374_, p_234373_, Direction.DOWN, fluidstate) && !isFaceOccludedByNeighbor(Direction.DOWN, 0.8888889F, blockstate);
      boolean flag3 = shouldRenderFace(p_234374_, p_234373_, Direction.NORTH, fluidstate2);
      boolean flag4 = shouldRenderFace(p_234374_, p_234373_, Direction.SOUTH, fluidstate3);
      boolean flag5 = shouldRenderFace(p_234374_, p_234373_, Direction.WEST, fluidstate4);
      boolean flag6 = shouldRenderFace(p_234374_, p_234373_, Direction.EAST, fluidstate5);
      if (flag1 || flag2 || flag6 || flag5 || flag3 || flag4) {
         float f3 = p_234370_.getShade(Direction.DOWN, true);
         float f4 = p_234370_.getShade(Direction.UP, true);
         float f5 = p_234370_.getShade(Direction.NORTH, true);
         float f6 = p_234370_.getShade(Direction.WEST, true);
         Fluid fluid = p_234374_.getType();
         float f11 = this.getHeight(p_234370_, fluid, p_234371_, p_234373_, p_234374_);
         float f7;
         float f8;
         float f9;
         float f10;
         if (f11 >= 1.0F) {
            f7 = 1.0F;
            f8 = 1.0F;
            f9 = 1.0F;
            f10 = 1.0F;
         } else {
            float f12 = this.getHeight(p_234370_, fluid, p_234371_.north(), blockstate2, fluidstate2);
            float f13 = this.getHeight(p_234370_, fluid, p_234371_.south(), blockstate3, fluidstate3);
            float f14 = this.getHeight(p_234370_, fluid, p_234371_.east(), blockstate5, fluidstate5);
            float f15 = this.getHeight(p_234370_, fluid, p_234371_.west(), blockstate4, fluidstate4);
            f7 = this.calculateAverageHeight(p_234370_, fluid, f11, f12, f14, p_234371_.relative(Direction.NORTH).relative(Direction.EAST));
            f8 = this.calculateAverageHeight(p_234370_, fluid, f11, f12, f15, p_234371_.relative(Direction.NORTH).relative(Direction.WEST));
            f9 = this.calculateAverageHeight(p_234370_, fluid, f11, f13, f14, p_234371_.relative(Direction.SOUTH).relative(Direction.EAST));
            f10 = this.calculateAverageHeight(p_234370_, fluid, f11, f13, f15, p_234371_.relative(Direction.SOUTH).relative(Direction.WEST));
         }

         float f37 = p_234371_.getX() & 15;
         float f38 = p_234371_.getY() & 15;
         float f39 = p_234371_.getZ() & 15;
         float f40 = 0.001F;
         float f16 = flag2 ? 0.001F : 0.0F;
         if (flag1 && !isFaceOccludedByNeighbor(Direction.UP, Math.min(Math.min(f8, f10), Math.min(f9, f7)), blockstate1)) {
            f8 -= 0.001F;
            f10 -= 0.001F;
            f9 -= 0.001F;
            f7 -= 0.001F;
            Vec3 vec3 = p_234374_.getFlow(p_234370_, p_234371_);
            float f17;
            float f18;
            float f19;
            float f20;
            float f21;
            float f22;
            float f23;
            float f24;
            if (vec3.x == 0.0 && vec3.z == 0.0) {
               f17 = textureatlassprite.getU(0.0F);
               f21 = textureatlassprite.getV(0.0F);
               f18 = f17;
               f22 = textureatlassprite.getV(1.0F);
               f19 = textureatlassprite.getU(1.0F);
               f23 = f22;
               f20 = f19;
               f24 = f21;
            } else {
               float f25 = (float)Mth.atan2(vec3.z, vec3.x) - (float) (Math.PI / 2);
               float f26 = Mth.sin(f25) * 0.25F;
               float f27 = Mth.cos(f25) * 0.25F;
               float f28 = 0.5F;
               f17 = textureatlassprite1.getU(0.5F + (-f27 - f26));
               f21 = textureatlassprite1.getV(0.5F + (-f27 + f26));
               f18 = textureatlassprite1.getU(0.5F + (-f27 + f26));
               f22 = textureatlassprite1.getV(0.5F + (f27 + f26));
               f19 = textureatlassprite1.getU(0.5F + (f27 + f26));
               f23 = textureatlassprite1.getV(0.5F + (f27 - f26));
               f20 = textureatlassprite1.getU(0.5F + (f27 - f26));
               f24 = textureatlassprite1.getV(0.5F + (-f27 - f26));
            }

            int l = this.getLightColor(p_234370_, p_234371_);
            float f54 = f4 * f;
            float f55 = f4 * f1;
            float f56 = f4 * f2;
            this.vertex(p_234372_, f37 + 0.0F, f38 + f8, f39 + 0.0F, f54, f55, f56, f17, f21, l);
            this.vertex(p_234372_, f37 + 0.0F, f38 + f10, f39 + 1.0F, f54, f55, f56, f18, f22, l);
            this.vertex(p_234372_, f37 + 1.0F, f38 + f9, f39 + 1.0F, f54, f55, f56, f19, f23, l);
            this.vertex(p_234372_, f37 + 1.0F, f38 + f7, f39 + 0.0F, f54, f55, f56, f20, f24, l);
            if (p_234374_.shouldRenderBackwardUpFace(p_234370_, p_234371_.above())) {
               this.vertex(p_234372_, f37 + 0.0F, f38 + f8, f39 + 0.0F, f54, f55, f56, f17, f21, l);
               this.vertex(p_234372_, f37 + 1.0F, f38 + f7, f39 + 0.0F, f54, f55, f56, f20, f24, l);
               this.vertex(p_234372_, f37 + 1.0F, f38 + f9, f39 + 1.0F, f54, f55, f56, f19, f23, l);
               this.vertex(p_234372_, f37 + 0.0F, f38 + f10, f39 + 1.0F, f54, f55, f56, f18, f22, l);
            }
         }

         if (flag2) {
            float f41 = textureatlassprite.getU0();
            float f42 = textureatlassprite.getU1();
            float f43 = textureatlassprite.getV0();
            float f44 = textureatlassprite.getV1();
            int k = this.getLightColor(p_234370_, p_234371_.below());
            float f47 = f3 * f;
            float f49 = f3 * f1;
            float f51 = f3 * f2;
            this.vertex(p_234372_, f37, f38 + f16, f39 + 1.0F, f47, f49, f51, f41, f44, k);
            this.vertex(p_234372_, f37, f38 + f16, f39, f47, f49, f51, f41, f43, k);
            this.vertex(p_234372_, f37 + 1.0F, f38 + f16, f39, f47, f49, f51, f42, f43, k);
            this.vertex(p_234372_, f37 + 1.0F, f38 + f16, f39 + 1.0F, f47, f49, f51, f42, f44, k);
         }

         int j = this.getLightColor(p_234370_, p_234371_);

         for (Direction direction : Direction.Plane.HORIZONTAL) {
            float f45;
            float f46;
            float f48;
            float f50;
            float f52;
            float f53;
            boolean flag7;
            switch (direction) {
               case NORTH:
                  f45 = f8;
                  f46 = f7;
                  f48 = f37;
                  f52 = f37 + 1.0F;
                  f50 = f39 + 0.001F;
                  f53 = f39 + 0.001F;
                  flag7 = flag3;
                  break;
               case SOUTH:
                  f45 = f9;
                  f46 = f10;
                  f48 = f37 + 1.0F;
                  f52 = f37;
                  f50 = f39 + 1.0F - 0.001F;
                  f53 = f39 + 1.0F - 0.001F;
                  flag7 = flag4;
                  break;
               case WEST:
                  f45 = f10;
                  f46 = f8;
                  f48 = f37 + 0.001F;
                  f52 = f37 + 0.001F;
                  f50 = f39 + 1.0F;
                  f53 = f39;
                  flag7 = flag5;
                  break;
               default:
                  f45 = f7;
                  f46 = f9;
                  f48 = f37 + 1.0F - 0.001F;
                  f52 = f37 + 1.0F - 0.001F;
                  f50 = f39;
                  f53 = f39 + 1.0F;
                  flag7 = flag6;
            }

            if (flag7 && !isFaceOccludedByNeighbor(direction, Math.max(f45, f46), p_234370_.getBlockState(p_234371_.relative(direction)))) {
               BlockPos blockpos = p_234371_.relative(direction);
               TextureAtlasSprite textureatlassprite2 = textureatlassprite1;
               if (!flag) {
                  Block block = p_234370_.getBlockState(blockpos).getBlock();
                  if (block instanceof HalfTransparentBlock || block instanceof LeavesBlock) {
                     textureatlassprite2 = this.waterOverlay;
                  }
               }

               float f57 = textureatlassprite2.getU(0.0F);
               float f29 = textureatlassprite2.getU(0.5F);
               float f30 = textureatlassprite2.getV((1.0F - f45) * 0.5F);
               float f31 = textureatlassprite2.getV((1.0F - f46) * 0.5F);
               float f32 = textureatlassprite2.getV(0.5F);
               float f33 = direction.getAxis() == Direction.Axis.Z ? f5 : f6;
               float f34 = f4 * f33 * f;
               float f35 = f4 * f33 * f1;
               float f36 = f4 * f33 * f2;
               this.vertex(p_234372_, f48, f38 + f45, f50, f34, f35, f36, f57, f30, j);
               this.vertex(p_234372_, f52, f38 + f46, f53, f34, f35, f36, f29, f31, j);
               this.vertex(p_234372_, f52, f38 + f16, f53, f34, f35, f36, f29, f32, j);
               this.vertex(p_234372_, f48, f38 + f16, f50, f34, f35, f36, f57, f32, j);
               if (textureatlassprite2 != this.waterOverlay) {
                  this.vertex(p_234372_, f48, f38 + f16, f50, f34, f35, f36, f57, f32, j);
                  this.vertex(p_234372_, f52, f38 + f16, f53, f34, f35, f36, f29, f32, j);
                  this.vertex(p_234372_, f52, f38 + f46, f53, f34, f35, f36, f29, f31, j);
                  this.vertex(p_234372_, f48, f38 + f45, f50, f34, f35, f36, f57, f30, j);
               }
            }
         }
      }
   }

   private float calculateAverageHeight(BlockAndTintGetter p_203150_, Fluid p_203151_, float p_203152_, float p_203153_, float p_203154_, BlockPos p_203155_) {
      if (!(p_203154_ >= 1.0F) && !(p_203153_ >= 1.0F)) {
         float[] afloat = new float[2];
         if (p_203154_ > 0.0F || p_203153_ > 0.0F) {
            float f = this.getHeight(p_203150_, p_203151_, p_203155_);
            if (f >= 1.0F) {
               return 1.0F;
            }

            this.addWeightedHeight(afloat, f);
         }

         this.addWeightedHeight(afloat, p_203152_);
         this.addWeightedHeight(afloat, p_203154_);
         this.addWeightedHeight(afloat, p_203153_);
         return afloat[0] / afloat[1];
      } else {
         return 1.0F;
      }
   }

   private void addWeightedHeight(float[] p_203189_, float p_203190_) {
      if (p_203190_ >= 0.8F) {
         p_203189_[0] += p_203190_ * 10.0F;
         p_203189_[1] += 10.0F;
      } else if (p_203190_ >= 0.0F) {
         p_203189_[0] += p_203190_;
         p_203189_[1]++;
      }
   }

   private float getHeight(BlockAndTintGetter p_203157_, Fluid p_203158_, BlockPos p_203159_) {
      BlockState blockstate = p_203157_.getBlockState(p_203159_);
      return this.getHeight(p_203157_, p_203158_, p_203159_, blockstate, blockstate.getFluidState());
   }

   private float getHeight(BlockAndTintGetter p_203161_, Fluid p_203162_, BlockPos p_203163_, BlockState p_203164_, FluidState p_203165_) {
      if (p_203162_.isSame(p_203165_.getType())) {
         BlockState blockstate = p_203161_.getBlockState(p_203163_.above());
         return p_203162_.isSame(blockstate.getFluidState().getType()) ? 1.0F : p_203165_.getOwnHeight();
      } else {
         return !p_203164_.isSolid() ? 0.0F : -1.0F;
      }
   }

   private void vertex(
      VertexConsumer p_110985_,
      float p_110989_,
      float p_110990_,
      float p_110991_,
      float p_110992_,
      float p_110993_,
      float p_343128_,
      float p_344448_,
      float p_344284_,
      int p_110994_
   ) {
      p_110985_.addVertex(p_110989_, p_110990_, p_110991_)
         .setColor(p_110992_, p_110993_, p_343128_, 1.0F)
         .setUv(p_344448_, p_344284_)
         .setLight(p_110994_)
         .setNormal(0.0F, 1.0F, 0.0F);
   }

   private int getLightColor(BlockAndTintGetter p_110946_, BlockPos p_110947_) {
      int i = LevelRenderer.getLightColor(p_110946_, p_110947_);
      int j = LevelRenderer.getLightColor(p_110946_, p_110947_.above());
      int k = i & 0xFF;
      int l = j & 0xFF;
      int i1 = i >> 16 & 0xFF;
      int j1 = j >> 16 & 0xFF;
      return (k > l ? k : l) | (i1 > j1 ? i1 : j1) << 16;
   }
}
