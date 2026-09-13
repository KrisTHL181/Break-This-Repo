package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.border.WorldBorder;

public class ClientboundInitializeBorderPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundInitializeBorderPacket> STREAM_CODEC = Packet.codec(
      ClientboundInitializeBorderPacket::write, ClientboundInitializeBorderPacket::new
   );
   private final double newCenterX;
   private final double newCenterZ;
   private final double oldSize;
   private final double newSize;
   private final long lerpTime;
   private final int newAbsoluteMaxSize;
   private final int warningBlocks;
   private final int warningTime;

   private ClientboundInitializeBorderPacket(final FriendlyByteBuf input) {
      this.newCenterX = input.readDouble();
      this.newCenterZ = input.readDouble();
      this.oldSize = input.readDouble();
      this.newSize = input.readDouble();
      this.lerpTime = input.readVarLong();
      this.newAbsoluteMaxSize = input.readVarInt();
      this.warningBlocks = input.readVarInt();
      this.warningTime = input.readVarInt();
   }

   public ClientboundInitializeBorderPacket(final WorldBorder border) {
      this.newCenterX = border.getCenterX();
      this.newCenterZ = border.getCenterZ();
      this.oldSize = border.getSize();
      this.newSize = border.getLerpTarget();
      this.lerpTime = border.getLerpTime();
      this.newAbsoluteMaxSize = border.getAbsoluteMaxSize();
      this.warningBlocks = border.getWarningBlocks();
      this.warningTime = border.getWarningTime();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeDouble(this.newCenterX);
      output.writeDouble(this.newCenterZ);
      output.writeDouble(this.oldSize);
      output.writeDouble(this.newSize);
      output.writeVarLong(this.lerpTime);
      output.writeVarInt(this.newAbsoluteMaxSize);
      output.writeVarInt(this.warningBlocks);
      output.writeVarInt(this.warningTime);
   }

   @Override
   public PacketType<ClientboundInitializeBorderPacket> type() {
      return GamePacketTypes.CLIENTBOUND_INITIALIZE_BORDER;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleInitializeBorder(this);
   }

   public double getNewCenterX() {
      return this.newCenterX;
   }

   public double getNewCenterZ() {
      return this.newCenterZ;
   }

   public double getNewSize() {
      return this.newSize;
   }

   public double getOldSize() {
      return this.oldSize;
   }

   public long getLerpTime() {
      return this.lerpTime;
   }

   public int getNewAbsoluteMaxSize() {
      return this.newAbsoluteMaxSize;
   }

   public int getWarningTime() {
      return this.warningTime;
   }

   public int getWarningBlocks() {
      return this.warningBlocks;
   }
}
