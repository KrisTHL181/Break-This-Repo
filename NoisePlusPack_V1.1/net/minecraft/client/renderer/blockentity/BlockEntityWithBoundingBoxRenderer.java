package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityWithBoundingBoxRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BoundingBoxRenderable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BlockEntityWithBoundingBoxRenderer<T extends BlockEntity & BoundingBoxRenderable>
   implements BlockEntityRenderer<T, BlockEntityWithBoundingBoxRenderState> {
   public static final int STRUCTURE_VOIDS_COLOR = ARGB.colorFromFloat(0.2F, 0.75F, 0.75F, 1.0F);

   public BlockEntityWithBoundingBoxRenderState createRenderState() {
      return new BlockEntityWithBoundingBoxRenderState();
   }

   public void extractRenderState(
      T p_425469_, BlockEntityWithBoundingBoxRenderState p_428653_, float p_431045_, Vec3 p_429829_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_422357_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_425469_, p_428653_, p_431045_, p_429829_, p_422357_);
      extract(p_425469_, p_428653_);
   }

   public static <T extends BlockEntity & BoundingBoxRenderable> void extract(T p_422522_, BlockEntityWithBoundingBoxRenderState p_428222_) {
      LocalPlayer localplayer = Minecraft.getInstance().player;
      p_428222_.isVisible = localplayer.canUseGameMasterBlocks() || localplayer.isSpectator();
      p_428222_.box = p_422522_.getRenderableBox();
      p_428222_.mode = p_422522_.renderMode();
      BlockPos blockpos = p_428222_.box.localPos();
      Vec3i vec3i = p_428222_.box.size();
      BlockPos blockpos1 = p_428222_.blockPos;
      BlockPos blockpos2 = blockpos1.offset(blockpos);
      if (p_428222_.isVisible && p_422522_.getLevel() != null && p_428222_.mode == BoundingBoxRenderable.Mode.BOX_AND_INVISIBLE_BLOCKS) {
         p_428222_.invisibleBlocks = new BlockEntityWithBoundingBoxRenderState.InvisibleBlockType[vec3i.getX() * vec3i.getY() * vec3i.getZ()];

         for (int i = 0; i < vec3i.getX(); i++) {
            for (int j = 0; j < vec3i.getY(); j++) {
               for (int k = 0; k < vec3i.getZ(); k++) {
                  int l = k * vec3i.getX() * vec3i.getY() + j * vec3i.getX() + i;
                  BlockState blockstate = p_422522_.getLevel().getBlockState(blockpos2.offset(i, j, k));
                  if (blockstate.isAir()) {
                     p_428222_.invisibleBlocks[l] = BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.AIR;
                  } else if (blockstate.is(Blocks.STRUCTURE_VOID)) {
                     p_428222_.invisibleBlocks[l] = BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.STRUCTURE_VOID;
                  } else if (blockstate.is(Blocks.BARRIER)) {
                     p_428222_.invisibleBlocks[l] = BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.BARRIER;
                  } else if (blockstate.is(Blocks.LIGHT)) {
                     p_428222_.invisibleBlocks[l] = BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.LIGHT;
                  }
               }
            }
         }
      } else {
         p_428222_.invisibleBlocks = null;
      }

      if (p_428222_.isVisible) {
      }

      p_428222_.structureVoids = null;
   }

   public void submit(BlockEntityWithBoundingBoxRenderState p_426313_, PoseStack p_426054_, SubmitNodeCollector p_428487_, CameraRenderState p_431343_) {
      if (p_426313_.isVisible) {
         BoundingBoxRenderable.Mode boundingboxrenderable$mode = p_426313_.mode;
         if (boundingboxrenderable$mode != BoundingBoxRenderable.Mode.NONE) {
            BoundingBoxRenderable.RenderableBox boundingboxrenderable$renderablebox = p_426313_.box;
            BlockPos blockpos = boundingboxrenderable$renderablebox.localPos();
            Vec3i vec3i = boundingboxrenderable$renderablebox.size();
            if (vec3i.getX() >= 1 && vec3i.getY() >= 1 && vec3i.getZ() >= 1) {
               float f = 1.0F;
               float f1 = 0.9F;
               BlockPos blockpos1 = blockpos.offset(vec3i);
               Gizmos.cuboid(
                  new AABB(blockpos.getX(), blockpos.getY(), blockpos.getZ(), blockpos1.getX(), blockpos1.getY(), blockpos1.getZ()).move(p_426313_.blockPos),
                  GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 0.9F, 0.9F, 0.9F)),
                  true
               );
               this.renderInvisibleBlocks(p_426313_, blockpos, vec3i);
            }
         }
      }
   }

   private void renderInvisibleBlocks(BlockEntityWithBoundingBoxRenderState p_460562_, BlockPos p_452874_, Vec3i p_457372_) {
      if (p_460562_.invisibleBlocks != null) {
         BlockPos blockpos = p_460562_.blockPos;
         BlockPos blockpos1 = blockpos.offset(p_452874_);

         for (int i = 0; i < p_457372_.getX(); i++) {
            for (int j = 0; j < p_457372_.getY(); j++) {
               for (int k = 0; k < p_457372_.getZ(); k++) {
                  int l = k * p_457372_.getX() * p_457372_.getY() + j * p_457372_.getX() + i;
                  BlockEntityWithBoundingBoxRenderState.InvisibleBlockType blockentitywithboundingboxrenderstate$invisibleblocktype = p_460562_.invisibleBlocks[l];
                  if (blockentitywithboundingboxrenderstate$invisibleblocktype != null) {
                     float f = blockentitywithboundingboxrenderstate$invisibleblocktype == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.AIR
                        ? 0.05F
                        : 0.0F;
                     double d0 = blockpos1.getX() + i + 0.45F - f;
                     double d1 = blockpos1.getY() + j + 0.45F - f;
                     double d2 = blockpos1.getZ() + k + 0.45F - f;
                     double d3 = blockpos1.getX() + i + 0.55F + f;
                     double d4 = blockpos1.getY() + j + 0.55F + f;
                     double d5 = blockpos1.getZ() + k + 0.55F + f;
                     AABB aabb = new AABB(d0, d1, d2, d3, d4, d5);
                     if (blockentitywithboundingboxrenderstate$invisibleblocktype == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.AIR) {
                        Gizmos.cuboid(aabb, GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 0.5F, 0.5F, 1.0F)));
                     } else if (blockentitywithboundingboxrenderstate$invisibleblocktype
                        == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.STRUCTURE_VOID) {
                        Gizmos.cuboid(aabb, GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 1.0F, 0.75F, 0.75F)));
                     } else if (blockentitywithboundingboxrenderstate$invisibleblocktype == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.BARRIER) {
                        Gizmos.cuboid(aabb, GizmoStyle.stroke(-65536));
                     } else if (blockentitywithboundingboxrenderstate$invisibleblocktype == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.LIGHT) {
                        Gizmos.cuboid(aabb, GizmoStyle.stroke(-256));
                     }
                  }
               }
            }
         }
      }
   }

   private void renderStructureVoids(BlockEntityWithBoundingBoxRenderState p_425318_, BlockPos p_397752_, Vec3i p_395592_) {
      if (p_425318_.structureVoids != null) {
         DiscreteVoxelShape discretevoxelshape = new BitSetDiscreteVoxelShape(p_395592_.getX(), p_395592_.getY(), p_395592_.getZ());

         for (int i = 0; i < p_395592_.getX(); i++) {
            for (int j = 0; j < p_395592_.getY(); j++) {
               for (int k = 0; k < p_395592_.getZ(); k++) {
                  int l = k * p_395592_.getX() * p_395592_.getY() + j * p_395592_.getX() + i;
                  if (p_425318_.structureVoids[l]) {
                     discretevoxelshape.fill(i, j, k);
                  }
               }
            }
         }

         discretevoxelshape.forAllFaces((p_448215_, p_448216_, p_448217_, p_448218_) -> {
            float f = 0.48F;
            float f1 = p_448216_ + p_397752_.getX() + 0.5F - 0.48F;
            float f2 = p_448217_ + p_397752_.getY() + 0.5F - 0.48F;
            float f3 = p_448218_ + p_397752_.getZ() + 0.5F - 0.48F;
            float f4 = p_448216_ + p_397752_.getX() + 0.5F + 0.48F;
            float f5 = p_448217_ + p_397752_.getY() + 0.5F + 0.48F;
            float f6 = p_448218_ + p_397752_.getZ() + 0.5F + 0.48F;
            Gizmos.rect(new Vec3(f1, f2, f3), new Vec3(f4, f5, f6), p_448215_, GizmoStyle.fill(STRUCTURE_VOIDS_COLOR));
         });
      }
   }

   @Override
   public boolean shouldRenderOffScreen() {
      return true;
   }

   @Override
   public int getViewDistance() {
      return 96;
   }
}
