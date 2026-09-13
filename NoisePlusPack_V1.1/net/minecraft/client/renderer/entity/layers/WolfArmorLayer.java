package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Crackiness;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WolfArmorLayer extends RenderLayer<WolfRenderState, WolfModel> {
   private final WolfModel adultModel;
   private final WolfModel babyModel;
   private final EquipmentLayerRenderer equipmentRenderer;
   private static final Map<Crackiness.Level, Identifier> ARMOR_CRACK_LOCATIONS = Map.of(
      Crackiness.Level.LOW,
      Identifier.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_low.png"),
      Crackiness.Level.MEDIUM,
      Identifier.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_medium.png"),
      Crackiness.Level.HIGH,
      Identifier.withDefaultNamespace("textures/entity/wolf/wolf_armor_crackiness_high.png")
   );

   public WolfArmorLayer(RenderLayerParent<WolfRenderState, WolfModel> p_329010_, EntityModelSet p_329062_, EquipmentLayerRenderer p_364552_) {
      super(p_329010_);
      this.adultModel = new WolfModel(p_329062_.bakeLayer(ModelLayers.WOLF_ARMOR));
      this.babyModel = new WolfModel(p_329062_.bakeLayer(ModelLayers.WOLF_BABY_ARMOR));
      this.equipmentRenderer = p_364552_;
   }

   public void submit(PoseStack p_426050_, SubmitNodeCollector p_431066_, int p_423107_, WolfRenderState p_423591_, float p_430412_, float p_424970_) {
      ItemStack itemstack = p_423591_.bodyArmorItem;
      Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
      if (equippable != null && !equippable.assetId().isEmpty()) {
         WolfModel wolfmodel = p_423591_.isBaby ? this.babyModel : this.adultModel;
         this.equipmentRenderer
            .renderLayers(
               EquipmentClientInfo.LayerType.WOLF_BODY,
               equippable.assetId().get(),
               wolfmodel,
               p_423591_,
               itemstack,
               p_426050_,
               p_431066_,
               p_423107_,
               p_423591_.outlineColor
            );
         this.maybeRenderCracks(p_426050_, p_431066_, p_423107_, itemstack, wolfmodel, p_423591_);
      }
   }

   private void maybeRenderCracks(
      PoseStack p_332031_, SubmitNodeCollector p_431668_, int p_329468_, ItemStack p_332244_, Model<WolfRenderState> p_365074_, WolfRenderState p_427448_
   ) {
      Crackiness.Level crackiness$level = Crackiness.WOLF_ARMOR.byDamage(p_332244_);
      if (crackiness$level != Crackiness.Level.NONE) {
         Identifier identifier = ARMOR_CRACK_LOCATIONS.get(crackiness$level);
         p_431668_.submitModel(
            p_365074_, p_427448_, p_332031_, RenderTypes.armorTranslucent(identifier), p_329468_, OverlayTexture.NO_OVERLAY, p_427448_.outlineColor, null
         );
      }
   }
}
