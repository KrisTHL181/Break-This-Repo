package net.minecraft.network.protocol.game;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;

public record ClientboundRecipeBookAddPacket(List<ClientboundRecipeBookAddPacket.Entry> entries, boolean replace) implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundRecipeBookAddPacket> STREAM_CODEC = StreamCodec.composite(
      ClientboundRecipeBookAddPacket.Entry.STREAM_CODEC.apply(ByteBufCodecs.list()),
      ClientboundRecipeBookAddPacket::entries,
      ByteBufCodecs.BOOL,
      ClientboundRecipeBookAddPacket::replace,
      ClientboundRecipeBookAddPacket::new
   );

   @Override
   public PacketType<ClientboundRecipeBookAddPacket> type() {
      return GamePacketTypes.CLIENTBOUND_RECIPE_BOOK_ADD;
   }

   public void handle(ClientGamePacketListener p_369082_) {
      p_369082_.handleRecipeBookAdd(this);
   }

   public record Entry(RecipeDisplayEntry contents, byte flags) {
      public static final byte FLAG_NOTIFICATION = 1;
      public static final byte FLAG_HIGHLIGHT = 2;
      public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundRecipeBookAddPacket.Entry> STREAM_CODEC = StreamCodec.composite(
         RecipeDisplayEntry.STREAM_CODEC,
         ClientboundRecipeBookAddPacket.Entry::contents,
         ByteBufCodecs.BYTE,
         ClientboundRecipeBookAddPacket.Entry::flags,
         ClientboundRecipeBookAddPacket.Entry::new
      );

      public Entry(RecipeDisplayEntry p_369640_, boolean p_365244_, boolean p_364233_) {
         this(p_369640_, (byte)((p_365244_ ? 1 : 0) | (p_364233_ ? 2 : 0)));
      }

      public boolean notification() {
         return (this.flags & 1) != 0;
      }

      public boolean highlight() {
         return (this.flags & 2) != 0;
      }
   }
}
