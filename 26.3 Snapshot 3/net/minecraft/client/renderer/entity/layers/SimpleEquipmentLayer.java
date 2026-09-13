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
import org.jspecify.annotations.Nullable;

public class SimpleEquipmentLayer<S extends LivingEntityRenderState, RM extends EntityModel<? super S>, EM extends EntityModel<? super S>>
   extends RenderLayer<S, RM> {
   private final EquipmentLayerRenderer equipmentRenderer;
   private final EquipmentClientInfo.LayerType layer;
   private final Function<S, ItemStack> itemGetter;
   private final EM adultModel;
   private final @Nullable EM babyModel;
   private final int order;

   public SimpleEquipmentLayer(
      final RenderLayerParent<S, RM> renderer,
      final EquipmentLayerRenderer equipmentRenderer,
      final EquipmentClientInfo.LayerType layer,
      final Function<S, ItemStack> itemGetter,
      final EM adultModel,
      final @Nullable EM babyModel,
      final int order
   ) {
      super(renderer);
      this.equipmentRenderer = equipmentRenderer;
      this.layer = layer;
      this.itemGetter = itemGetter;
      this.adultModel = adultModel;
      this.babyModel = babyModel;
      this.order = order;
   }

   public SimpleEquipmentLayer(
      final RenderLayerParent<S, RM> renderer,
      final EquipmentLayerRenderer equipmentRenderer,
      final EquipmentClientInfo.LayerType layer,
      final Function<S, ItemStack> itemGetter,
      final EM adultModel,
      final @Nullable EM babyModel
   ) {
      this(renderer, equipmentRenderer, layer, itemGetter, adultModel, babyModel, 0);
   }

   public void submit(
      final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int lightCoords, final S state, final float yRot, final float xRot
   ) {
      ItemStack equipment = this.itemGetter.apply(state);
      Equippable equippable = equipment.get(DataComponents.EQUIPPABLE);
      if (equippable != null && !equippable.assetId().isEmpty() && (!state.isBaby || this.babyModel != null)) {
         EM model = state.isBaby ? this.babyModel : this.adultModel;
         this.equipmentRenderer
            .renderLayers(
               this.layer,
               equippable.assetId().get(),
               model,
               state,
               equipment,
               poseStack,
               submitNodeCollector,
               lightCoords,
               null,
               state.outlineColor,
               this.order
            );
      }
   }
}
