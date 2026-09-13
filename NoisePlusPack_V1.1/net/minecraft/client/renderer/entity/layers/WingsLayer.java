package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class WingsLayer<S extends HumanoidRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
   private final ElytraModel elytraModel;
   private final ElytraModel elytraBabyModel;
   private final EquipmentLayerRenderer equipmentRenderer;

   public WingsLayer(RenderLayerParent<S, M> p_366720_, EntityModelSet p_369504_, EquipmentLayerRenderer p_361718_) {
      super(p_366720_);
      this.elytraModel = new ElytraModel(p_369504_.bakeLayer(ModelLayers.ELYTRA));
      this.elytraBabyModel = new ElytraModel(p_369504_.bakeLayer(ModelLayers.ELYTRA_BABY));
      this.equipmentRenderer = p_361718_;
   }

   public void submit(PoseStack p_423573_, SubmitNodeCollector p_430991_, int p_423681_, S p_424203_, float p_425267_, float p_425536_) {
      ItemStack itemstack = p_424203_.chestEquipment;
      Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
      if (equippable != null && !equippable.assetId().isEmpty()) {
         Identifier identifier = getPlayerElytraTexture(p_424203_);
         ElytraModel elytramodel = p_424203_.isBaby ? this.elytraBabyModel : this.elytraModel;
         p_423573_.pushPose();
         p_423573_.translate(0.0F, 0.0F, 0.125F);
         this.equipmentRenderer
            .renderLayers(
               EquipmentClientInfo.LayerType.WINGS,
               equippable.assetId().get(),
               elytramodel,
               p_424203_,
               itemstack,
               p_423573_,
               p_430991_,
               p_423681_,
               identifier,
               p_424203_.outlineColor,
               0
            );
         p_423573_.popPose();
      }
   }

   private static @Nullable Identifier getPlayerElytraTexture(HumanoidRenderState p_364125_) {
      if (p_364125_ instanceof AvatarRenderState avatarrenderstate) {
         PlayerSkin playerskin = avatarrenderstate.skin;
         if (playerskin.elytra() != null) {
            return playerskin.elytra().texturePath();
         }

         if (playerskin.cape() != null && avatarrenderstate.showCape) {
            return playerskin.cape().texturePath();
         }
      }

      return null;
   }
}
