package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.client.model.animal.rabbit.AdultRabbitModel;
import net.minecraft.client.model.animal.rabbit.BabyRabbitModel;
import net.minecraft.client.model.animal.rabbit.RabbitModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.RabbitRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.rabbit.Rabbit;

public class RabbitRenderer extends AgeableMobRenderer<Rabbit, RabbitRenderState, RabbitModel> {
   private static final Identifier TOAST = Identifier.withDefaultNamespace("textures/entity/rabbit/rabbit_toast.png");
   private static final Identifier TOAST_BABY = Identifier.withDefaultNamespace("textures/entity/rabbit/rabbit_toast_baby.png");
   private static final Map<Rabbit.Variant, Identifier> RABBIT_LOCATIONS = Maps.newEnumMap(
      Map.of(
         Rabbit.Variant.BROWN,
         Identifier.withDefaultNamespace("textures/entity/rabbit/rabbit_brown.png"),
         Rabbit.Variant.WHITE_SPLOTCHED,
         Identifier.withDefaultNamespace("textures/entity/rabbit/rabbit_white_splotched.png"),
         Rabbit.Variant.EVIL,
         Identifier.withDefaultNamespace("textures/entity/rabbit/rabbit_caerbannog.png"),
         Rabbit.Variant.WHITE,
         Identifier.withDefaultNamespace("textures/entity/rabbit/rabbit_white.png"),
         Rabbit.Variant.GOLD,
         Identifier.withDefaultNamespace("textures/entity/rabbit/rabbit_gold.png"),
         Rabbit.Variant.BLACK,
         Identifier.withDefaultNamespace("textures/entity/rabbit/rabbit_black.png"),
         Rabbit.Variant.SALT,
         Identifier.withDefaultNamespace("textures/entity/rabbit/rabbit_salt.png")
      )
   );
   private static final Map<Rabbit.Variant, Identifier> BABY_RABBIT_LOCATIONS = Maps.newEnumMap(
      Map.of(
         Rabbit.Variant.BROWN,
         Identifier.withDefaultNamespace("textures/entity/rabbit/rabbit_brown_baby.png"),
         Rabbit.Variant.WHITE_SPLOTCHED,
         Identifier.withDefaultNamespace("textures/entity/rabbit/rabbit_white_splotched_baby.png"),
         Rabbit.Variant.EVIL,
         Identifier.withDefaultNamespace("textures/entity/rabbit/rabbit_caerbannog_baby.png"),
         Rabbit.Variant.WHITE,
         Identifier.withDefaultNamespace("textures/entity/rabbit/rabbit_white_baby.png"),
         Rabbit.Variant.GOLD,
         Identifier.withDefaultNamespace("textures/entity/rabbit/rabbit_gold_baby.png"),
         Rabbit.Variant.BLACK,
         Identifier.withDefaultNamespace("textures/entity/rabbit/rabbit_black_baby.png"),
         Rabbit.Variant.SALT,
         Identifier.withDefaultNamespace("textures/entity/rabbit/rabbit_salt_baby.png")
      )
   );

   public RabbitRenderer(final EntityRendererProvider.Context context) {
      super(context, new AdultRabbitModel(context.bakeLayer(ModelLayers.RABBIT)), new BabyRabbitModel(context.bakeLayer(ModelLayers.RABBIT_BABY)), 0.3F);
   }

   public Identifier getTextureLocation(final RabbitRenderState state) {
      if (state.isToast) {
         return state.isBaby ? TOAST_BABY : TOAST;
      }

      Map<Rabbit.Variant, Identifier> locations = state.isBaby ? BABY_RABBIT_LOCATIONS : RABBIT_LOCATIONS;
      return locations.get(state.variant);
   }

   public RabbitRenderState createRenderState() {
      return new RabbitRenderState();
   }

   public void extractRenderState(final Rabbit entity, final RabbitRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.jumpCompletion = entity.getJumpCompletion(partialTicks);
      state.isToast = checkMagicName(entity, "Toast");
      state.variant = entity.getVariant();
      state.hopAnimationState.copyFrom(entity.hopAnimationState);
      state.idleHeadTiltAnimationState.copyFrom(entity.idleHeadTiltAnimationState);
   }
}
