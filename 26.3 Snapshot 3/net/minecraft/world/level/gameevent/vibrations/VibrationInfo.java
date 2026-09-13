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
      i -> i.group(
            GameEvent.CODEC.fieldOf("game_event").forGetter(VibrationInfo::gameEvent),
            Codec.floatRange(0.0F, Float.MAX_VALUE).fieldOf("distance").forGetter(VibrationInfo::distance),
            Vec3.CODEC.fieldOf("pos").forGetter(VibrationInfo::pos),
            UUIDUtil.CODEC.lenientOptionalFieldOf("source").forGetter(o -> Optional.ofNullable(o.uuid())),
            UUIDUtil.CODEC.lenientOptionalFieldOf("projectile_owner").forGetter(o -> Optional.ofNullable(o.projectileOwnerUuid()))
         )
         .apply(
            i,
            (event, distance, pos, source, projectileOwner) -> new VibrationInfo(
               event, distance, pos, (UUID)source.orElse(null), (UUID)projectileOwner.orElse(null)
            )
         )
   );

   public VibrationInfo(
      final Holder<GameEvent> gameEvent, final float distance, final Vec3 pos, final @Nullable UUID uuid, final @Nullable UUID projectileOwnerUuid
   ) {
      this(gameEvent, distance, pos, uuid, projectileOwnerUuid, null);
   }

   public VibrationInfo(final Holder<GameEvent> gameEvent, final float distance, final Vec3 pos, final @Nullable Entity entity) {
      this(gameEvent, distance, pos, entity == null ? null : entity.getUUID(), getProjectileOwner(entity), entity);
   }

   private static @Nullable UUID getProjectileOwner(final @Nullable Entity entity) {
      return entity instanceof Projectile projectile && projectile.getOwner() != null ? projectile.getOwner().getUUID() : null;
   }

   public Optional<Entity> getEntity(final ServerLevel level) {
      return Optional.ofNullable(this.entity).or(() -> Optional.ofNullable(this.uuid).map(level::getEntity));
   }

   public Optional<Entity> getProjectileOwner(final ServerLevel level) {
      return this.getEntity(level)
         .filter(e -> e instanceof Projectile)
         .map(e -> (Projectile)e)
         .map(Projectile::getOwner)
         .or(() -> Optional.ofNullable(this.projectileOwnerUuid).map(level::getEntity));
   }
}
