package net.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundGameEventPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundGameEventPacket> STREAM_CODEC = Packet.codec(
      ClientboundGameEventPacket::write, ClientboundGameEventPacket::new
   );
   public static final ClientboundGameEventPacket.Type NO_RESPAWN_BLOCK_AVAILABLE = new ClientboundGameEventPacket.Type(0);
   public static final ClientboundGameEventPacket.Type START_RAINING = new ClientboundGameEventPacket.Type(1);
   public static final ClientboundGameEventPacket.Type STOP_RAINING = new ClientboundGameEventPacket.Type(2);
   public static final ClientboundGameEventPacket.Type CHANGE_GAME_MODE = new ClientboundGameEventPacket.Type(3);
   public static final ClientboundGameEventPacket.Type WIN_GAME = new ClientboundGameEventPacket.Type(4);
   public static final ClientboundGameEventPacket.Type DEMO_EVENT = new ClientboundGameEventPacket.Type(5);
   public static final ClientboundGameEventPacket.Type PLAY_ARROW_HIT_SOUND = new ClientboundGameEventPacket.Type(6);
   public static final ClientboundGameEventPacket.Type RAIN_LEVEL_CHANGE = new ClientboundGameEventPacket.Type(7);
   public static final ClientboundGameEventPacket.Type THUNDER_LEVEL_CHANGE = new ClientboundGameEventPacket.Type(8);
   public static final ClientboundGameEventPacket.Type PUFFER_FISH_STING = new ClientboundGameEventPacket.Type(9);
   public static final ClientboundGameEventPacket.Type GUARDIAN_ELDER_EFFECT = new ClientboundGameEventPacket.Type(10);
   public static final ClientboundGameEventPacket.Type IMMEDIATE_RESPAWN = new ClientboundGameEventPacket.Type(11);
   public static final ClientboundGameEventPacket.Type LIMITED_CRAFTING = new ClientboundGameEventPacket.Type(12);
   public static final ClientboundGameEventPacket.Type LEVEL_CHUNKS_LOAD_START = new ClientboundGameEventPacket.Type(13);
   public static final int DEMO_PARAM_INTRO = 0;
   public static final int DEMO_PARAM_HINT_1 = 101;
   public static final int DEMO_PARAM_HINT_2 = 102;
   public static final int DEMO_PARAM_HINT_3 = 103;
   public static final int DEMO_PARAM_HINT_4 = 104;
   private final ClientboundGameEventPacket.Type event;
   private final float param;

   public ClientboundGameEventPacket(ClientboundGameEventPacket.Type p_132170_, float p_132171_) {
      this.event = p_132170_;
      this.param = p_132171_;
   }

   private ClientboundGameEventPacket(FriendlyByteBuf p_178865_) {
      this.event = (ClientboundGameEventPacket.Type)ClientboundGameEventPacket.Type.TYPES.get(p_178865_.readUnsignedByte());
      this.param = p_178865_.readFloat();
   }

   private void write(FriendlyByteBuf p_132180_) {
      p_132180_.writeByte(this.event.id);
      p_132180_.writeFloat(this.param);
   }

   @Override
   public PacketType<ClientboundGameEventPacket> type() {
      return GamePacketTypes.CLIENTBOUND_GAME_EVENT;
   }

   public void handle(ClientGamePacketListener p_132177_) {
      p_132177_.handleGameEvent(this);
   }

   public ClientboundGameEventPacket.Type getEvent() {
      return this.event;
   }

   public float getParam() {
      return this.param;
   }

   public static class Type {
      static final Int2ObjectMap<ClientboundGameEventPacket.Type> TYPES = new Int2ObjectOpenHashMap();
      final int id;

      public Type(int p_132186_) {
         this.id = p_132186_;
         TYPES.put(p_132186_, this);
      }
   }
}
