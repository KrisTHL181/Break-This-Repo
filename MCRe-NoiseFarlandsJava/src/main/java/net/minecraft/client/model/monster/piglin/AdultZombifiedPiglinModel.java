package net.minecraft.client.model.monster.piglin;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AdultZombifiedPiglinModel extends ZombifiedPiglinModel {
    public AdultZombifiedPiglinModel(final ModelPart root) {
        super(root);
    }

    @Override
    protected float getDefaultEarAngleInDegrees() {
        return 30.0F;
    }

    public static LayerDefinition createBodyLayer() {
        return AdultPiglinModel.createBodyLayer();
    }
}