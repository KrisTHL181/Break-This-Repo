package net.minecraft.client.model.monster.zombie;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BabyDrownedModel extends DrownedModel {
    public BabyDrownedModel(final ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer(final CubeDeformation g) {
        return BabyZombieModel.createBodyLayer(g);
    }
}