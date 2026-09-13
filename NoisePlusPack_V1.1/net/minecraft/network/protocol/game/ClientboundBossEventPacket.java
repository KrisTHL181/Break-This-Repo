package net.minecraft.network.protocol.game;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.BossEvent;

public class ClientboundBossEventPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundBossEventPacket> STREAM_CODEC = Packet.codec(
      ClientboundBossEventPacket::write, ClientboundBossEventPacket::new
   );
   private static final int FLAG_DARKEN = 1;
   private static final int FLAG_MUSIC = 2;
   private static final int FLAG_FOG = 4;
   private final UUID id;
   private final ClientboundBossEventPacket.Operation operation;
   static final ClientboundBossEventPacket.Operation REMOVE_OPERATION = new ClientboundBossEventPacket.Operation() {
      @Override
      public ClientboundBossEventPacket.OperationType getType() {
         return ClientboundBossEventPacket.OperationType.REMOVE;
      }

      @Override
      public void dispatch(UUID p_178660_, ClientboundBossEventPacket.Handler p_178661_) {
         p_178661_.remove(p_178660_);
      }

      @Override
      public void write(RegistryFriendlyByteBuf p_328351_) {
      }
   };

   private ClientboundBossEventPacket(UUID p_178635_, ClientboundBossEventPacket.Operation p_178636_) {
      this.id = p_178635_;
      this.operation = p_178636_;
   }

   private ClientboundBossEventPacket(RegistryFriendlyByteBuf p_333479_) {
      this.id = p_333479_.readUUID();
      ClientboundBossEventPacket.OperationType clientboundbosseventpacket$operationtype = p_333479_.readEnum(ClientboundBossEventPacket.OperationType.class);
      this.operation = clientboundbosseventpacket$operationtype.reader.decode(p_333479_);
   }

   public static ClientboundBossEventPacket createAddPacket(BossEvent p_178640_) {
      return new ClientboundBossEventPacket(p_178640_.getId(), new ClientboundBossEventPacket.AddOperation(p_178640_));
   }

   public static ClientboundBossEventPacket createRemovePacket(UUID p_178642_) {
      return new ClientboundBossEventPacket(p_178642_, REMOVE_OPERATION);
   }

   public static ClientboundBossEventPacket createUpdateProgressPacket(BossEvent p_178650_) {
      return new ClientboundBossEventPacket(p_178650_.getId(), new ClientboundBossEventPacket.UpdateProgressOperation(p_178650_.getProgress()));
   }

   public static ClientboundBossEventPacket createUpdateNamePacket(BossEvent p_178652_) {
      return new ClientboundBossEventPacket(p_178652_.getId(), new ClientboundBossEventPacket.UpdateNameOperation(p_178652_.getName()));
   }

   public static ClientboundBossEventPacket createUpdateStylePacket(BossEvent p_178654_) {
      return new ClientboundBossEventPacket(
         p_178654_.getId(), new ClientboundBossEventPacket.UpdateStyleOperation(p_178654_.getColor(), p_178654_.getOverlay())
      );
   }

   public static ClientboundBossEventPacket createUpdatePropertiesPacket(BossEvent p_178656_) {
      return new ClientboundBossEventPacket(
         p_178656_.getId(),
         new ClientboundBossEventPacket.UpdatePropertiesOperation(
            p_178656_.shouldDarkenScreen(), p_178656_.shouldPlayBossMusic(), p_178656_.shouldCreateWorldFog()
         )
      );
   }

   private void write(RegistryFriendlyByteBuf p_331755_) {
      p_331755_.writeUUID(this.id);
      p_331755_.writeEnum(this.operation.getType());
      this.operation.write(p_331755_);
   }

   static int encodeProperties(boolean p_178646_, boolean p_178647_, boolean p_178648_) {
      int i = 0;
      if (p_178646_) {
         i |= 1;
      }

      if (p_178647_) {
         i |= 2;
      }

      if (p_178648_) {
         i |= 4;
      }

      return i;
   }

   @Override
   public PacketType<ClientboundBossEventPacket> type() {
      return GamePacketTypes.CLIENTBOUND_BOSS_EVENT;
   }

   public void handle(ClientGamePacketListener p_131770_) {
      p_131770_.handleBossUpdate(this);
   }

   public void dispatch(ClientboundBossEventPacket.Handler p_178644_) {
      this.operation.dispatch(this.id, p_178644_);
   }

   static class AddOperation implements ClientboundBossEventPacket.Operation {
      private final Component name;
      private final float progress;
      private final BossEvent.BossBarColor color;
      private final BossEvent.BossBarOverlay overlay;
      private final boolean darkenScreen;
      private final boolean playMusic;
      private final boolean createWorldFog;

      AddOperation(BossEvent p_178672_) {
         this.name = p_178672_.getName();
         this.progress = p_178672_.getProgress();
         this.color = p_178672_.getColor();
         this.overlay = p_178672_.getOverlay();
         this.darkenScreen = p_178672_.shouldDarkenScreen();
         this.playMusic = p_178672_.shouldPlayBossMusic();
         this.createWorldFog = p_178672_.shouldCreateWorldFog();
      }

      private AddOperation(RegistryFriendlyByteBuf p_331482_) {
         this.name = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(p_331482_);
         this.progress = p_331482_.readFloat();
         this.color = p_331482_.readEnum(BossEvent.BossBarColor.class);
         this.overlay = p_331482_.readEnum(BossEvent.BossBarOverlay.class);
         int i = p_331482_.readUnsignedByte();
         this.darkenScreen = (i & 1) > 0;
         this.playMusic = (i & 2) > 0;
         this.createWorldFog = (i & 4) > 0;
      }

      @Override
      public ClientboundBossEventPacket.OperationType getType() {
         return ClientboundBossEventPacket.OperationType.ADD;
      }

      @Override
      public void dispatch(UUID p_178677_, ClientboundBossEventPacket.Handler p_178678_) {
         p_178678_.add(p_178677_, this.name, this.progress, this.color, this.overlay, this.darkenScreen, this.playMusic, this.createWorldFog);
      }

      @Override
      public void write(RegistryFriendlyByteBuf p_330694_) {
         ComponentSerialization.TRUSTED_STREAM_CODEC.encode(p_330694_, this.name);
         p_330694_.writeFloat(this.progress);
         p_330694_.writeEnum(this.color);
         p_330694_.writeEnum(this.overlay);
         p_330694_.writeByte(ClientboundBossEventPacket.encodeProperties(this.darkenScreen, this.playMusic, this.createWorldFog));
      }
   }

   public interface Handler {
      default void add(
         UUID p_178689_,
         Component p_178690_,
         float p_178691_,
         BossEvent.BossBarColor p_178692_,
         BossEvent.BossBarOverlay p_178693_,
         boolean p_178694_,
         boolean p_178695_,
         boolean p_178696_
      ) {
      }

      default void remove(UUID p_178681_) {
      }

      default void updateProgress(UUID p_178682_, float p_178683_) {
      }

      default void updateName(UUID p_178687_, Component p_178688_) {
      }

      default void updateStyle(UUID p_178684_, BossEvent.BossBarColor p_178685_, BossEvent.BossBarOverlay p_178686_) {
      }

      default void updateProperties(UUID p_178697_, boolean p_178698_, boolean p_178699_, boolean p_178700_) {
      }
   }

   interface Operation {
      ClientboundBossEventPacket.OperationType getType();

      void dispatch(UUID var1, ClientboundBossEventPacket.Handler var2);

      void write(RegistryFriendlyByteBuf var1);
   }

   enum OperationType {
      ADD(ClientboundBossEventPacket.AddOperation::new),
      REMOVE(p_329619_ -> ClientboundBossEventPacket.REMOVE_OPERATION),
      UPDATE_PROGRESS(ClientboundBossEventPacket.UpdateProgressOperation::new),
      UPDATE_NAME(ClientboundBossEventPacket.UpdateNameOperation::new),
      UPDATE_STYLE(ClientboundBossEventPacket.UpdateStyleOperation::new),
      UPDATE_PROPERTIES(ClientboundBossEventPacket.UpdatePropertiesOperation::new);

      final StreamDecoder<RegistryFriendlyByteBuf, ClientboundBossEventPacket.Operation> reader;

      OperationType(final StreamDecoder<RegistryFriendlyByteBuf, ClientboundBossEventPacket.Operation> p_328519_) {
         this.reader = p_328519_;
      }
   }

   record UpdateNameOperation(Component name) implements ClientboundBossEventPacket.Operation {
      private UpdateNameOperation(RegistryFriendlyByteBuf p_335508_) {
         this(ComponentSerialization.TRUSTED_STREAM_CODEC.decode(p_335508_));
      }

      @Override
      public ClientboundBossEventPacket.OperationType getType() {
         return ClientboundBossEventPacket.OperationType.UPDATE_NAME;
      }

      @Override
      public void dispatch(UUID p_178730_, ClientboundBossEventPacket.Handler p_178731_) {
         p_178731_.updateName(p_178730_, this.name);
      }

      @Override
      public void write(RegistryFriendlyByteBuf p_332126_) {
         ComponentSerialization.TRUSTED_STREAM_CODEC.encode(p_332126_, this.name);
      }
   }

   record UpdateProgressOperation(float progress) implements ClientboundBossEventPacket.Operation {
      private UpdateProgressOperation(RegistryFriendlyByteBuf p_329547_) {
         this(p_329547_.readFloat());
      }

      @Override
      public ClientboundBossEventPacket.OperationType getType() {
         return ClientboundBossEventPacket.OperationType.UPDATE_PROGRESS;
      }

      @Override
      public void dispatch(UUID p_178741_, ClientboundBossEventPacket.Handler p_178742_) {
         p_178742_.updateProgress(p_178741_, this.progress);
      }

      @Override
      public void write(RegistryFriendlyByteBuf p_331612_) {
         p_331612_.writeFloat(this.progress);
      }
   }

   static class UpdatePropertiesOperation implements ClientboundBossEventPacket.Operation {
      private final boolean darkenScreen;
      private final boolean playMusic;
      private final boolean createWorldFog;

      UpdatePropertiesOperation(boolean p_178751_, boolean p_178752_, boolean p_178753_) {
         this.darkenScreen = p_178751_;
         this.playMusic = p_178752_;
         this.createWorldFog = p_178753_;
      }

      private UpdatePropertiesOperation(RegistryFriendlyByteBuf p_331654_) {
         int i = p_331654_.readUnsignedByte();
         this.darkenScreen = (i & 1) > 0;
         this.playMusic = (i & 2) > 0;
         this.createWorldFog = (i & 4) > 0;
      }

      @Override
      public ClientboundBossEventPacket.OperationType getType() {
         return ClientboundBossEventPacket.OperationType.UPDATE_PROPERTIES;
      }

      @Override
      public void dispatch(UUID p_178756_, ClientboundBossEventPacket.Handler p_178757_) {
         p_178757_.updateProperties(p_178756_, this.darkenScreen, this.playMusic, this.createWorldFog);
      }

      @Override
      public void write(RegistryFriendlyByteBuf p_327814_) {
         p_327814_.writeByte(ClientboundBossEventPacket.encodeProperties(this.darkenScreen, this.playMusic, this.createWorldFog));
      }
   }

   static class UpdateStyleOperation implements ClientboundBossEventPacket.Operation {
      private final BossEvent.BossBarColor color;
      private final BossEvent.BossBarOverlay overlay;

      UpdateStyleOperation(BossEvent.BossBarColor p_178763_, BossEvent.BossBarOverlay p_178764_) {
         this.color = p_178763_;
         this.overlay = p_178764_;
      }

      private UpdateStyleOperation(RegistryFriendlyByteBuf p_328394_) {
         this.color = p_328394_.readEnum(BossEvent.BossBarColor.class);
         this.overlay = p_328394_.readEnum(BossEvent.BossBarOverlay.class);
      }

      @Override
      public ClientboundBossEventPacket.OperationType getType() {
         return ClientboundBossEventPacket.OperationType.UPDATE_STYLE;
      }

      @Override
      public void dispatch(UUID p_178769_, ClientboundBossEventPacket.Handler p_178770_) {
         p_178770_.updateStyle(p_178769_, this.color, this.overlay);
      }

      @Override
      public void write(RegistryFriendlyByteBuf p_332920_) {
         p_332920_.writeEnum(this.color);
         p_332920_.writeEnum(this.overlay);
      }
   }
}
