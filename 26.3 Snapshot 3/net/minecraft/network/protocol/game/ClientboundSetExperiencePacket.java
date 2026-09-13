package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundSetExperiencePacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundSetExperiencePacket> STREAM_CODEC = Packet.codec(
      ClientboundSetExperiencePacket::write, ClientboundSetExperiencePacket::new
   );
   private final float experienceProgress;
   private final int totalExperience;
   private final int experienceLevel;

   public ClientboundSetExperiencePacket(final float experienceProgress, final int totalExperience, final int experienceLevel) {
      this.experienceProgress = experienceProgress;
      this.totalExperience = totalExperience;
      this.experienceLevel = experienceLevel;
   }

   private ClientboundSetExperiencePacket(final FriendlyByteBuf input) {
      this.experienceProgress = input.readFloat();
      this.experienceLevel = input.readVarInt();
      this.totalExperience = input.readVarInt();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeFloat(this.experienceProgress);
      output.writeVarInt(this.experienceLevel);
      output.writeVarInt(this.totalExperience);
   }

   @Override
   public PacketType<ClientboundSetExperiencePacket> type() {
      return GamePacketTypes.CLIENTBOUND_SET_EXPERIENCE;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleSetExperience(this);
   }

   public float getExperienceProgress() {
      return this.experienceProgress;
   }

   public int getTotalExperience() {
      return this.totalExperience;
   }

   public int getExperienceLevel() {
      return this.experienceLevel;
   }
}
