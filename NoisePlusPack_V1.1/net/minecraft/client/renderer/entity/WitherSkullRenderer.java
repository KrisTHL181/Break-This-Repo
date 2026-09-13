package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.object.skull.SkullModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.WitherSkullRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WitherSkullRenderer extends EntityRenderer<WitherSkull, WitherSkullRenderState> {
   private static final Identifier WITHER_INVULNERABLE_LOCATION = Identifier.withDefaultNamespace("textures/entity/wither/wither_invulnerable.png");
   private static final Identifier WITHER_LOCATION = Identifier.withDefaultNamespace("textures/entity/wither/wither.png");
   private final SkullModel model;

   public WitherSkullRenderer(EntityRendererProvider.Context p_174449_) {
      super(p_174449_);
      this.model = new SkullModel(p_174449_.bakeLayer(ModelLayers.WITHER_SKULL));
   }

   public static LayerDefinition createSkullLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 35).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   protected int getBlockLightLevel(WitherSkull p_455180_, BlockPos p_116492_) {
      return 15;
   }

   public void submit(WitherSkullRenderState p_424969_, PoseStack p_431637_, SubmitNodeCollector p_430989_, CameraRenderState p_428328_) {
      p_431637_.pushPose();
      p_431637_.scale(-1.0F, -1.0F, 1.0F);
      p_430989_.submitModel(
         this.model,
         p_424969_.modelState,
         p_431637_,
         this.model.renderType(this.getTextureLocation(p_424969_)),
         p_424969_.lightCoords,
         OverlayTexture.NO_OVERLAY,
         p_424969_.outlineColor,
         null
      );
      p_431637_.popPose();
      super.submit(p_424969_, p_431637_, p_430989_, p_428328_);
   }

   private Identifier getTextureLocation(WitherSkullRenderState p_366519_) {
      return p_366519_.isDangerous ? WITHER_INVULNERABLE_LOCATION : WITHER_LOCATION;
   }

   public WitherSkullRenderState createRenderState() {
      return new WitherSkullRenderState();
   }

   public void extractRenderState(WitherSkull p_458754_, WitherSkullRenderState p_363928_, float p_365976_) {
      super.extractRenderState(p_458754_, p_363928_, p_365976_);
      p_363928_.isDangerous = p_458754_.isDangerous();
      p_363928_.modelState.animationPos = 0.0F;
      p_363928_.modelState.yRot = p_458754_.getYRot(p_365976_);
      p_363928_.modelState.xRot = p_458754_.getXRot(p_365976_);
   }
}
