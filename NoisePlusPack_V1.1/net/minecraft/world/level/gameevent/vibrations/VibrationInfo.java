package net.minecraft.world.level.gameevent.vibrations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record VibrationInfo(
   Holder<GameEvent> gameEvent, float distance, Vec3 pos, @Nullable UUID uuid, @Nullable UUID projectileOwnerUuid, @Nullable Entity entity
) {
   public static final Codec<VibrationInfo> CODEC = RecordCodecBuilder.create(
      p_341894_ -> p_341894_.group(
            GameEvent.CODEC.fieldOf("game_event").forGetter(VibrationInfo::gameEvent),
            Codec.floatRange(0.0F, Float.MAX_VALUE).fieldOf("distance").forGetter(VibrationInfo::distance),
            Vec3.CODEC.fieldOf("pos").forGetter(VibrationInfo::pos),
            UUIDUtil.CODEC.lenientOptionalFieldOf("source").forGetter(p_250608_ -> Optional.ofNullable(p_250608_.uuid())),
            UUIDUtil.CODEC.lenientOptionalFieldOf("projectile_owner").forGetter(p_250607_ -> Optional.ofNullable(p_250607_.projectileOwnerUuid()))
         )
         .apply(
            p_341894_,
            (p_327438_, p_327439_, p_327440_, p_327441_, p_327442_) -> new VibrationInfo(
               p_327438_, p_327439_, p_327440_, (UUID)p_327441_.orElse(null), (UUID)p_327442_.orElse(null)
            )
         )
   );

   public VibrationInfo(Holder<GameEvent> p_332399_, float p_250190_, Vec3 p_251692_, @Nullable UUID p_249849_, @Nullable UUID p_249731_) {
      this(p_332399_, p_250190_, p_251692_, p_249849_, p_249731_, null);
   }

   public VibrationInfo(Holder<GameEvent> p_333558_, float p_251086_, Vec3 p_250935_, @Nullable Entity p_249432_) {
      this(p_333558_, p_251086_, p_250935_, p_249432_ == null ? null : p_249432_.getUUID(), getProjectileOwner(p_249432_), p_249432_);
   }

   private static @Nullable UUID getProjectileOwner(@Nullable Entity p_251531_) {
      return p_251531_ instanceof Projectile projectile && projectile.getOwner() != null ? projectile.getOwner().getUUID() : null;
   }

   public Optional<Entity> getEntity(ServerLevel p_249184_) {
      return Optional.ofNullable(this.entity).or(() -> Optional.ofNullable(this.uuid).map(p_249184_::getEntity));
   }

   public Optional<Entity> getProjectileOwner(ServerLevel p_249217_) {
      return this.getEntity(p_249217_)
         .filter(p_249829_ -> p_249829_ instanceof Projectile)
         .map(p_249388_ -> (Projectile)p_249388_)
         .map(Projectile::getOwner)
         .or(() -> Optional.ofNullable(this.projectileOwnerUuid).map(p_249217_::getEntity));
   }
}
