package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.model.AdultAndBabyModelPair;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.CowRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.CowVariant;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CowRenderer extends MobRenderer<Cow, CowRenderState, CowModel> {
   private final Map<CowVariant.ModelType, AdultAndBabyModelPair<CowModel>> models;

   public CowRenderer(EntityRendererProvider.Context p_173956_) {
      super(p_173956_, new CowModel(p_173956_.bakeLayer(ModelLayers.COW)), 0.7F);
      this.models = bakeModels(p_173956_);
   }

   private static Map<CowVariant.ModelType, AdultAndBabyModelPair<CowModel>> bakeModels(EntityRendererProvider.Context p_395482_) {
      return Maps.newEnumMap(
         Map.of(
            CowVariant.ModelType.NORMAL,
            new AdultAndBabyModelPair<>(new CowModel(p_395482_.bakeLayer(ModelLayers.COW)), new CowModel(p_395482_.bakeLayer(ModelLayers.COW_BABY))),
            CowVariant.ModelType.WARM,
            new AdultAndBabyModelPair<>(new CowModel(p_395482_.bakeLayer(ModelLayers.WARM_COW)), new CowModel(p_395482_.bakeLayer(ModelLayers.WARM_COW_BABY))),
            CowVariant.ModelType.COLD,
            new AdultAndBabyModelPair<>(new CowModel(p_395482_.bakeLayer(ModelLayers.COLD_COW)), new CowModel(p_395482_.bakeLayer(ModelLayers.COLD_COW_BABY)))
         )
      );
   }

   public Identifier getTextureLocation(CowRenderState p_393210_) {
      return p_393210_.variant == null ? MissingTextureAtlasSprite.getLocation() : p_393210_.variant.modelAndTexture().asset().texturePath();
   }

   public CowRenderState createRenderState() {
      return new CowRenderState();
   }

   public void extractRenderState(Cow p_459217_, CowRenderState p_395845_, float p_367056_) {
      super.extractRenderState(p_459217_, p_395845_, p_367056_);
      p_395845_.variant = p_459217_.getVariant().value();
   }

   public void submit(CowRenderState p_429722_, PoseStack p_425829_, SubmitNodeCollector p_423074_, CameraRenderState p_422290_) {
      if (p_429722_.variant != null) {
         this.model = this.models.get(p_429722_.variant.modelAndTexture().model()).getModel(p_429722_.isBaby);
         super.submit(p_429722_, p_425829_, p_423074_, p_422290_);
      }
   }
}
