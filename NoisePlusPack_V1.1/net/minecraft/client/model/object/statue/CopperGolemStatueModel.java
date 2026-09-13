package net.minecraft.client.model.object.statue;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CopperGolemStatueModel extends Model<Direction> {
   public CopperGolemStatueModel(ModelPart p_453702_) {
      super(p_453702_, RenderTypes::entityCutoutNoCull);
   }

   public void setupAnim(Direction p_458575_) {
      this.root.y = 0.0F;
      this.root.yRot = p_458575_.getOpposite().toYRot() * (float) (Math.PI / 180.0);
      this.root.zRot = (float) Math.PI;
   }
}
