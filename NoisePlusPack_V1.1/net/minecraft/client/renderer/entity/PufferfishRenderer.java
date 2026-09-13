package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.fish.PufferfishBigModel;
import net.minecraft.client.model.animal.fish.PufferfishMidModel;
import net.minecraft.client.model.animal.fish.PufferfishSmallModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.PufferfishRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PufferfishRenderer extends MobRenderer<Pufferfish, PufferfishRenderState, EntityModel<EntityRenderState>> {
   private static final Identifier PUFFER_LOCATION = Identifier.withDefaultNamespace("textures/entity/fish/pufferfish.png");
   private final EntityModel<EntityRenderState> small;
   private final EntityModel<EntityRenderState> mid;
   private final EntityModel<EntityRenderState> big = this.getModel();

   public PufferfishRenderer(EntityRendererProvider.Context p_174358_) {
      super(p_174358_, new PufferfishBigModel(p_174358_.bakeLayer(ModelLayers.PUFFERFISH_BIG)), 0.2F);
      this.mid = new PufferfishMidModel(p_174358_.bakeLayer(ModelLayers.PUFFERFISH_MEDIUM));
      this.small = new PufferfishSmallModel(p_174358_.bakeLayer(ModelLayers.PUFFERFISH_SMALL));
   }

   public Identifier getTextureLocation(PufferfishRenderState p_458353_) {
      return PUFFER_LOCATION;
   }

   public PufferfishRenderState createRenderState() {
      return new PufferfishRenderState();
   }

   protected float getShadowRadius(PufferfishRenderState p_376900_) {
      return 0.1F + 0.1F * p_376900_.puffState;
   }

   public void submit(PufferfishRenderState p_423620_, PoseStack p_429802_, SubmitNodeCollector p_426114_, CameraRenderState p_430239_) {
      this.model = switch (p_423620_.puffState) {
         case 0 -> this.small;
         case 1 -> this.mid;
         default -> this.big;
      };
      super.submit(p_423620_, p_429802_, p_426114_, p_430239_);
   }

   public void extractRenderState(Pufferfish p_455130_, PufferfishRenderState p_362078_, float p_368845_) {
      super.extractRenderState(p_455130_, p_362078_, p_368845_);
      p_362078_.puffState = p_455130_.getPuffState();
   }

   protected void setupRotations(PufferfishRenderState p_362931_, PoseStack p_115785_, float p_115786_, float p_115787_) {
      p_115785_.translate(0.0F, Mth.cos(p_362931_.ageInTicks * 0.05F) * 0.08F, 0.0F);
      super.setupRotations(p_362931_, p_115785_, p_115786_, p_115787_);
   }
}
