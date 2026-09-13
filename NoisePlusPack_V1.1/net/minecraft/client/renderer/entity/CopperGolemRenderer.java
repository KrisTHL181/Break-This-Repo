package net.minecraft.client.renderer.entity;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.model.animal.golem.CopperGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.BlockDecorationLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.CopperGolemRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemOxidationLevels;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CopperGolemRenderer extends MobRenderer<CopperGolem, CopperGolemRenderState, CopperGolemModel> {
   public CopperGolemRenderer(EntityRendererProvider.Context p_428998_) {
      super(p_428998_, new CopperGolemModel(p_428998_.bakeLayer(ModelLayers.COPPER_GOLEM)), 0.5F);
      this.addLayer(
         new LivingEntityEmissiveLayer<>(
            this,
            getEyeTextureLocationProvider(),
            (p_424565_, p_422617_) -> 1.0F,
            new CopperGolemModel(p_428998_.bakeLayer(ModelLayers.COPPER_GOLEM)),
            RenderTypes::eyes,
            false
         )
      );
      this.addLayer(new ItemInHandLayer<>(this));
      this.addLayer(new BlockDecorationLayer<>(this, p_426295_ -> p_426295_.blockOnAntenna, this.model::applyBlockOnAntennaTransform));
      this.addLayer(new CustomHeadLayer<>(this, p_428998_.getModelSet(), p_428998_.getPlayerSkinRenderCache()));
   }

   public Identifier getTextureLocation(CopperGolemRenderState p_458422_) {
      return CopperGolemOxidationLevels.getOxidationLevel(p_458422_.weathering).texture();
   }

   private static Function<CopperGolemRenderState, Identifier> getEyeTextureLocationProvider() {
      return p_448314_ -> CopperGolemOxidationLevels.getOxidationLevel(p_448314_.weathering).eyeTexture();
   }

   public CopperGolemRenderState createRenderState() {
      return new CopperGolemRenderState();
   }

   public void extractRenderState(CopperGolem p_452025_, CopperGolemRenderState p_431593_, float p_427347_) {
      super.extractRenderState(p_452025_, p_431593_, p_427347_);
      ArmedEntityRenderState.extractArmedEntityRenderState(p_452025_, p_431593_, this.itemModelResolver, p_427347_);
      p_431593_.weathering = p_452025_.getWeatherState();
      p_431593_.copperGolemState = p_452025_.getState();
      p_431593_.idleAnimationState.copyFrom(p_452025_.getIdleAnimationState());
      p_431593_.interactionGetItem.copyFrom(p_452025_.getInteractionGetItemAnimationState());
      p_431593_.interactionGetNoItem.copyFrom(p_452025_.getInteractionGetNoItemAnimationState());
      p_431593_.interactionDropItem.copyFrom(p_452025_.getInteractionDropItemAnimationState());
      p_431593_.interactionDropNoItem.copyFrom(p_452025_.getInteractionDropNoItemAnimationState());
      p_431593_.blockOnAntenna = Optional.of(p_452025_.getItemBySlot(CopperGolem.EQUIPMENT_SLOT_ANTENNA)).flatMap(p_428707_ -> {
         if (p_428707_.getItem() instanceof BlockItem blockitem) {
            BlockItemStateProperties blockitemstateproperties = p_428707_.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
            return Optional.of(blockitemstateproperties.apply(blockitem.getBlock().defaultBlockState()));
         } else {
            return Optional.empty();
         }
      });
   }
}
