package net.minecraft.network.protocol.game;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;

public record ServerboundTestInstanceBlockActionPacket(BlockPos pos, ServerboundTestInstanceBlockActionPacket.Action action, TestInstanceBlockEntity.Data data)
   implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundTestInstanceBlockActionPacket> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      ServerboundTestInstanceBlockActionPacket::pos,
      ServerboundTestInstanceBlockActionPacket.Action.STREAM_CODEC,
      ServerboundTestInstanceBlockActionPacket::action,
      TestInstanceBlockEntity.Data.STREAM_CODEC,
      ServerboundTestInstanceBlockActionPacket::data,
      ServerboundTestInstanceBlockActionPacket::new
   );

   public ServerboundTestInstanceBlockActionPacket(
      BlockPos p_391315_,
      ServerboundTestInstanceBlockActionPacket.Action p_392848_,
      Optional<ResourceKey<GameTestInstance>> p_393624_,
      Vec3i p_391405_,
      Rotation p_395308_,
      boolean p_396033_
   ) {
      this(
         p_391315_,
         p_392848_,
         new TestInstanceBlockEntity.Data(p_393624_, p_391405_, p_395308_, p_396033_, TestInstanceBlockEntity.Status.CLEARED, Optional.empty())
      );
   }

   @Override
   public PacketType<ServerboundTestInstanceBlockActionPacket> type() {
      return GamePacketTypes.SERVERBOUND_TEST_INSTANCE_BLOCK_ACTION;
   }

   public void handle(ServerGamePacketListener p_397453_) {
      p_397453_.handleTestInstanceBlockAction(this);
   }

   public enum Action {
      INIT(0),
      QUERY(1),
      SET(2),
      RESET(3),
      SAVE(4),
      EXPORT(5),
      RUN(6);

      private static final IntFunction<ServerboundTestInstanceBlockActionPacket.Action> BY_ID = ByIdMap.continuous(
         p_395495_ -> p_395495_.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO
      );
      public static final StreamCodec<ByteBuf, ServerboundTestInstanceBlockActionPacket.Action> STREAM_CODEC = ByteBufCodecs.idMapper(
         BY_ID, p_391553_ -> p_391553_.id
      );
      private final int id;

      Action(final int p_397304_) {
         this.id = p_397304_;
      }
   }
}
