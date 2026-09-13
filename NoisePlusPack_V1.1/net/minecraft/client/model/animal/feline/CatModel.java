package net.minecraft.client.model.animal.feline;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CatModel extends FelineModel<CatRenderState> {
   public static final MeshTransformer CAT_TRANSFORMER = MeshTransformer.scaling(0.8F);

   public CatModel(ModelPart p_459866_) {
      super(p_459866_);
   }
}
