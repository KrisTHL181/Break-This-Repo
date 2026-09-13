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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public record TeleportRandomlyConsumeEffect(float diameter) implements ConsumeEffect {
   private static final float DEFAULT_DIAMETER = 16.0F;
   public static final MapCodec<TeleportRandomlyConsumeEffect> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("diameter", 16.0F).forGetter(TeleportRandomlyConsumeEffect::diameter))
         .apply(i, TeleportRandomlyConsumeEffect::new)
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
   public boolean apply(final Level level, final ItemStack stack, final LivingEntity user) {
      boolean teleported = false;

      for (int attempt = 0; attempt < 16; attempt++) {
         double xx = user.getX() + (user.getRandom().nextDouble() - 0.5) * this.diameter;
         double yy = Mth.clamp(
            user.getY() + (user.getRandom().nextDouble() - 0.5) * this.diameter, level.getMinY(), level.getMinY() + ((ServerLevel)level).getLogicalHeight() - 1
         );
         double zz = user.getZ() + (user.getRandom().nextDouble() - 0.5) * this.diameter;
         if (user.isPassenger()) {
            user.stopRiding();
         }

         Vec3 oldPos = user.position();
         if (user.randomTeleport(xx, yy, zz, true)) {
            level.gameEvent(GameEvent.TELEPORT, oldPos, GameEvent.Context.of(user));
            SoundSource soundSource;
            SoundEvent soundEvent;
            if (user instanceof Fox) {
               soundEvent = SoundEvents.FOX_TELEPORT;
               soundSource = SoundSource.NEUTRAL;
            } else {
               soundEvent = SoundEvents.CHORUS_FRUIT_TELEPORT;
               soundSource = SoundSource.PLAYERS;
            }

            level.playSound(null, user.getX(), user.getY(), user.getZ(), soundEvent, soundSource);
            user.resetFallDistance();
            teleported = true;
            break;
         }
      }

      if (teleported) {
         user.resetCurrentImpulseContext();
      }

      return teleported;
   }
}
