package net.minecraft.client.model.animal.feline;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.FelineRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AdultOcelotModel extends AdultFelineModel<FelineRenderState> {
    public AdultOcelotModel(final ModelPart root) {
        super(root);
    }
}