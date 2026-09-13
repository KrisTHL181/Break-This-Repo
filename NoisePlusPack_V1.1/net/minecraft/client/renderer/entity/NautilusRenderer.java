package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.nautilus.NautilusArmorModel;
import net.minecraft.client.model.animal.nautilus.NautilusModel;
import net.minecraft.client.model.animal.nautilus.NautilusSaddleModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.NautilusRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NautilusRenderer<T extends AbstractNautilus> extends AgeableMobRenderer<T, NautilusRenderState, NautilusModel> {
   private static final Identifier NAUTILUS_LOCATION = Identifier.withDefaultNamespace("textures/entity/nautilus/nautilus.png");
   private static final Identifier NAUTILUS_BABY_LOCATION = Identifier.withDefaultNamespace("textures/entity/nautilus/nautilus_baby.png");

   public NautilusRenderer(EntityRendererProvider.Context p_455826_) {
      super(p_455826_, new NautilusModel(p_455826_.bakeLayer(ModelLayers.NAUTILUS)), new NautilusModel(p_455826_.bakeLayer(ModelLayers.NAUTILUS_BABY)), 0.7F);
      this.addLayer(
         new SimpleEquipmentLayer<>(
            this,
            p_455826_.getEquipmentRenderer(),
            EquipmentClientInfo.LayerType.NAUTILUS_BODY,
            p_451388_ -> p_451388_.bodyArmorItem,
            new NautilusArmorModel(p_455826_.bakeLayer(ModelLayers.NAUTILUS_ARMOR)),
            null
         )
      );
      this.addLayer(
         new SimpleEquipmentLayer<>(
            this,
            p_455826_.getEquipmentRenderer(),
            EquipmentClientInfo.LayerType.NAUTILUS_SADDLE,
            p_456153_ -> p_456153_.saddle,
            new NautilusSaddleModel(p_455826_.bakeLayer(ModelLayers.NAUTILUS_SADDLE)),
            null
         )
      );
   }

   public Identifier getTextureLocation(NautilusRenderState p_457526_) {
      return p_457526_.isBaby ? NAUTILUS_BABY_LOCATION : NAUTILUS_LOCATION;
   }

   public NautilusRenderState createRenderState() {
      return new NautilusRenderState();
   }

   public void extractRenderState(T p_455241_, NautilusRenderState p_459551_, float p_459760_) {
      super.extractRenderState(p_455241_, p_459551_, p_459760_);
      p_459551_.saddle = p_455241_.getItemBySlot(EquipmentSlot.SADDLE).copy();
      p_459551_.bodyArmorItem = p_455241_.getBodyArmorItem().copy();
   }
}
