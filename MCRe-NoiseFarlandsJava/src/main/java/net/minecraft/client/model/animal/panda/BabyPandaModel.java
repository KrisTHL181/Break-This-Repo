package net.minecraft.client.model.animal.panda;

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
public class BabyPandaModel extends PandaModel {
    public BabyPandaModel(final ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "body", CubeListBuilder.create().texOffs(0, 11).addBox(-4.5F, -3.5F, -5.5F, 9.0F, 7.0F, 11.0F), PartPose.offset(0.0F, 18.5F, 2.5F)
        );
        root.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-3.5F, -3.0F, -5.0F, 7.0F, 6.0F, 5.0F)
                .texOffs(24, 6)
                .addBox(-2.0F, 1.0F, -6.0F, 4.0F, 2.0F, 1.0F)
                .texOffs(24, 0)
                .addBox(-4.5F, -4.0F, -3.5F, 3.0F, 3.0F, 1.0F)
                .texOffs(33, 0)
                .addBox(1.5F, -4.0F, -3.5F, 3.0F, 3.0F, 1.0F),
            PartPose.offset(0.0F, 19.0F, -3.0F)
        );
        root.addOrReplaceChild(
            "right_hind_leg", CubeListBuilder.create().texOffs(0, 34).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 2.0F, 3.0F), PartPose.offset(-3.0F, 22.0F, 6.5F)
        );
        root.addOrReplaceChild(
            "left_hind_leg", CubeListBuilder.create().texOffs(12, 34).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 2.0F, 3.0F), PartPose.offset(3.0F, 22.0F, 6.5F)
        );
        root.addOrReplaceChild(
            "right_front_leg", CubeListBuilder.create().texOffs(0, 29).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 2.0F, 3.0F), PartPose.offset(-3.0F, 22.0F, -1.5F)
        );
        root.addOrReplaceChild(
            "left_front_leg", CubeListBuilder.create().texOffs(12, 29).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 2.0F, 3.0F), PartPose.offset(3.0F, 22.0F, -1.5F)
        );
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    protected void animateSitting(final PandaRenderState state) {
        this.body.xRot = Mth.rotLerpRad(state.sitAmount, this.body.xRot, (float) (Math.PI / 18));
        this.body.z = Mth.lerp(state.sitAmount, this.body.z, -1.5F);
        this.head.z = Mth.lerp(state.sitAmount, this.head.z, -11.5F);
        this.head.y = Mth.lerp(state.sitAmount, this.head.y, 17.5F);
        this.rightFrontLeg.z = Mth.lerp(state.sitAmount, this.rightFrontLeg.z, -5.0F);
        this.leftFrontLeg.z = Mth.lerp(state.sitAmount, this.leftFrontLeg.z, -5.0F);
        this.rightHindLeg.z = Mth.lerp(state.sitAmount, this.rightHindLeg.z, 3.0F);
        this.leftHindLeg.z = Mth.lerp(state.sitAmount, this.leftHindLeg.z, 3.0F);
        this.rightFrontLeg.zRot = -0.27079642F;
        this.leftFrontLeg.zRot = 0.27079642F;
        this.rightHindLeg.zRot = 0.5707964F;
        this.leftHindLeg.zRot = -0.5707964F;
    }
}