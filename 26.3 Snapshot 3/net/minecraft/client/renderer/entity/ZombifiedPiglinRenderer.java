package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.monster.piglin.AdultZombifiedPiglinModel;
import net.minecraft.client.model.monster.piglin.BabyZombifiedPiglinModel;
import net.minecraft.client.model.monster.piglin.ZombifiedPiglinModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.ZombifiedPiglinRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;

public class ZombifiedPiglinRenderer extends HumanoidMobRenderer<ZombifiedPiglin, ZombifiedPiglinRenderState, ZombifiedPiglinModel> {
   private static final Identifier ZOMBIFIED_PIGLIN_LOCATION = Identifier.withDefaultNamespace("textures/entity/piglin/zombified_piglin.png");
   private static final Identifier BABY_ZOMBIFIED_PIGLIN_LOCATION = Identifier.withDefaultNamespace("textures/entity/piglin/zombified_piglin_baby.png");

   public ZombifiedPiglinRenderer(
      final EntityRendererProvider.Context context,
      final ModelLayerLocation body,
      final ModelLayerLocation babyBody,
      final ArmorModelSet<ModelLayerLocation> armorSet,
      final ArmorModelSet<ModelLayerLocation> babyArmorSet
   ) {
      super(
         context,
         new AdultZombifiedPiglinModel(context.bakeLayer(body)),
         new BabyZombifiedPiglinModel(context.bakeLayer(babyBody)),
         0.5F,
         PiglinRenderer.PIGLIN_CUSTOM_HEAD_TRANSFORMS
      );
      this.addLayer(
         new HumanoidArmorLayer<>(
            this,
            ArmorModelSet.bake(armorSet, context.getModelSet(), AdultZombifiedPiglinModel::new),
            ArmorModelSet.bake(babyArmorSet, context.getModelSet(), BabyZombifiedPiglinModel::new),
            context.getEquipmentRenderer()
         )
      );
   }

   public Identifier getTextureLocation(final ZombifiedPiglinRenderState state) {
      return state.isBaby ? BABY_ZOMBIFIED_PIGLIN_LOCATION : ZOMBIFIED_PIGLIN_LOCATION;
   }

   public ZombifiedPiglinRenderState createRenderState() {
      return new ZombifiedPiglinRenderState();
   }

   public void extractRenderState(final ZombifiedPiglin entity, final ZombifiedPiglinRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.isAggressive = entity.isAggressive();
   }
}
