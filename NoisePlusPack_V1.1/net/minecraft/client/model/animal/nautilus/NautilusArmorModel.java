package net.minecraft.client.model.animal.nautilus;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NautilusArmorModel extends NautilusModel {
   private final ModelPart nautilus;
   private final ModelPart shell;

   public NautilusArmorModel(ModelPart p_454587_) {
      super(p_454587_);
      this.nautilus = p_454587_.getChild("root");
      this.shell = this.nautilus.getChild("shell");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = createBodyMesh();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 29.0F, -6.0F));
      PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild(
         "shell",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-7.0F, -10.0F, -7.0F, 14.0F, 10.0F, 16.0F, new CubeDeformation(0.01F))
            .texOffs(0, 26)
            .addBox(-7.0F, 0.0F, -7.0F, 14.0F, 8.0F, 20.0F, new CubeDeformation(0.01F))
            .texOffs(48, 26)
            .addBox(-7.0F, 0.0F, 6.0F, 14.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -13.0F, 5.0F)
      );
      return LayerDefinition.create(meshdefinition, 128, 128);
   }
}
