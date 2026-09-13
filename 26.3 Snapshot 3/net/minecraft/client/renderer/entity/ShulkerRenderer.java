package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.shulker.ShulkerModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.ShulkerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ShulkerRenderer extends MobRenderer<Shulker, ShulkerRenderState, ShulkerModel> {
   private static final Identifier DEFAULT_TEXTURE_LOCATION = Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION.texture().withPath(path -> "textures/" + path + ".png");
   private static final Identifier[] TEXTURE_LOCATION = Sheets.SHULKER_TEXTURE_LOCATION
      .stream()
      .map(location -> location.texture().withPath(path -> "textures/" + path + ".png"))
      .toArray(Identifier[]::new);

   public ShulkerRenderer(final EntityRendererProvider.Context context) {
      super(context, new ShulkerModel(context.bakeLayer(ModelLayers.SHULKER)), 0.0F);
   }

   public Vec3 getRenderOffset(final ShulkerRenderState state) {
      return state.renderOffset;
   }

   public boolean shouldRender(final Shulker entity, final Frustum culler, final double camX, final double camY, final double camZ) {
      if (super.shouldRender(entity, culler, camX, camY, camZ)) {
         return true;
      }

      Vec3 startPos = entity.getRenderPosition(0.0F);
      if (startPos == null) {
         return false;
      }

      EntityType<?> type = entity.getType();
      float halfHeight = type.getHeight() / 2.0F;
      float halfWidth = type.getWidth() / 2.0F;
      Vec3 targetPos = Vec3.atBottomCenterOf(entity.blockPosition());
      return culler.isVisible(
         new AABB(startPos.x, startPos.y + halfHeight, startPos.z, targetPos.x, targetPos.y + halfHeight, targetPos.z)
            .inflate(halfWidth, halfHeight, halfWidth)
      );
   }

   public Identifier getTextureLocation(final ShulkerRenderState state) {
      return getTextureLocation(state.color);
   }

   public ShulkerRenderState createRenderState() {
      return new ShulkerRenderState();
   }

   public void extractRenderState(final Shulker entity, final ShulkerRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.renderOffset = Objects.requireNonNullElse(entity.getRenderPosition(partialTicks), Vec3.ZERO);
      state.color = entity.getColor();
      state.peekAmount = entity.getClientPeekAmount(partialTicks);
      state.yHeadRot = entity.yHeadRot;
      state.yBodyRot = entity.yBodyRot;
      state.attachFace = entity.getAttachFace();
   }

   public static Identifier getTextureLocation(final @Nullable DyeColor color) {
      return color == null ? DEFAULT_TEXTURE_LOCATION : TEXTURE_LOCATION[color.getId()];
   }

   protected void setupRotations(final ShulkerRenderState state, final PoseStack poseStack, final float bodyRot, final float entityScale) {
      super.setupRotations(state, poseStack, bodyRot + 180.0F, entityScale);
      poseStack.rotateAround(state.attachFace.getOpposite().getRotation(), 0.0F, 0.5F, 0.0F);
   }
}
