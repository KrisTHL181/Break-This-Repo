package net.minecraft.client.model.animal.panda;

import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.PandaRenderState;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PandaModel extends QuadrupedModel<PandaRenderState> {
    public PandaModel(final ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(0, 6)
                .addBox(-6.5F, -5.0F, -4.0F, 13.0F, 10.0F, 9.0F)
                .texOffs(45, 16)
                .addBox("nose", -3.5F, 0.0F, -6.0F, 7.0F, 5.0F, 2.0F)
                .texOffs(52, 25)
                .addBox("left_ear", 3.5F, -8.0F, -1.0F, 5.0F, 4.0F, 1.0F)
                .texOffs(52, 25)
                .addBox("right_ear", -8.5F, -8.0F, -1.0F, 5.0F, 4.0F, 1.0F),
            PartPose.offset(0.0F, 11.5F, -17.0F)
        );
        root.addOrReplaceChild(
            "body",
            CubeListBuilder.create().texOffs(0, 25).addBox(-9.5F, -13.0F, -6.5F, 19.0F, 26.0F, 13.0F),
            PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
        );
        int legH = 9;
        int legW = 6;
        CubeListBuilder leg = CubeListBuilder.create().texOffs(40, 0).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 9.0F, 6.0F);
        root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-5.5F, 15.0F, 9.0F));
        root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(5.5F, 15.0F, 9.0F));
        root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-5.5F, 15.0F, -9.0F));
        root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(5.5F, 15.0F, -9.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public void setupAnim(final PandaRenderState state) {
        super.setupAnim(state);
        if (state.isUnhappy) {
            this.head.yRot = 0.35F * Mth.sin(0.6F * state.ageInTicks);
            this.head.zRot = 0.35F * Mth.sin(0.6F * state.ageInTicks);
            this.rightFrontLeg.xRot = -0.75F * Mth.sin(0.3F * state.ageInTicks);
            this.leftFrontLeg.xRot = 0.75F * Mth.sin(0.3F * state.ageInTicks);
        } else {
            this.head.zRot = 0.0F;
        }

        if (state.isSneezing) {
            if (state.sneezeTime < 15) {
                this.head.xRot = (float) (-Math.PI / 4) * state.sneezeTime / 14.0F;
            } else if (state.sneezeTime < 20) {
                float internalSneezePos = (state.sneezeTime - 15) / 5;
                this.head.xRot = (float) (-Math.PI / 4) + (float) (Math.PI / 4) * internalSneezePos;
            }
        }

        if (state.sitAmount > 0.0F) {
            this.animateSitting(state);
            if (state.isEating) {
                this.head.xRot = (float) (Math.PI / 2) + 0.2F * Mth.sin(state.ageInTicks * 0.6F);
                this.rightFrontLeg.xRot = -0.4F - 0.2F * Mth.sin(state.ageInTicks * 0.6F);
                this.leftFrontLeg.xRot = -0.4F - 0.2F * Mth.sin(state.ageInTicks * 0.6F);
            }

            if (state.isScared) {
                this.head.xRot = 2.1707964F;
                this.rightFrontLeg.xRot = -0.9F;
                this.leftFrontLeg.xRot = -0.9F;
            }
        } else {
            this.rightHindLeg.zRot = 0.0F;
            this.leftHindLeg.zRot = 0.0F;
            this.rightFrontLeg.zRot = 0.0F;
            this.leftFrontLeg.zRot = 0.0F;
        }

        if (state.lieOnBackAmount > 0.0F) {
            this.rightHindLeg.xRot = -0.6F * Mth.sin(state.ageInTicks * 0.15F);
            this.leftHindLeg.xRot = 0.6F * Mth.sin(state.ageInTicks * 0.15F);
            this.rightFrontLeg.xRot = 0.3F * Mth.sin(state.ageInTicks * 0.25F);
            this.leftFrontLeg.xRot = -0.3F * Mth.sin(state.ageInTicks * 0.25F);
            this.head.xRot = Mth.rotLerpRad(state.lieOnBackAmount, this.head.xRot, (float) (Math.PI / 2));
        }

        if (state.rollAmount > 0.0F) {
            this.head.xRot = Mth.rotLerpRad(state.rollAmount, this.head.xRot, 2.0561945F);
            this.rightHindLeg.xRot = -0.5F * Mth.sin(state.ageInTicks * 0.5F);
            this.leftHindLeg.xRot = 0.5F * Mth.sin(state.ageInTicks * 0.5F);
            this.rightFrontLeg.xRot = 0.5F * Mth.sin(state.ageInTicks * 0.5F);
            this.leftFrontLeg.xRot = -0.5F * Mth.sin(state.ageInTicks * 0.5F);
        }
    }

    protected void animateSitting(final PandaRenderState state) {
        this.body.xRot = Mth.rotLerpRad(state.sitAmount, this.body.xRot, 1.7407963F);
        this.head.xRot = Mth.rotLerpRad(state.sitAmount, this.head.xRot, (float) (Math.PI / 2));
        this.rightFrontLeg.zRot = -0.27079642F;
        this.leftFrontLeg.zRot = 0.27079642F;
        this.rightHindLeg.zRot = 0.5707964F;
        this.leftHindLeg.zRot = -0.5707964F;
    }
}