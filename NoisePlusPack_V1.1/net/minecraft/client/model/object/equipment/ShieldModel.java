package net.minecraft.client.model.object.equipment;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Unit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShieldModel extends Model<Unit> {
   private static final String PLATE = "plate";
   private static final String HANDLE = "handle";
   private static final int SHIELD_WIDTH = 10;
   private static final int SHIELD_HEIGHT = 20;
   private final ModelPart plate;
   private final ModelPart handle;

   public ShieldModel(ModelPart p_455594_) {
      super(p_455594_, RenderTypes::entitySolid);
      this.plate = p_455594_.getChild("plate");
      this.handle = p_455594_.getChild("handle");
   }

   public static LayerDefinition createLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild("plate", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -11.0F, -2.0F, 12.0F, 22.0F, 1.0F), PartPose.ZERO);
      partdefinition.addOrReplaceChild("handle", CubeListBuilder.create().texOffs(26, 0).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 6.0F, 6.0F), PartPose.ZERO);
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public ModelPart plate() {
      return this.plate;
   }

   public ModelPart handle() {
      return this.handle;
   }
}
