package net.minecraft.network.protocol.game;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ClientboundCommandSuggestionsPacket(int id, int start, int length, List<ClientboundCommandSuggestionsPacket.Entry> suggestions)
   implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundCommandSuggestionsPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      ClientboundCommandSuggestionsPacket::id,
      ByteBufCodecs.VAR_INT,
      ClientboundCommandSuggestionsPacket::start,
      ByteBufCodecs.VAR_INT,
      ClientboundCommandSuggestionsPacket::length,
      ClientboundCommandSuggestionsPacket.Entry.STREAM_CODEC.apply(ByteBufCodecs.list()),
      ClientboundCommandSuggestionsPacket::suggestions,
      ClientboundCommandSuggestionsPacket::new
   );

   public ClientboundCommandSuggestionsPacket(int p_131846_, Suggestions p_131847_) {
      this(
         p_131846_,
         p_131847_.getRange().getStart(),
         p_131847_.getRange().getLength(),
         p_131847_.getList()
            .stream()
            .map(
               p_326097_ -> new ClientboundCommandSuggestionsPacket.Entry(
                  p_326097_.getText(), Optional.ofNullable(p_326097_.getTooltip()).map(ComponentUtils::fromMessage)
               )
            )
            .toList()
      );
   }

   @Override
   public PacketType<ClientboundCommandSuggestionsPacket> type() {
      return GamePacketTypes.CLIENTBOUND_COMMAND_SUGGESTIONS;
   }

   public void handle(ClientGamePacketListener p_131853_) {
      p_131853_.handleCommandSuggestions(this);
   }

   public Suggestions toSuggestions() {
      StringRange stringrange = StringRange.between(this.start, this.start + this.length);
      return new Suggestions(
         stringrange, this.suggestions.stream().map(p_326096_ -> new Suggestion(stringrange, p_326096_.text(), p_326096_.tooltip().orElse(null))).toList()
      );
   }

   public record Entry(String text, Optional<Component> tooltip) {
      public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundCommandSuggestionsPacket.Entry> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.STRING_UTF8,
         ClientboundCommandSuggestionsPacket.Entry::text,
         ComponentSerialization.TRUSTED_OPTIONAL_STREAM_CODEC,
         ClientboundCommandSuggestionsPacket.Entry::tooltip,
         ClientboundCommandSuggestionsPacket.Entry::new
      );
   }
}
