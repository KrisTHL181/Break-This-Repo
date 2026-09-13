package net.minecraft.network.protocol.game;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class ServerboundSeenAdvancementsPacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundSeenAdvancementsPacket> STREAM_CODEC = Packet.codec(
      ServerboundSeenAdvancementsPacket::write, ServerboundSeenAdvancementsPacket::new
   );
   private final ServerboundSeenAdvancementsPacket.Action action;
   private final @Nullable Identifier tab;

   public ServerboundSeenAdvancementsPacket(final ServerboundSeenAdvancementsPacket.Action action, final @Nullable Identifier tab) {
      this.action = action;
      this.tab = tab;
   }

   public static ServerboundSeenAdvancementsPacket openedTab(final AdvancementHolder tab) {
      return new ServerboundSeenAdvancementsPacket(ServerboundSeenAdvancementsPacket.Action.OPENED_TAB, tab.id());
   }

   public static ServerboundSeenAdvancementsPacket closedScreen() {
      return new ServerboundSeenAdvancementsPacket(ServerboundSeenAdvancementsPacket.Action.CLOSED_SCREEN, null);
   }

   private ServerboundSeenAdvancementsPacket(final FriendlyByteBuf input) {
      this.action = input.readEnum(ServerboundSeenAdvancementsPacket.Action.class);
      if (this.action == ServerboundSeenAdvancementsPacket.Action.OPENED_TAB) {
         this.tab = input.readIdentifier();
      } else {
         this.tab = null;
      }
   }

   private void write(final FriendlyByteBuf output) {
      output.writeEnum(this.action);
      if (this.action == ServerboundSeenAdvancementsPacket.Action.OPENED_TAB) {
         output.writeIdentifier(this.tab);
      }
   }

   @Override
   public PacketType<ServerboundSeenAdvancementsPacket> type() {
      return GamePacketTypes.SERVERBOUND_SEEN_ADVANCEMENTS;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleSeenAdvancements(this);
   }

   public ServerboundSeenAdvancementsPacket.Action getAction() {
      return this.action;
   }

   public @Nullable Identifier getTab() {
      return this.tab;
   }

   public enum Action {
      OPENED_TAB,
      CLOSED_SCREEN;
   }
}
