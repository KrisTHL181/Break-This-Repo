package net.minecraft.client.renderer.entity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class EntityRenderDispatcher implements ResourceManagerReloadListener {
   private Map<EntityType<?>, EntityRenderer<?, ?>> renderers = ImmutableMap.of();
   private Map<PlayerModelType, AvatarRenderer<AbstractClientPlayer>> playerRenderers = Map.of();
   private Map<PlayerModelType, AvatarRenderer<ClientMannequin>> mannequinRenderers = Map.of();
   public final TextureManager textureManager;
   public @Nullable Camera camera;
   public Entity crosshairPickEntity;
   private final ItemModelResolver itemModelResolver;
   private final MapRenderer mapRenderer;
   private final BlockRenderDispatcher blockRenderDispatcher;
   private final ItemInHandRenderer itemInHandRenderer;
   private final AtlasManager atlasManager;
   private final Font font;
   public final Options options;
   private final Supplier<EntityModelSet> entityModels;
   private final EquipmentAssetManager equipmentAssets;
   private final PlayerSkinRenderCache playerSkinRenderCache;

   public <E extends Entity> int getPackedLightCoords(E p_114395_, float p_114396_) {
      return this.getRenderer(p_114395_).getPackedLightCoords(p_114395_, p_114396_);
   }

   public EntityRenderDispatcher(
      Minecraft p_234579_,
      TextureManager p_234580_,
      ItemModelResolver p_376277_,
      MapRenderer p_363170_,
      BlockRenderDispatcher p_234582_,
      AtlasManager p_431150_,
      Font p_234583_,
      Options p_234584_,
      Supplier<EntityModelSet> p_377712_,
      EquipmentAssetManager p_377123_,
      PlayerSkinRenderCache p_425801_
   ) {
      this.textureManager = p_234580_;
      this.itemModelResolver = p_376277_;
      this.mapRenderer = p_363170_;
      this.atlasManager = p_431150_;
      this.playerSkinRenderCache = p_425801_;
      this.itemInHandRenderer = new ItemInHandRenderer(p_234579_, this, p_376277_);
      this.blockRenderDispatcher = p_234582_;
      this.font = p_234583_;
      this.options = p_234584_;
      this.entityModels = p_377712_;
      this.equipmentAssets = p_377123_;
   }

   public <T extends Entity> EntityRenderer<? super T, ?> getRenderer(T p_114383_) {
      return switch (p_114383_) {
         case AbstractClientPlayer abstractclientplayer -> this.getAvatarRenderer(
            (Map<PlayerModelType, AvatarRenderer<T>>)this.playerRenderers, (T)abstractclientplayer
         );
         case ClientMannequin clientmannequin -> this.getAvatarRenderer((Map<PlayerModelType, AvatarRenderer<T>>)this.mannequinRenderers, (T)clientmannequin);
         default -> (EntityRenderer)this.renderers.get(p_114383_.getType());
      };
   }

   public AvatarRenderer<AbstractClientPlayer> getPlayerRenderer(AbstractClientPlayer p_430893_) {
      return this.getAvatarRenderer(this.playerRenderers, p_430893_);
   }

   private <T extends Avatar & ClientAvatarEntity> AvatarRenderer<T> getAvatarRenderer(Map<PlayerModelType, AvatarRenderer<T>> p_426047_, T p_422466_) {
      PlayerModelType playermodeltype = p_422466_.getSkin().model();
      AvatarRenderer<T> avatarrenderer = p_426047_.get(playermodeltype);
      return avatarrenderer != null ? avatarrenderer : p_426047_.get(PlayerModelType.WIDE);
   }

   public <S extends EntityRenderState> EntityRenderer<?, ? super S> getRenderer(S p_397828_) {
      if (p_397828_ instanceof AvatarRenderState avatarrenderstate) {
         PlayerModelType playermodeltype = avatarrenderstate.skin.model();
         EntityRenderer<? extends Avatar, ?> entityrenderer = (EntityRenderer<? extends Avatar, ?>)this.playerRenderers.get(playermodeltype);
         return (EntityRenderer<?, ? super S>)(entityrenderer != null ? entityrenderer : (EntityRenderer)this.playerRenderers.get(PlayerModelType.WIDE));
      } else {
         return (EntityRenderer<?, ? super S>)this.renderers.get(p_397828_.entityType);
      }
   }

   public void prepare(Camera p_114410_, Entity p_114411_) {
      this.camera = p_114410_;
      this.crosshairPickEntity = p_114411_;
   }

   public <E extends Entity> boolean shouldRender(E p_114398_, Frustum p_114399_, double p_114400_, double p_114401_, double p_114402_) {
      EntityRenderer<? super E, ?> entityrenderer = this.getRenderer(p_114398_);
      return entityrenderer.shouldRender(p_114398_, p_114399_, p_114400_, p_114401_, p_114402_);
   }

   public <E extends Entity> EntityRenderState extractEntity(E p_428219_, float p_430777_) {
      EntityRenderer<? super E, ?> entityrenderer = this.getRenderer(p_428219_);

      try {
         return entityrenderer.createRenderState(p_428219_, p_430777_);
      } catch (Throwable throwable) {
         CrashReport crashreport = CrashReport.forThrowable(throwable, "Extracting render state for an entity in world");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Entity being extracted");
         p_428219_.fillCrashReportCategory(crashreportcategory);
         CrashReportCategory crashreportcategory1 = this.fillRendererDetails(entityrenderer, crashreport);
         crashreportcategory1.setDetail("Delta", p_430777_);
         throw new ReportedException(crashreport);
      }
   }

   public <S extends EntityRenderState> void submit(
      S p_428874_, CameraRenderState p_423138_, double p_424289_, double p_429411_, double p_427612_, PoseStack p_430125_, SubmitNodeCollector p_426766_
   ) {
      EntityRenderer<?, ? super S> entityrenderer = this.getRenderer(p_428874_);

      try {
         Vec3 vec3 = entityrenderer.getRenderOffset(p_428874_);
         double d2 = p_424289_ + vec3.x();
         double d0 = p_429411_ + vec3.y();
         double d1 = p_427612_ + vec3.z();
         p_430125_.pushPose();
         p_430125_.translate(d2, d0, d1);
         entityrenderer.submit(p_428874_, p_430125_, p_426766_, p_423138_);
         if (p_428874_.displayFireAnimation) {
            p_426766_.submitFlame(p_430125_, p_428874_, Mth.rotationAroundAxis(Mth.Y_AXIS, p_423138_.orientation, new Quaternionf()));
         }

         if (p_428874_ instanceof AvatarRenderState) {
            p_430125_.translate(-vec3.x(), -vec3.y(), -vec3.z());
         }

         if (!p_428874_.shadowPieces.isEmpty()) {
            p_426766_.submitShadow(p_430125_, p_428874_.shadowRadius, p_428874_.shadowPieces);
         }

         if (!(p_428874_ instanceof AvatarRenderState)) {
            p_430125_.translate(-vec3.x(), -vec3.y(), -vec3.z());
         }

         p_430125_.popPose();
      } catch (Throwable throwable) {
         CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering entity in world");
         CrashReportCategory crashreportcategory = crashreport.addCategory("EntityRenderState being rendered");
         p_428874_.fillCrashReportCategory(crashreportcategory);
         this.fillRendererDetails(entityrenderer, crashreport);
         throw new ReportedException(crashreport);
      }
   }

   private <S extends EntityRenderState> CrashReportCategory fillRendererDetails(EntityRenderer<?, S> p_396247_, CrashReport p_396589_) {
      CrashReportCategory crashreportcategory = p_396589_.addCategory("Renderer details");
      crashreportcategory.setDetail("Assigned renderer", p_396247_);
      return crashreportcategory;
   }

   public void resetCamera() {
      this.camera = null;
   }

   public double distanceToSqr(Entity p_114472_) {
      return this.camera.position().distanceToSqr(p_114472_.position());
   }

   public ItemInHandRenderer getItemInHandRenderer() {
      return this.itemInHandRenderer;
   }

   @Override
   public void onResourceManagerReload(ResourceManager p_174004_) {
      EntityRendererProvider.Context entityrendererprovider$context = new EntityRendererProvider.Context(
         this,
         this.itemModelResolver,
         this.mapRenderer,
         this.blockRenderDispatcher,
         p_174004_,
         this.entityModels.get(),
         this.equipmentAssets,
         this.atlasManager,
         this.font,
         this.playerSkinRenderCache
      );
      this.renderers = EntityRenderers.createEntityRenderers(entityrendererprovider$context);
      this.playerRenderers = EntityRenderers.createAvatarRenderers(entityrendererprovider$context);
      this.mannequinRenderers = EntityRenderers.createAvatarRenderers(entityrendererprovider$context);
   }
}
