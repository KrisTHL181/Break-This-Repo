package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.player.Abilities;

public class ClientboundPlayerAbilitiesPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundPlayerAbilitiesPacket> STREAM_CODEC = Packet.codec(
      ClientboundPlayerAbilitiesPacket::write, ClientboundPlayerAbilitiesPacket::new
   );
   private static final int FLAG_INVULNERABLE = 1;
   private static final int FLAG_FLYING = 2;
   private static final int FLAG_CAN_FLY = 4;
   private static final int FLAG_INSTABUILD = 8;
   private final boolean invulnerable;
   private final boolean isFlying;
   private final boolean canFly;
   private final boolean instabuild;
   private final float flyingSpeed;
   private final float walkingSpeed;

   public ClientboundPlayerAbilitiesPacket(final Abilities abilities) {
      this.invulnerable = abilities.invulnerable;
      this.isFlying = abilities.flying;
      this.canFly = abilities.mayfly;
      this.instabuild = abilities.instabuild;
      this.flyingSpeed = abilities.getFlyingSpeed();
      this.walkingSpeed = abilities.getWalkingSpeed();
   }

   private ClientboundPlayerAbilitiesPacket(final FriendlyByteBuf input) {
      byte bitfield = input.readByte();
      this.invulnerable = (bitfield & 1) != 0;
      this.isFlying = (bitfield & 2) != 0;
      this.canFly = (bitfield & 4) != 0;
      this.instabuild = (bitfield & 8) != 0;
      this.flyingSpeed = input.readFloat();
      this.walkingSpeed = input.readFloat();
   }

   private void write(final FriendlyByteBuf output) {
      byte bitfield = 0;
      if (this.invulnerable) {
         bitfield = (byte)(bitfield | 1);
      }

      if (this.isFlying) {
         bitfield = (byte)(bitfield | 2);
      }

      if (this.canFly) {
         bitfield = (byte)(bitfield | 4);
      }

      if (this.instabuild) {
         bitfield = (byte)(bitfield | 8);
      }

      output.writeByte(bitfield);
      output.writeFloat(this.flyingSpeed);
      output.writeFloat(this.walkingSpeed);
   }

   @Override
   public PacketType<ClientboundPlayerAbilitiesPacket> type() {
      return GamePacketTypes.CLIENTBOUND_PLAYER_ABILITIES;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handlePlayerAbilities(this);
   }

   public boolean isInvulnerable() {
      return this.invulnerable;
   }

   public boolean isFlying() {
      return this.isFlying;
   }

   public boolean canFly() {
      return this.canFly;
   }

   public boolean canInstabuild() {
      return this.instabuild;
   }

   public float getFlyingSpeed() {
      return this.flyingSpeed;
   }

   public float getWalkingSpeed() {
      return this.walkingSpeed;
   }
}
