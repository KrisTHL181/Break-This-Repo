package net.minecraft.world.item.consume_effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public record TeleportRandomlyConsumeEffect(float diameter) implements ConsumeEffect {
   private static final float DEFAULT_DIAMETER = 16.0F;
   public static final MapCodec<TeleportRandomlyConsumeEffect> CODEC = RecordCodecBuilder.mapCodec(
      p_363911_ -> p_363911_.group(ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("diameter", 16.0F).forGetter(TeleportRandomlyConsumeEffect::diameter))
         .apply(p_363911_, TeleportRandomlyConsumeEffect::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, TeleportRandomlyConsumeEffect> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.FLOAT, TeleportRandomlyConsumeEffect::diameter, TeleportRandomlyConsumeEffect::new
   );

   public TeleportRandomlyConsumeEffect() {
      this(16.0F);
   }

   @Override
   public ConsumeEffect.Type<TeleportRandomlyConsumeEffect> getType() {
      return ConsumeEffect.Type.TELEPORT_RANDOMLY;
   }

   @Override
   public boolean apply(Level p_369919_, ItemStack p_362169_, LivingEntity p_364416_) {
      boolean flag = false;

      for (int i = 0; i < 16; i++) {
         double d0 = p_364416_.getX() + (p_364416_.getRandom().nextDouble() - 0.5) * this.diameter;
         double d1 = Mth.clamp(
            p_364416_.getY() + (p_364416_.getRandom().nextDouble() - 0.5) * this.diameter,
            p_369919_.getMinY(),
            p_369919_.getMinY() + ((ServerLevel)p_369919_).getLogicalHeight() - 1
         );
         double d2 = p_364416_.getZ() + (p_364416_.getRandom().nextDouble() - 0.5) * this.diameter;
         if (p_364416_.isPassenger()) {
            p_364416_.stopRiding();
         }

         Vec3 vec3 = p_364416_.position();
         if (p_364416_.randomTeleport(d0, d1, d2, true)) {
            p_369919_.gameEvent(GameEvent.TELEPORT, vec3, GameEvent.Context.of(p_364416_));
            SoundSource soundsource;
            SoundEvent soundevent;
            if (p_364416_ instanceof Fox) {
               soundevent = SoundEvents.FOX_TELEPORT;
               soundsource = SoundSource.NEUTRAL;
            } else {
               soundevent = SoundEvents.CHORUS_FRUIT_TELEPORT;
               soundsource = SoundSource.PLAYERS;
            }

            p_369919_.playSound(null, p_364416_.getX(), p_364416_.getY(), p_364416_.getZ(), soundevent, soundsource);
            p_364416_.resetFallDistance();
            flag = true;
            break;
         }
      }

      if (flag && p_364416_ instanceof Player player) {
         player.resetCurrentImpulseContext();
      }

      return flag;
   }
}
