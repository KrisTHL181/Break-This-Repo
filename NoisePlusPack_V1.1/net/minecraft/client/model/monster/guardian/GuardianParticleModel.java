package net.minecraft.client.model.monster.guardian;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Unit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuardianParticleModel extends Model<Unit> {
   public GuardianParticleModel(ModelPart p_454598_) {
      super(p_454598_, RenderTypes::entityCutoutNoCull);
   }
}
