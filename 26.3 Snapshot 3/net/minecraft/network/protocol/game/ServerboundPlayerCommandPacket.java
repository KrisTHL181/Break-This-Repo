package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;

public class ServerboundPlayerCommandPacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundPlayerCommandPacket> STREAM_CODEC = Packet.codec(
      ServerboundPlayerCommandPacket::write, ServerboundPlayerCommandPacket::new
   );
   private final int id;
   private final ServerboundPlayerCommandPacket.Action action;
   private final int data;

   public ServerboundPlayerCommandPacket(final Entity entity, final ServerboundPlayerCommandPacket.Action action) {
      this(entity, action, 0);
   }

   public ServerboundPlayerCommandPacket(final Entity entity, final ServerboundPlayerCommandPacket.Action action, final int data) {
      this.id = entity.getId();
      this.action = action;
      this.data = data;
   }

   private ServerboundPlayerCommandPacket(final FriendlyByteBuf input) {
      this.id = input.readVarInt();
      this.action = input.readEnum(ServerboundPlayerCommandPacket.Action.class);
      this.data = input.readVarInt();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeVarInt(this.id);
      output.writeEnum(this.action);
      output.writeVarInt(this.data);
   }

   @Override
   public PacketType<ServerboundPlayerCommandPacket> type() {
      return GamePacketTypes.SERVERBOUND_PLAYER_COMMAND;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handlePlayerCommand(this);
   }

   public int getId() {
      return this.id;
   }

   public ServerboundPlayerCommandPacket.Action getAction() {
      return this.action;
   }

   public int getData() {
      return this.data;
   }

   public enum Action {
      STOP_SLEEPING,
      START_SPRINTING,
      STOP_SPRINTING,
      START_RIDING_JUMP,
      STOP_RIDING_JUMP,
      OPEN_INVENTORY,
      START_FALL_FLYING;
   }
}
