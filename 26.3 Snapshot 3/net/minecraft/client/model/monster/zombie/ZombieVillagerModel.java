package net.minecraft.client.model.monster.zombie;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.VillagerLikeModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.state.ZombieVillagerRenderState;
import net.minecraft.world.entity.HumanoidArm;

public class ZombieVillagerModel<S extends ZombieVillagerRenderState> extends HumanoidModel<S> implements VillagerLikeModel<S> {
   public ZombieVillagerModel(final ModelPart root) {
      super(root);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
      PartDefinition root = mesh.getRoot();
      PartDefinition head = root.addOrReplaceChild(
         "head",
         new CubeListBuilder().texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F).texOffs(24, 0).addBox(-1.0F, -3.0F, -6.0F, 2.0F, 4.0F, 2.0F),
         PartPose.ZERO
      );
      PartDefinition hat = head.addOrReplaceChild(
         "hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.5F)), PartPose.ZERO
      );
      hat.addOrReplaceChild(
         "hat_rim",
         CubeListBuilder.create().texOffs(30, 47).addBox(-8.0F, -8.0F, -6.0F, 16.0F, 16.0F, 1.0F),
         PartPose.rotation((float) (-Math.PI / 2), 0.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(16, 20)
            .addBox(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F)
            .texOffs(0, 38)
            .addBox(-4.0F, 0.0F, -3.0F, 8.0F, 20.0F, 6.0F, new CubeDeformation(0.05F)),
         PartPose.ZERO
      );
      root.addOrReplaceChild(
         "right_arm", CubeListBuilder.create().texOffs(44, 22).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-5.0F, 2.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_arm", CubeListBuilder.create().texOffs(44, 22).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(5.0F, 2.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "right_leg", CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-2.0F, 12.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_leg", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(2.0F, 12.0F, 0.0F)
      );
      return LayerDefinition.create(mesh, 64, 64);
   }

   public static LayerDefinition createNoHatLayer() {
      return createBodyLayer().apply(mesh -> {
         mesh.getRoot().clearChild("head").clearRecursively();
         return mesh;
      });
   }

   public static ArmorModelSet<LayerDefinition> createArmorLayerSet(final CubeDeformation innerDeformation, final CubeDeformation outerDeformation) {
      return createArmorMeshSet(ZombieVillagerModel::createBaseArmorMesh, ADULT_ARMOR_PARTS_PER_SLOT, innerDeformation, outerDeformation)
         .map(mesh -> LayerDefinition.create(mesh, 64, 32));
   }

   private static MeshDefinition createBaseArmorMesh(final CubeDeformation g) {
      MeshDefinition mesh = HumanoidModel.createMesh(g, 0.0F);
      PartDefinition root = mesh.getRoot();
      PartDefinition head = root.addOrReplaceChild(
         "head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 8.0F, 8.0F, g), PartPose.ZERO
      );
      root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, g.extend(0.1F)), PartPose.ZERO);
      root.addOrReplaceChild(
         "right_leg",
         CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, g.extend(0.1F)),
         PartPose.offset(-2.0F, 12.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "left_leg",
         CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, g.extend(0.1F)),
         PartPose.offset(2.0F, 12.0F, 0.0F)
      );
      head.getChild("hat").addOrReplaceChild("hat_rim", CubeListBuilder.create(), PartPose.ZERO);
      return mesh;
   }

   public void setupAnim(final S state) {
      super.setupAnim(state);
      AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, state.isAggressive, state);
   }

   public void translateToArms(final ZombieVillagerRenderState state, final PoseStack outputPoseStack) {
      this.translateToHand(state, HumanoidArm.RIGHT, outputPoseStack);
   }
}
