package net.minecraft.world.level.gameevent;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class EntityPositionSource implements PositionSource {
   public static final MapCodec<EntityPositionSource> CODEC = RecordCodecBuilder.mapCodec(
      p_253607_ -> p_253607_.group(
            UUIDUtil.CODEC.fieldOf("source_entity").forGetter(EntityPositionSource::getUuid),
            Codec.FLOAT.fieldOf("y_offset").orElse(0.0F).forGetter(p_223666_ -> p_223666_.yOffset)
         )
         .apply(p_253607_, (p_223672_, p_223673_) -> new EntityPositionSource(Either.right(Either.left(p_223672_)), p_223673_))
   );
   public static final StreamCodec<ByteBuf, EntityPositionSource> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      EntityPositionSource::getId,
      ByteBufCodecs.FLOAT,
      p_327428_ -> p_327428_.yOffset,
      (p_327429_, p_327430_) -> new EntityPositionSource(Either.right(Either.right(p_327429_)), p_327430_)
   );
   private Either<Entity, Either<UUID, Integer>> entityOrUuidOrId;
   private final float yOffset;

   public EntityPositionSource(Entity p_223648_, float p_223649_) {
      this(Either.left(p_223648_), p_223649_);
   }

   private EntityPositionSource(Either<Entity, Either<UUID, Integer>> p_223651_, float p_223652_) {
      this.entityOrUuidOrId = p_223651_;
      this.yOffset = p_223652_;
   }

   @Override
   public Optional<Vec3> getPosition(Level p_157733_) {
      if (this.entityOrUuidOrId.left().isEmpty()) {
         this.resolveEntity(p_157733_);
      }

      return this.entityOrUuidOrId.left().map(p_223676_ -> p_223676_.position().add(0.0, this.yOffset, 0.0));
   }

   private void resolveEntity(Level p_223678_) {
      ((Optional)this.entityOrUuidOrId
            .map(
               Optional::of,
               p_223657_ -> Optional.ofNullable(
                  (Entity)p_223657_.map(
                     p_422211_ -> p_223678_ instanceof ServerLevel serverlevel ? serverlevel.getEntity(p_422211_) : null, p_223678_::getEntity
                  )
               )
            ))
         .ifPresent(p_223654_ -> this.entityOrUuidOrId = Either.left(p_223654_));
   }

   public UUID getUuid() {
      return (UUID)this.entityOrUuidOrId.map(Entity::getUUID, p_223680_ -> (UUID)p_223680_.map(Function.identity(), p_223668_ -> {
         throw new RuntimeException("Unable to get entityId from uuid");
      }));
   }

   private int getId() {
      return (Integer)this.entityOrUuidOrId.map(Entity::getId, p_223662_ -> (Integer)p_223662_.map(p_223670_ -> {
         throw new IllegalStateException("Unable to get entityId from uuid");
      }, Function.identity()));
   }

   @Override
   public PositionSourceType<EntityPositionSource> getType() {
      return PositionSourceType.ENTITY;
   }

   public static class Type implements PositionSourceType<EntityPositionSource> {
      @Override
      public MapCodec<EntityPositionSource> codec() {
         return EntityPositionSource.CODEC;
      }

      @Override
      public StreamCodec<ByteBuf, EntityPositionSource> streamCodec() {
         return EntityPositionSource.STREAM_CODEC;
      }
   }
}
