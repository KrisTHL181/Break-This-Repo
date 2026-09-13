package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.MinecartCommandBlock;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class ServerboundSetCommandMinecartPacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundSetCommandMinecartPacket> STREAM_CODEC = Packet.codec(
      ServerboundSetCommandMinecartPacket::write, ServerboundSetCommandMinecartPacket::new
   );
   private final int entity;
   private final String command;
   private final boolean trackOutput;

   public ServerboundSetCommandMinecartPacket(int p_134534_, String p_134535_, boolean p_134536_) {
      this.entity = p_134534_;
      this.command = p_134535_;
      this.trackOutput = p_134536_;
   }

   private ServerboundSetCommandMinecartPacket(FriendlyByteBuf p_179758_) {
      this.entity = p_179758_.readVarInt();
      this.command = p_179758_.readUtf();
      this.trackOutput = p_179758_.readBoolean();
   }

   private void write(FriendlyByteBuf p_134547_) {
      p_134547_.writeVarInt(this.entity);
      p_134547_.writeUtf(this.command);
      p_134547_.writeBoolean(this.trackOutput);
   }

   @Override
   public PacketType<ServerboundSetCommandMinecartPacket> type() {
      return GamePacketTypes.SERVERBOUND_SET_COMMAND_MINECART;
   }

   public void handle(ServerGamePacketListener p_134544_) {
      p_134544_.handleSetCommandMinecart(this);
   }

   public @Nullable BaseCommandBlock getCommandBlock(Level p_134538_) {
      Entity entity = p_134538_.getEntity(this.entity);
      return entity instanceof MinecartCommandBlock ? ((MinecartCommandBlock)entity).getCommandBlock() : null;
   }

   public String getCommand() {
      return this.command;
   }

   public boolean isTrackOutput() {
      return this.trackOutput;
   }
}
