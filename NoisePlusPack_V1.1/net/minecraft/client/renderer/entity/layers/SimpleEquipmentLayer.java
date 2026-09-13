package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SimpleEquipmentLayer<S extends LivingEntityRenderState, RM extends EntityModel<? super S>, EM extends EntityModel<? super S>>
   extends RenderLayer<S, RM> {
   private final EquipmentLayerRenderer equipmentRenderer;
   private final EquipmentClientInfo.LayerType layer;
   private final Function<S, ItemStack> itemGetter;
   private final EM adultModel;
   private final @Nullable EM babyModel;
   private final int order;

   public SimpleEquipmentLayer(
      RenderLayerParent<S, RM> p_397316_,
      EquipmentLayerRenderer p_393279_,
      EquipmentClientInfo.LayerType p_393287_,
      Function<S, ItemStack> p_395803_,
      EM p_397758_,
      @Nullable EM p_422311_,
      int p_427336_
   ) {
      super(p_397316_);
      this.equipmentRenderer = p_393279_;
      this.layer = p_393287_;
      this.itemGetter = p_395803_;
      this.adultModel = p_397758_;
      this.babyModel = p_422311_;
      this.order = p_427336_;
   }

   public SimpleEquipmentLayer(
      RenderLayerParent<S, RM> p_391337_,
      EquipmentLayerRenderer p_396277_,
      EquipmentClientInfo.LayerType p_393091_,
      Function<S, ItemStack> p_394811_,
      EM p_392402_,
      @Nullable EM p_397424_
   ) {
      this(p_391337_, p_396277_, p_393091_, p_394811_, p_392402_, p_397424_, 0);
   }

   public void submit(PoseStack p_429227_, SubmitNodeCollector p_430830_, int p_428153_, S p_429907_, float p_428265_, float p_430172_) {
      ItemStack itemstack = this.itemGetter.apply(p_429907_);
      Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
      if (equippable != null && !equippable.assetId().isEmpty() && (!p_429907_.isBaby || this.babyModel != null)) {
         EM em = p_429907_.isBaby ? this.babyModel : this.adultModel;
         this.equipmentRenderer
            .renderLayers(
               this.layer, equippable.assetId().get(), em, p_429907_, itemstack, p_429227_, p_430830_, p_428153_, null, p_429907_.outlineColor, this.order
            );
      }
   }
}
