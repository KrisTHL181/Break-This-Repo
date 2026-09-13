package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HumanoidArmorLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>> extends RenderLayer<S, M> {
   private final ArmorModelSet<A> modelSet;
   private final ArmorModelSet<A> babyModelSet;
   private final EquipmentLayerRenderer equipmentRenderer;

   public HumanoidArmorLayer(RenderLayerParent<S, M> p_267286_, ArmorModelSet<A> p_422869_, EquipmentLayerRenderer p_369441_) {
      this(p_267286_, p_422869_, p_422869_, p_369441_);
   }

   public HumanoidArmorLayer(RenderLayerParent<S, M> p_364333_, ArmorModelSet<A> p_422636_, ArmorModelSet<A> p_428999_, EquipmentLayerRenderer p_361027_) {
      super(p_364333_);
      this.modelSet = p_422636_;
      this.babyModelSet = p_428999_;
      this.equipmentRenderer = p_361027_;
   }

   public static boolean shouldRender(ItemStack p_362744_, EquipmentSlot p_366990_) {
      Equippable equippable = p_362744_.get(DataComponents.EQUIPPABLE);
      return equippable != null && shouldRender(equippable, p_366990_);
   }

   private static boolean shouldRender(Equippable p_369539_, EquipmentSlot p_369578_) {
      return p_369539_.assetId().isPresent() && p_369539_.slot() == p_369578_;
   }

   public void submit(PoseStack p_422800_, SubmitNodeCollector p_425810_, int p_424719_, S p_428929_, float p_423314_, float p_423050_) {
      this.renderArmorPiece(p_422800_, p_425810_, p_428929_.chestEquipment, EquipmentSlot.CHEST, p_424719_, p_428929_);
      this.renderArmorPiece(p_422800_, p_425810_, p_428929_.legsEquipment, EquipmentSlot.LEGS, p_424719_, p_428929_);
      this.renderArmorPiece(p_422800_, p_425810_, p_428929_.feetEquipment, EquipmentSlot.FEET, p_424719_, p_428929_);
      this.renderArmorPiece(p_422800_, p_425810_, p_428929_.headEquipment, EquipmentSlot.HEAD, p_424719_, p_428929_);
   }

   private void renderArmorPiece(PoseStack p_117119_, SubmitNodeCollector p_425749_, ItemStack p_366444_, EquipmentSlot p_117122_, int p_117123_, S p_423438_) {
      Equippable equippable = p_366444_.get(DataComponents.EQUIPPABLE);
      if (equippable != null && shouldRender(equippable, p_117122_)) {
         A a = this.getArmorModel(p_423438_, p_117122_);
         EquipmentClientInfo.LayerType equipmentclientinfo$layertype = this.usesInnerModel(p_117122_)
            ? EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS
            : EquipmentClientInfo.LayerType.HUMANOID;
         this.equipmentRenderer
            .renderLayers(
               equipmentclientinfo$layertype,
               equippable.assetId().orElseThrow(),
               a,
               p_423438_,
               p_366444_,
               p_117119_,
               p_425749_,
               p_117123_,
               p_423438_.outlineColor
            );
      }
   }

   private A getArmorModel(S p_363587_, EquipmentSlot p_117079_) {
      return (p_363587_.isBaby ? this.babyModelSet : this.modelSet).get(p_117079_);
   }

   private boolean usesInnerModel(EquipmentSlot p_117129_) {
      return p_117129_ == EquipmentSlot.LEGS;
   }
}
