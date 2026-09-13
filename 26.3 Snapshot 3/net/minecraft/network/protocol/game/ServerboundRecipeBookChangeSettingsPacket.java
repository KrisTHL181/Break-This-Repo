package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.inventory.RecipeBookType;

public class ServerboundRecipeBookChangeSettingsPacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundRecipeBookChangeSettingsPacket> STREAM_CODEC = Packet.codec(
      ServerboundRecipeBookChangeSettingsPacket::write, ServerboundRecipeBookChangeSettingsPacket::new
   );
   private final RecipeBookType bookType;
   private final boolean isOpen;
   private final boolean isFiltering;

   public ServerboundRecipeBookChangeSettingsPacket(final RecipeBookType bookType, final boolean isOpen, final boolean isFiltering) {
      this.bookType = bookType;
      this.isOpen = isOpen;
      this.isFiltering = isFiltering;
   }

   private ServerboundRecipeBookChangeSettingsPacket(final FriendlyByteBuf input) {
      this.bookType = input.readEnum(RecipeBookType.class);
      this.isOpen = input.readBoolean();
      this.isFiltering = input.readBoolean();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeEnum(this.bookType);
      output.writeBoolean(this.isOpen);
      output.writeBoolean(this.isFiltering);
   }

   @Override
   public PacketType<ServerboundRecipeBookChangeSettingsPacket> type() {
      return GamePacketTypes.SERVERBOUND_RECIPE_BOOK_CHANGE_SETTINGS;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleRecipeBookChangeSettingsPacket(this);
   }

   public RecipeBookType getBookType() {
      return this.bookType;
   }

   public boolean isOpen() {
      return this.isOpen;
   }

   public boolean isFiltering() {
      return this.isFiltering;
   }
}
