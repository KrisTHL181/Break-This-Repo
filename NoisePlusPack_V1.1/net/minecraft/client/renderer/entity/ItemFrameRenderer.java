package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BlockStateDefinitions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemFrameRenderer<T extends ItemFrame> extends EntityRenderer<T, ItemFrameRenderState> {
   public static final int GLOW_FRAME_BRIGHTNESS = 5;
   public static final int BRIGHT_MAP_LIGHT_ADJUSTMENT = 30;
   private final ItemModelResolver itemModelResolver;
   private final MapRenderer mapRenderer;
   private final BlockRenderDispatcher blockRenderer;

   public ItemFrameRenderer(EntityRendererProvider.Context p_174204_) {
      super(p_174204_);
      this.itemModelResolver = p_174204_.getItemModelResolver();
      this.mapRenderer = p_174204_.getMapRenderer();
      this.blockRenderer = p_174204_.getBlockRenderDispatcher();
   }

   protected int getBlockLightLevel(T p_174216_, BlockPos p_174217_) {
      return p_174216_.getType() == EntityType.GLOW_ITEM_FRAME
         ? Math.max(5, super.getBlockLightLevel(p_174216_, p_174217_))
         : super.getBlockLightLevel(p_174216_, p_174217_);
   }

   public void submit(ItemFrameRenderState p_430553_, PoseStack p_431616_, SubmitNodeCollector p_423714_, CameraRenderState p_430139_) {
      super.submit(p_430553_, p_431616_, p_423714_, p_430139_);
      p_431616_.pushPose();
      Direction direction = p_430553_.direction;
      Vec3 vec3 = this.getRenderOffset(p_430553_);
      p_431616_.translate(-vec3.x(), -vec3.y(), -vec3.z());
      double d0 = 0.46875;
      p_431616_.translate(direction.getStepX() * 0.46875, direction.getStepY() * 0.46875, direction.getStepZ() * 0.46875);
      float f;
      float f1;
      if (direction.getAxis().isHorizontal()) {
         f = 0.0F;
         f1 = 180.0F - direction.toYRot();
      } else {
         f = -90 * direction.getAxisDirection().getStep();
         f1 = 180.0F;
      }

      p_431616_.mulPose(Axis.XP.rotationDegrees(f));
      p_431616_.mulPose(Axis.YP.rotationDegrees(f1));
      if (!p_430553_.isInvisible) {
         BlockState blockstate = BlockStateDefinitions.getItemFrameFakeState(p_430553_.isGlowFrame, p_430553_.mapId != null);
         BlockStateModel blockstatemodel = this.blockRenderer.getBlockModel(blockstate);
         p_431616_.pushPose();
         p_431616_.translate(-0.5F, -0.5F, -0.5F);
         p_423714_.submitBlockModel(
            p_431616_,
            RenderTypes.entitySolidZOffsetForward(TextureAtlas.LOCATION_BLOCKS),
            blockstatemodel,
            1.0F,
            1.0F,
            1.0F,
            p_430553_.lightCoords,
            OverlayTexture.NO_OVERLAY,
            p_430553_.outlineColor
         );
         p_431616_.popPose();
      }

      if (p_430553_.isInvisible) {
         p_431616_.translate(0.0F, 0.0F, 0.5F);
      } else {
         p_431616_.translate(0.0F, 0.0F, 0.4375F);
      }

      if (p_430553_.mapId != null) {
         int j = p_430553_.rotation % 4 * 2;
         p_431616_.mulPose(Axis.ZP.rotationDegrees(j * 360.0F / 8.0F));
         p_431616_.mulPose(Axis.ZP.rotationDegrees(180.0F));
         float f2 = 0.0078125F;
         p_431616_.scale(0.0078125F, 0.0078125F, 0.0078125F);
         p_431616_.translate(-64.0F, -64.0F, 0.0F);
         p_431616_.translate(0.0F, 0.0F, -1.0F);
         int i = this.getLightCoords(p_430553_.isGlowFrame, 15728850, p_430553_.lightCoords);
         this.mapRenderer.render(p_430553_.mapRenderState, p_431616_, p_423714_, true, i);
      } else if (!p_430553_.item.isEmpty()) {
         p_431616_.mulPose(Axis.ZP.rotationDegrees(p_430553_.rotation * 360.0F / 8.0F));
         int k = this.getLightCoords(p_430553_.isGlowFrame, 15728880, p_430553_.lightCoords);
         p_431616_.scale(0.5F, 0.5F, 0.5F);
         p_430553_.item.submit(p_431616_, p_423714_, k, OverlayTexture.NO_OVERLAY, p_430553_.outlineColor);
      }

      p_431616_.popPose();
   }

   private int getLightCoords(boolean p_368253_, int p_174210_, int p_174211_) {
      return p_368253_ ? p_174210_ : p_174211_;
   }

   public Vec3 getRenderOffset(ItemFrameRenderState p_368370_) {
      return new Vec3(p_368370_.direction.getStepX() * 0.3F, -0.25, p_368370_.direction.getStepZ() * 0.3F);
   }

   protected boolean shouldShowName(T p_115091_, double p_366137_) {
      return Minecraft.renderNames() && this.entityRenderDispatcher.crosshairPickEntity == p_115091_ && p_115091_.getItem().getCustomName() != null;
   }

   protected Component getNameTag(T p_364863_) {
      return p_364863_.getItem().getHoverName();
   }

   public ItemFrameRenderState createRenderState() {
      return new ItemFrameRenderState();
   }

   public void extractRenderState(T p_369136_, ItemFrameRenderState p_364469_, float p_366511_) {
      super.extractRenderState(p_369136_, p_364469_, p_366511_);
      p_364469_.direction = p_369136_.getDirection();
      ItemStack itemstack = p_369136_.getItem();
      this.itemModelResolver.updateForNonLiving(p_364469_.item, itemstack, ItemDisplayContext.FIXED, p_369136_);
      p_364469_.rotation = p_369136_.getRotation();
      p_364469_.isGlowFrame = p_369136_.getType() == EntityType.GLOW_ITEM_FRAME;
      p_364469_.mapId = null;
      if (!itemstack.isEmpty()) {
         MapId mapid = p_369136_.getFramedMapId(itemstack);
         if (mapid != null) {
            MapItemSavedData mapitemsaveddata = p_369136_.level().getMapData(mapid);
            if (mapitemsaveddata != null) {
               this.mapRenderer.extractRenderState(mapid, mapitemsaveddata, p_364469_.mapRenderState);
               p_364469_.mapId = mapid;
            }
         }
      }
   }
}
