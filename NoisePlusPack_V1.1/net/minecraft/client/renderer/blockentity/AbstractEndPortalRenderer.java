package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.EnumSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.EndPortalRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractEndPortalRenderer<T extends TheEndPortalBlockEntity, S extends EndPortalRenderState> implements BlockEntityRenderer<T, S> {
   public static final Identifier END_SKY_LOCATION = Identifier.withDefaultNamespace("textures/environment/end_sky.png");
   public static final Identifier END_PORTAL_LOCATION = Identifier.withDefaultNamespace("textures/entity/end_portal.png");

   public void extractRenderState(T p_423846_, S p_429747_, float p_431596_, Vec3 p_428772_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_425644_) {
      BlockEntityRenderer.super.extractRenderState(p_423846_, p_429747_, p_431596_, p_428772_, p_425644_);
      p_429747_.facesToShow.clear();

      for (Direction direction : Direction.values()) {
         if (p_423846_.shouldRenderFace(direction)) {
            p_429747_.facesToShow.add(direction);
         }
      }
   }

   public void submit(S p_430863_, PoseStack p_430020_, SubmitNodeCollector p_427983_, CameraRenderState p_427264_) {
      p_427983_.submitCustomGeometry(
         p_430020_, this.renderType(), (p_425714_, p_426138_) -> this.renderCube(p_430863_.facesToShow, p_425714_.pose(), p_426138_)
      );
   }

   private void renderCube(EnumSet<Direction> p_428171_, Matrix4f p_425617_, VertexConsumer p_431374_) {
      float f = this.getOffsetDown();
      float f1 = this.getOffsetUp();
      this.renderFace(p_428171_, p_425617_, p_431374_, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, Direction.SOUTH);
      this.renderFace(p_428171_, p_425617_, p_431374_, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, Direction.NORTH);
      this.renderFace(p_428171_, p_425617_, p_431374_, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, Direction.EAST);
      this.renderFace(p_428171_, p_425617_, p_431374_, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, Direction.WEST);
      this.renderFace(p_428171_, p_425617_, p_431374_, 0.0F, 1.0F, f, f, 0.0F, 0.0F, 1.0F, 1.0F, Direction.DOWN);
      this.renderFace(p_428171_, p_425617_, p_431374_, 0.0F, 1.0F, f1, f1, 1.0F, 1.0F, 0.0F, 0.0F, Direction.UP);
   }

   private void renderFace(
      EnumSet<Direction> p_429374_,
      Matrix4f p_422965_,
      VertexConsumer p_423823_,
      float p_429028_,
      float p_426278_,
      float p_424387_,
      float p_425021_,
      float p_422505_,
      float p_427080_,
      float p_427363_,
      float p_431538_,
      Direction p_429758_
   ) {
      if (p_429374_.contains(p_429758_)) {
         p_423823_.addVertex(p_422965_, p_429028_, p_424387_, p_422505_);
         p_423823_.addVertex(p_422965_, p_426278_, p_424387_, p_427080_);
         p_423823_.addVertex(p_422965_, p_426278_, p_425021_, p_427363_);
         p_423823_.addVertex(p_422965_, p_429028_, p_425021_, p_431538_);
      }
   }

   protected float getOffsetUp() {
      return 0.75F;
   }

   protected float getOffsetDown() {
      return 0.375F;
   }

   protected RenderType renderType() {
      return RenderTypes.endPortal();
   }
}
