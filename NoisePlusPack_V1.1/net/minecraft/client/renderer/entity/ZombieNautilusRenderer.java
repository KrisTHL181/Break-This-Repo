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
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilus;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilusVariant;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ZombieNautilusRenderer extends MobRenderer<ZombieNautilus, NautilusRenderState, NautilusModel> {
   private final Map<ZombieNautilusVariant.ModelType, NautilusModel> models;

   public ZombieNautilusRenderer(EntityRendererProvider.Context p_456698_) {
      super(p_456698_, new NautilusModel(p_456698_.bakeLayer(ModelLayers.ZOMBIE_NAUTILUS)), 0.7F);
      this.addLayer(
         new SimpleEquipmentLayer<>(
            this,
            p_456698_.getEquipmentRenderer(),
            EquipmentClientInfo.LayerType.NAUTILUS_BODY,
            p_458206_ -> p_458206_.bodyArmorItem,
            new NautilusArmorModel(p_456698_.bakeLayer(ModelLayers.NAUTILUS_ARMOR)),
            null
         )
      );
      this.addLayer(
         new SimpleEquipmentLayer<>(
            this,
            p_456698_.getEquipmentRenderer(),
            EquipmentClientInfo.LayerType.NAUTILUS_SADDLE,
            p_452019_ -> p_452019_.saddle,
            new NautilusSaddleModel(p_456698_.bakeLayer(ModelLayers.NAUTILUS_SADDLE)),
            null
         )
      );
      this.models = bakeModels(p_456698_);
   }

   private static Map<ZombieNautilusVariant.ModelType, NautilusModel> bakeModels(EntityRendererProvider.Context p_456928_) {
      return Maps.newEnumMap(
         Map.of(
            ZombieNautilusVariant.ModelType.NORMAL,
            new NautilusModel(p_456928_.bakeLayer(ModelLayers.ZOMBIE_NAUTILUS)),
            ZombieNautilusVariant.ModelType.WARM,
            new ZombieNautilusCoralModel(p_456928_.bakeLayer(ModelLayers.ZOMBIE_NAUTILUS_CORAL))
         )
      );
   }

   public void submit(NautilusRenderState p_459622_, PoseStack p_451863_, SubmitNodeCollector p_455984_, CameraRenderState p_459422_) {
      if (p_459622_.variant != null) {
         this.model = this.models.get(p_459622_.variant.modelAndTexture().model());
         super.submit(p_459622_, p_451863_, p_455984_, p_459422_);
      }
   }

   public Identifier getTextureLocation(NautilusRenderState p_456811_) {
      return p_456811_.variant == null ? MissingTextureAtlasSprite.getLocation() : p_456811_.variant.modelAndTexture().asset().texturePath();
   }

   public NautilusRenderState createRenderState() {
      return new NautilusRenderState();
   }

   public void extractRenderState(ZombieNautilus p_455985_, NautilusRenderState p_452269_, float p_459534_) {
      super.extractRenderState(p_455985_, p_452269_, p_459534_);
      p_452269_.saddle = p_455985_.getItemBySlot(EquipmentSlot.SADDLE).copy();
      p_452269_.bodyArmorItem = p_455985_.getBodyArmorItem().copy();
      p_452269_.variant = p_455985_.getVariant().value();
   }
}
