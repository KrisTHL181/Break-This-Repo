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
   private static final ClientboundBossEventPacket.Operation REMOVE_OPERATION = new ClientboundBossEventPacket.Operation() {
      @Override
      public ClientboundBossEventPacket.OperationType getType() {
         return ClientboundBossEventPacket.OperationType.REMOVE;
      }

      @Override
      public void dispatch(final UUID id, final ClientboundBossEventPacket.Handler handler) {
         handler.remove(id);
      }

      @Override
      public void write(final RegistryFriendlyByteBuf output) {
      }
   };

   private ClientboundBossEventPacket(final UUID id, final ClientboundBossEventPacket.Operation operation) {
      this.id = id;
      this.operation = operation;
   }

   private ClientboundBossEventPacket(final RegistryFriendlyByteBuf input) {
      this.id = input.readUUID();
      ClientboundBossEventPacket.OperationType type = input.readEnum(ClientboundBossEventPacket.OperationType.class);
      this.operation = type.reader.decode(input);
   }

   public static ClientboundBossEventPacket createAddPacket(final BossEvent event) {
      return new ClientboundBossEventPacket(event.getId(), new ClientboundBossEventPacket.AddOperation(event));
   }

   public static ClientboundBossEventPacket createRemovePacket(final UUID id) {
      return new ClientboundBossEventPacket(id, REMOVE_OPERATION);
   }

   public static ClientboundBossEventPacket createUpdateProgressPacket(final BossEvent event) {
      return new ClientboundBossEventPacket(event.getId(), new ClientboundBossEventPacket.UpdateProgressOperation(event.getProgress()));
   }

   public static ClientboundBossEventPacket createUpdateNamePacket(final BossEvent event) {
      return new ClientboundBossEventPacket(event.getId(), new ClientboundBossEventPacket.UpdateNameOperation(event.getName()));
   }

   public static ClientboundBossEventPacket createUpdateStylePacket(final BossEvent event) {
      return new ClientboundBossEventPacket(event.getId(), new ClientboundBossEventPacket.UpdateStyleOperation(event.getColor(), event.getOverlay()));
   }

   public static ClientboundBossEventPacket createUpdatePropertiesPacket(final BossEvent event) {
      return new ClientboundBossEventPacket(
         event.getId(),
         new ClientboundBossEventPacket.UpdatePropertiesOperation(event.shouldDarkenScreen(), event.shouldPlayBossMusic(), event.shouldCreateWorldFog())
      );
   }

   private void write(final RegistryFriendlyByteBuf output) {
      output.writeUUID(this.id);
      output.writeEnum(this.operation.getType());
      this.operation.write(output);
   }

   private static int encodeProperties(final boolean darkenScreen, final boolean playMusic, final boolean createWorldFog) {
      int properties = 0;
      if (darkenScreen) {
         properties |= 1;
      }

      if (playMusic) {
         properties |= 2;
      }

      if (createWorldFog) {
         properties |= 4;
      }

      return properties;
   }

   @Override
   public PacketType<ClientboundBossEventPacket> type() {
      return GamePacketTypes.CLIENTBOUND_BOSS_EVENT;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleBossUpdate(this);
   }

   public void dispatch(final ClientboundBossEventPacket.Handler handler) {
      this.operation.dispatch(this.id, handler);
   }

   private static class AddOperation implements ClientboundBossEventPacket.Operation {
      private final Component name;
      private final float progress;
      private final BossEvent.BossBarColor color;
      private final BossEvent.BossBarOverlay overlay;
      private final boolean darkenScreen;
      private final boolean playMusic;
      private final boolean createWorldFog;

      private AddOperation(final BossEvent event) {
         this.name = event.getName();
         this.progress = event.getProgress();
         this.color = event.getColor();
         this.overlay = event.getOverlay();
         this.darkenScreen = event.shouldDarkenScreen();
         this.playMusic = event.shouldPlayBossMusic();
         this.createWorldFog = event.shouldCreateWorldFog();
      }

      private AddOperation(final RegistryFriendlyByteBuf input) {
         this.name = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(input);
         this.progress = input.readFloat();
         this.color = input.readEnum(BossEvent.BossBarColor.class);
         this.overlay = input.readEnum(BossEvent.BossBarOverlay.class);
         int flags = input.readUnsignedByte();
         this.darkenScreen = (flags & 1) > 0;
         this.playMusic = (flags & 2) > 0;
         this.createWorldFog = (flags & 4) > 0;
      }

      @Override
      public ClientboundBossEventPacket.OperationType getType() {
         return ClientboundBossEventPacket.OperationType.ADD;
      }

      @Override
      public void dispatch(final UUID id, final ClientboundBossEventPacket.Handler handler) {
         handler.add(id, this.name, this.progress, this.color, this.overlay, this.darkenScreen, this.playMusic, this.createWorldFog);
      }

      @Override
      public void write(final RegistryFriendlyByteBuf output) {
         ComponentSerialization.TRUSTED_STREAM_CODEC.encode(output, this.name);
         output.writeFloat(this.progress);
         output.writeEnum(this.color);
         output.writeEnum(this.overlay);
         output.writeByte(ClientboundBossEventPacket.encodeProperties(this.darkenScreen, this.playMusic, this.createWorldFog));
      }
   }

   public interface Handler {
      default void add(
         final UUID id,
         final Component name,
         final float progress,
         final BossEvent.BossBarColor color,
         final BossEvent.BossBarOverlay overlay,
         final boolean darkenScreen,
         final boolean playMusic,
         final boolean createWorldFog
      ) {
      }

      default void remove(final UUID id) {
      }

      default void updateProgress(final UUID id, final float progress) {
      }

      default void updateName(final UUID id, final Component name) {
      }

      default void updateStyle(final UUID id, final BossEvent.BossBarColor color, final BossEvent.BossBarOverlay overlay) {
      }

      default void updateProperties(final UUID id, final boolean darkenScreen, final boolean playMusic, final boolean createWorldFog) {
      }
   }

   private interface Operation {
      ClientboundBossEventPacket.OperationType getType();

      void dispatch(UUID id, ClientboundBossEventPacket.Handler handler);

      void write(RegistryFriendlyByteBuf output);
   }

   private enum OperationType {
      ADD(ClientboundBossEventPacket.AddOperation::new),
      REMOVE(input -> ClientboundBossEventPacket.REMOVE_OPERATION),
      UPDATE_PROGRESS(ClientboundBossEventPacket.UpdateProgressOperation::new),
      UPDATE_NAME(ClientboundBossEventPacket.UpdateNameOperation::new),
      UPDATE_STYLE(ClientboundBossEventPacket.UpdateStyleOperation::new),
      UPDATE_PROPERTIES(ClientboundBossEventPacket.UpdatePropertiesOperation::new);

      private final StreamDecoder<RegistryFriendlyByteBuf, ClientboundBossEventPacket.Operation> reader;

      OperationType(final StreamDecoder<RegistryFriendlyByteBuf, ClientboundBossEventPacket.Operation> reader) {
         this.reader = reader;
      }
   }

   private record UpdateNameOperation(Component name) implements ClientboundBossEventPacket.Operation {
      private UpdateNameOperation(final RegistryFriendlyByteBuf input) {
         this(ComponentSerialization.TRUSTED_STREAM_CODEC.decode(input));
      }

      @Override
      public ClientboundBossEventPacket.OperationType getType() {
         return ClientboundBossEventPacket.OperationType.UPDATE_NAME;
      }

      @Override
      public void dispatch(final UUID id, final ClientboundBossEventPacket.Handler handler) {
         handler.updateName(id, this.name);
      }

      @Override
      public void write(final RegistryFriendlyByteBuf output) {
         ComponentSerialization.TRUSTED_STREAM_CODEC.encode(output, this.name);
      }
   }

   private record UpdateProgressOperation(float progress) implements ClientboundBossEventPacket.Operation {
      private UpdateProgressOperation(final RegistryFriendlyByteBuf input) {
         this(input.readFloat());
      }

      @Override
      public ClientboundBossEventPacket.OperationType getType() {
         return ClientboundBossEventPacket.OperationType.UPDATE_PROGRESS;
      }

      @Override
      public void dispatch(final UUID id, final ClientboundBossEventPacket.Handler handler) {
         handler.updateProgress(id, this.progress);
      }

      @Override
      public void write(final RegistryFriendlyByteBuf output) {
         output.writeFloat(this.progress);
      }
   }

   private static class UpdatePropertiesOperation implements ClientboundBossEventPacket.Operation {
      private final boolean darkenScreen;
      private final boolean playMusic;
      private final boolean createWorldFog;

      private UpdatePropertiesOperation(final boolean darkenScreen, final boolean playMusic, final boolean createWorldFog) {
         this.darkenScreen = darkenScreen;
         this.playMusic = playMusic;
         this.createWorldFog = createWorldFog;
      }

      private UpdatePropertiesOperation(final RegistryFriendlyByteBuf input) {
         int flags = input.readUnsignedByte();
         this.darkenScreen = (flags & 1) > 0;
         this.playMusic = (flags & 2) > 0;
         this.createWorldFog = (flags & 4) > 0;
      }

      @Override
      public ClientboundBossEventPacket.OperationType getType() {
         return ClientboundBossEventPacket.OperationType.UPDATE_PROPERTIES;
      }

      @Override
      public void dispatch(final UUID id, final ClientboundBossEventPacket.Handler handler) {
         handler.updateProperties(id, this.darkenScreen, this.playMusic, this.createWorldFog);
      }

      @Override
      public void write(final RegistryFriendlyByteBuf output) {
         output.writeByte(ClientboundBossEventPacket.encodeProperties(this.darkenScreen, this.playMusic, this.createWorldFog));
      }
   }

   private static class UpdateStyleOperation implements ClientboundBossEventPacket.Operation {
      private final BossEvent.BossBarColor color;
      private final BossEvent.BossBarOverlay overlay;

      private UpdateStyleOperation(final BossEvent.BossBarColor color, final BossEvent.BossBarOverlay overlay) {
         this.color = color;
         this.overlay = overlay;
      }

      private UpdateStyleOperation(final RegistryFriendlyByteBuf input) {
         this.color = input.readEnum(BossEvent.BossBarColor.class);
         this.overlay = input.readEnum(BossEvent.BossBarOverlay.class);
      }

      @Override
      public ClientboundBossEventPacket.OperationType getType() {
         return ClientboundBossEventPacket.OperationType.UPDATE_STYLE;
      }

      @Override
      public void dispatch(final UUID id, final ClientboundBossEventPacket.Handler handler) {
         handler.updateStyle(id, this.color, this.overlay);
      }

      @Override
      public void write(final RegistryFriendlyByteBuf output) {
         output.writeEnum(this.color);
         output.writeEnum(this.overlay);
      }
   }
}
