package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.model.animal.nautilus.NautilusArmorModel;
import net.minecraft.client.model.animal.nautilus.NautilusModel;
import net.minecraft.client.model.animal.nautilus.NautilusSaddleModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.nautilus.ZombieNautilusCoralModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.NautilusRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilus;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilusVariant;

public class ZombieNautilusRenderer extends MobRenderer<ZombieNautilus, NautilusRenderState, NautilusModel> {
   private final Map<ZombieNautilusVariant.ModelType, NautilusModel> models;

   public ZombieNautilusRenderer(final EntityRendererProvider.Context context) {
      super(context, new NautilusModel(context.bakeLayer(ModelLayers.ZOMBIE_NAUTILUS)), 0.7F);
      this.addLayer(
         new SimpleEquipmentLayer<>(
            this,
            context.getEquipmentRenderer(),
            EquipmentClientInfo.LayerType.NAUTILUS_BODY,
            state -> state.bodyArmorItem,
            new NautilusArmorModel(context.bakeLayer(ModelLayers.NAUTILUS_ARMOR)),
            null
         )
      );
      this.addLayer(
         new SimpleEquipmentLayer<>(
            this,
            context.getEquipmentRenderer(),
            EquipmentClientInfo.LayerType.NAUTILUS_SADDLE,
            state -> state.saddle,
            new NautilusSaddleModel(context.bakeLayer(ModelLayers.NAUTILUS_SADDLE)),
            null
         )
      );
      this.models = bakeModels(context);
   }

   private static Map<ZombieNautilusVariant.ModelType, NautilusModel> bakeModels(final EntityRendererProvider.Context context) {
      return Maps.newEnumMap(
         Map.of(
            ZombieNautilusVariant.ModelType.NORMAL,
            new NautilusModel(context.bakeLayer(ModelLayers.ZOMBIE_NAUTILUS)),
            ZombieNautilusVariant.ModelType.WARM,
            new ZombieNautilusCoralModel(context.bakeLayer(ModelLayers.ZOMBIE_NAUTILUS_CORAL))
         )
      );
   }

   public void submit(final NautilusRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
      if (state.variant != null) {
         this.model = this.models.get(state.variant.modelAndTexture().model());
         super.submit(state, poseStack, submitNodeCollector, camera);
      }
   }

   public Identifier getTextureLocation(final NautilusRenderState state) {
      return state.variant == null ? MissingTextureAtlasSprite.getLocation() : state.variant.modelAndTexture().asset().texturePath();
   }

   public NautilusRenderState createRenderState() {
      return new NautilusRenderState();
   }

   public void extractRenderState(final ZombieNautilus entity, final NautilusRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.saddle = entity.getItemBySlot(EquipmentSlot.SADDLE).copy();
      state.bodyArmorItem = entity.getBodyArmorItem().copy();
      state.variant = entity.getVariant().value();
   }
}
