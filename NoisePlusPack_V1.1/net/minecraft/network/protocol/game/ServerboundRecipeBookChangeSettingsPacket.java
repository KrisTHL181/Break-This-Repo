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

   public ServerboundRecipeBookChangeSettingsPacket(RecipeBookType p_134366_, boolean p_134367_, boolean p_134368_) {
      this.bookType = p_134366_;
      this.isOpen = p_134367_;
      this.isFiltering = p_134368_;
   }

   private ServerboundRecipeBookChangeSettingsPacket(FriendlyByteBuf p_179734_) {
      this.bookType = p_179734_.readEnum(RecipeBookType.class);
      this.isOpen = p_179734_.readBoolean();
      this.isFiltering = p_179734_.readBoolean();
   }

   private void write(FriendlyByteBuf p_134377_) {
      p_134377_.writeEnum(this.bookType);
      p_134377_.writeBoolean(this.isOpen);
      p_134377_.writeBoolean(this.isFiltering);
   }

   @Override
   public PacketType<ServerboundRecipeBookChangeSettingsPacket> type() {
      return GamePacketTypes.SERVERBOUND_RECIPE_BOOK_CHANGE_SETTINGS;
   }

   public void handle(ServerGamePacketListener p_134374_) {
      p_134374_.handleRecipeBookChangeSettingsPacket(this);
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
