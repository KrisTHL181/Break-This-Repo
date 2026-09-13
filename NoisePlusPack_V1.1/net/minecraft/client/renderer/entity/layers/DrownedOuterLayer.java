package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.DrownedModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DrownedOuterLayer extends RenderLayer<ZombieRenderState, DrownedModel> {
   private static final Identifier DROWNED_OUTER_LAYER_LOCATION = Identifier.withDefaultNamespace("textures/entity/zombie/drowned_outer_layer.png");
   private final DrownedModel model;
   private final DrownedModel babyModel;

   public DrownedOuterLayer(RenderLayerParent<ZombieRenderState, DrownedModel> p_174490_, EntityModelSet p_174491_) {
      super(p_174490_);
      this.model = new DrownedModel(p_174491_.bakeLayer(ModelLayers.DROWNED_OUTER_LAYER));
      this.babyModel = new DrownedModel(p_174491_.bakeLayer(ModelLayers.DROWNED_BABY_OUTER_LAYER));
   }

   public void submit(PoseStack p_429100_, SubmitNodeCollector p_422809_, int p_424026_, ZombieRenderState p_424631_, float p_422374_, float p_424373_) {
      DrownedModel drownedmodel = p_424631_.isBaby ? this.babyModel : this.model;
      coloredCutoutModelCopyLayerRender(drownedmodel, DROWNED_OUTER_LAYER_LOCATION, p_429100_, p_422809_, p_424026_, p_424631_, -1, 1);
   }
}
