package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Arrays;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.IllusionerRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.illager.Illusioner;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IllusionerRenderer extends IllagerRenderer<Illusioner, IllusionerRenderState> {
   private static final Identifier ILLUSIONER = Identifier.withDefaultNamespace("textures/entity/illager/illusioner.png");

   public IllusionerRenderer(EntityRendererProvider.Context p_174186_) {
      super(p_174186_, new IllagerModel<>(p_174186_.bakeLayer(ModelLayers.ILLUSIONER)), 0.5F);
      this.addLayer(
         new ItemInHandLayer<IllusionerRenderState, IllagerModel<IllusionerRenderState>>(this) {
            public void submit(
               PoseStack p_426491_, SubmitNodeCollector p_427039_, int p_428554_, IllusionerRenderState p_431279_, float p_424321_, float p_427359_
            ) {
               if (p_431279_.isCastingSpell || p_431279_.isAggressive) {
                  super.submit(p_426491_, p_427039_, p_428554_, p_431279_, p_424321_, p_427359_);
               }
            }
         }
      );
      this.model.getHat().visible = true;
   }

   public Identifier getTextureLocation(IllusionerRenderState p_450626_) {
      return ILLUSIONER;
   }

   public IllusionerRenderState createRenderState() {
      return new IllusionerRenderState();
   }

   public void extractRenderState(Illusioner p_456991_, IllusionerRenderState p_458854_, float p_368555_) {
      super.extractRenderState(p_456991_, p_458854_, p_368555_);
      Vec3[] avec3 = p_456991_.getIllusionOffsets(p_368555_);
      p_458854_.illusionOffsets = Arrays.copyOf(avec3, avec3.length);
      p_458854_.isCastingSpell = p_456991_.isCastingSpell();
   }

   public void submit(IllusionerRenderState p_429210_, PoseStack p_427619_, SubmitNodeCollector p_423215_, CameraRenderState p_427530_) {
      if (p_429210_.isInvisible) {
         Vec3[] avec3 = p_429210_.illusionOffsets;

         for (int i = 0; i < avec3.length; i++) {
            p_427619_.pushPose();
            p_427619_.translate(
               avec3[i].x + Mth.cos(i + p_429210_.ageInTicks * 0.5F) * 0.025,
               avec3[i].y + Mth.cos(i + p_429210_.ageInTicks * 0.75F) * 0.0125,
               avec3[i].z + Mth.cos(i + p_429210_.ageInTicks * 0.7F) * 0.025
            );
            super.submit(p_429210_, p_427619_, p_423215_, p_427530_);
            p_427619_.popPose();
         }
      } else {
         super.submit(p_429210_, p_427619_, p_423215_, p_427530_);
      }
   }

   protected boolean isBodyVisible(IllusionerRenderState p_361370_) {
      return true;
   }

   protected AABB getBoundingBoxForCulling(Illusioner p_456983_) {
      return super.getBoundingBoxForCulling(p_456983_).inflate(3.0, 0.0, 3.0);
   }
}
