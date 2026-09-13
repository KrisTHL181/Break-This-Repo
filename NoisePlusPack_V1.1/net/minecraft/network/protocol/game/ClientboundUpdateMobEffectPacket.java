package net.minecraft.network.protocol.game;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public class ClientboundUpdateMobEffectPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundUpdateMobEffectPacket> STREAM_CODEC = Packet.codec(
      ClientboundUpdateMobEffectPacket::write, ClientboundUpdateMobEffectPacket::new
   );
   private static final int FLAG_AMBIENT = 1;
   private static final int FLAG_VISIBLE = 2;
   private static final int FLAG_SHOW_ICON = 4;
   private static final int FLAG_BLEND = 8;
   private final int entityId;
   private final Holder<MobEffect> effect;
   private final int effectAmplifier;
   private final int effectDurationTicks;
   private final byte flags;

   public ClientboundUpdateMobEffectPacket(int p_333405_, MobEffectInstance p_336386_, boolean p_333848_) {
      this.entityId = p_333405_;
      this.effect = p_336386_.getEffect();
      this.effectAmplifier = p_336386_.getAmplifier();
      this.effectDurationTicks = p_336386_.getDuration();
      byte b0 = 0;
      if (p_336386_.isAmbient()) {
         b0 = (byte)(b0 | 1);
      }

      if (p_336386_.isVisible()) {
         b0 = (byte)(b0 | 2);
      }

      if (p_336386_.showIcon()) {
         b0 = (byte)(b0 | 4);
      }

      if (p_333848_) {
         b0 = (byte)(b0 | 8);
      }

      this.flags = b0;
   }

   private ClientboundUpdateMobEffectPacket(RegistryFriendlyByteBuf p_334526_) {
      this.entityId = p_334526_.readVarInt();
      this.effect = MobEffect.STREAM_CODEC.decode(p_334526_);
      this.effectAmplifier = p_334526_.readVarInt();
      this.effectDurationTicks = p_334526_.readVarInt();
      this.flags = p_334526_.readByte();
   }

   private void write(RegistryFriendlyByteBuf p_332024_) {
      p_332024_.writeVarInt(this.entityId);
      MobEffect.STREAM_CODEC.encode(p_332024_, this.effect);
      p_332024_.writeVarInt(this.effectAmplifier);
      p_332024_.writeVarInt(this.effectDurationTicks);
      p_332024_.writeByte(this.flags);
   }

   @Override
   public PacketType<ClientboundUpdateMobEffectPacket> type() {
      return GamePacketTypes.CLIENTBOUND_UPDATE_MOB_EFFECT;
   }

   public void handle(ClientGamePacketListener p_133618_) {
      p_133618_.handleUpdateMobEffect(this);
   }

   public int getEntityId() {
      return this.entityId;
   }

   public Holder<MobEffect> getEffect() {
      return this.effect;
   }

   public int getEffectAmplifier() {
      return this.effectAmplifier;
   }

   public int getEffectDurationTicks() {
      return this.effectDurationTicks;
   }

   public boolean isEffectVisible() {
      return (this.flags & 2) != 0;
   }

   public boolean isEffectAmbient() {
      return (this.flags & 1) != 0;
   }

   public boolean effectShowsIcon() {
      return (this.flags & 4) != 0;
   }

   public boolean shouldBlend() {
      return (this.flags & 8) != 0;
   }
}
