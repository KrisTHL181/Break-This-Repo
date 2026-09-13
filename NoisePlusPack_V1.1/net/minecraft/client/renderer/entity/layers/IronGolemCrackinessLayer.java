package net.minecraft.client.renderer.entity.layers;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.model.animal.golem.IronGolemModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Crackiness;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IronGolemCrackinessLayer extends RenderLayer<IronGolemRenderState, IronGolemModel> {
   private static final Map<Crackiness.Level, Identifier> identifiers = ImmutableMap.of(
      Crackiness.Level.LOW,
      Identifier.withDefaultNamespace("textures/entity/iron_golem/iron_golem_crackiness_low.png"),
      Crackiness.Level.MEDIUM,
      Identifier.withDefaultNamespace("textures/entity/iron_golem/iron_golem_crackiness_medium.png"),
      Crackiness.Level.HIGH,
      Identifier.withDefaultNamespace("textures/entity/iron_golem/iron_golem_crackiness_high.png")
   );

   public IronGolemCrackinessLayer(RenderLayerParent<IronGolemRenderState, IronGolemModel> p_117135_) {
      super(p_117135_);
   }

   public void submit(PoseStack p_429148_, SubmitNodeCollector p_430393_, int p_427802_, IronGolemRenderState p_423813_, float p_423313_, float p_427138_) {
      if (!p_423813_.isInvisible) {
         Crackiness.Level crackiness$level = p_423813_.crackiness;
         if (crackiness$level != Crackiness.Level.NONE) {
            Identifier identifier = identifiers.get(crackiness$level);
            renderColoredCutoutModel(this.getParentModel(), identifier, p_429148_, p_430393_, p_427802_, p_423813_, -1, 1);
         }
      }
   }
}
