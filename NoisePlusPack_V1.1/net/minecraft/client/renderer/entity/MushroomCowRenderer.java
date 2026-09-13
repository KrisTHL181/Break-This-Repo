package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.MushroomCowMushroomLayer;
import net.minecraft.client.renderer.entity.state.MushroomCowRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MushroomCowRenderer extends AgeableMobRenderer<MushroomCow, MushroomCowRenderState, CowModel> {
   private static final Map<MushroomCow.Variant, Identifier> TEXTURES = Util.make(Maps.newHashMap(), p_448328_ -> {
      p_448328_.put(MushroomCow.Variant.BROWN, Identifier.withDefaultNamespace("textures/entity/cow/brown_mooshroom.png"));
      p_448328_.put(MushroomCow.Variant.RED, Identifier.withDefaultNamespace("textures/entity/cow/red_mooshroom.png"));
   });

   public MushroomCowRenderer(EntityRendererProvider.Context p_174324_) {
      super(p_174324_, new CowModel(p_174324_.bakeLayer(ModelLayers.MOOSHROOM)), new CowModel(p_174324_.bakeLayer(ModelLayers.MOOSHROOM_BABY)), 0.7F);
      this.addLayer(new MushroomCowMushroomLayer(this, p_174324_.getBlockRenderDispatcher()));
   }

   public Identifier getTextureLocation(MushroomCowRenderState p_365464_) {
      return TEXTURES.get(p_365464_.variant);
   }

   public MushroomCowRenderState createRenderState() {
      return new MushroomCowRenderState();
   }

   public void extractRenderState(MushroomCow p_458883_, MushroomCowRenderState p_366405_, float p_362405_) {
      super.extractRenderState(p_458883_, p_366405_, p_362405_);
      p_366405_.variant = p_458883_.getVariant();
   }
}
