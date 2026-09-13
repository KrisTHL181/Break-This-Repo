package net.minecraft.client.model.monster.piglin;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ZombifiedPiglinRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ZombifiedPiglinModel extends AbstractPiglinModel<ZombifiedPiglinRenderState> {
    public ZombifiedPiglinModel(final ModelPart root) {
        super(root);
    }

    public void setupAnim(final ZombifiedPiglinRenderState state) {
        super.setupAnim(state);
        AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, state.isAggressive, state);
    }
}