package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerCapeModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CapeLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
   private final HumanoidModel<AvatarRenderState> model;
   private final EquipmentAssetManager equipmentAssets;

   public CapeLayer(RenderLayerParent<AvatarRenderState, PlayerModel> p_116602_, EntityModelSet p_364158_, EquipmentAssetManager p_378632_) {
      super(p_116602_);
      this.model = new PlayerCapeModel(p_364158_.bakeLayer(ModelLayers.PLAYER_CAPE));
      this.equipmentAssets = p_378632_;
   }

   private boolean hasLayer(ItemStack p_362441_, EquipmentClientInfo.LayerType p_377432_) {
      Equippable equippable = p_362441_.get(DataComponents.EQUIPPABLE);
      if (equippable != null && !equippable.assetId().isEmpty()) {
         EquipmentClientInfo equipmentclientinfo = this.equipmentAssets.get(equippable.assetId().get());
         return !equipmentclientinfo.getLayers(p_377432_).isEmpty();
      } else {
         return false;
      }
   }

   public void submit(PoseStack p_431599_, SubmitNodeCollector p_430860_, int p_427257_, AvatarRenderState p_428454_, float p_429917_, float p_424453_) {
      if (!p_428454_.isInvisible && p_428454_.showCape) {
         PlayerSkin playerskin = p_428454_.skin;
         if (playerskin.cape() != null && !this.hasLayer(p_428454_.chestEquipment, EquipmentClientInfo.LayerType.WINGS)) {
            p_431599_.pushPose();
            if (this.hasLayer(p_428454_.chestEquipment, EquipmentClientInfo.LayerType.HUMANOID)) {
               p_431599_.translate(0.0F, -0.053125F, 0.06875F);
            }

            p_430860_.submitModel(
               this.model,
               p_428454_,
               p_431599_,
               RenderTypes.entitySolid(playerskin.cape().texturePath()),
               p_427257_,
               OverlayTexture.NO_OVERLAY,
               p_428454_.outlineColor,
               null
            );
            p_431599_.popPose();
         }
      }
   }
}
