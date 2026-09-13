package net.minecraft.server;

import com.mojang.datafixers.util.Either;
import io.netty.buffer.ByteBuf;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

public record ServerLinks(List<ServerLinks.Entry> entries) {
   public static final ServerLinks EMPTY = new ServerLinks(List.of());
   public static final StreamCodec<ByteBuf, Either<ServerLinks.KnownLinkType, Component>> TYPE_STREAM_CODEC = ByteBufCodecs.either(
      ServerLinks.KnownLinkType.STREAM_CODEC, ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC
   );
   public static final StreamCodec<ByteBuf, List<ServerLinks.UntrustedEntry>> UNTRUSTED_LINKS_STREAM_CODEC = ServerLinks.UntrustedEntry.STREAM_CODEC
      .apply(ByteBufCodecs.list());

   public boolean isEmpty() {
      return this.entries.isEmpty();
   }

   public Optional<ServerLinks.Entry> findKnownType(ServerLinks.KnownLinkType p_345340_) {
      return this.entries.stream().filter(p_344406_ -> (Boolean)p_344406_.type.map(p_344429_ -> p_344429_ == p_345340_, p_343563_ -> false)).findFirst();
   }

   public List<ServerLinks.UntrustedEntry> untrust() {
      return this.entries.stream().map(p_343119_ -> new ServerLinks.UntrustedEntry(p_343119_.type, p_343119_.link.toString())).toList();
   }

   public record Entry(Either<ServerLinks.KnownLinkType, Component> type, URI link) {
      public static ServerLinks.Entry knownType(ServerLinks.KnownLinkType p_344933_, URI p_343801_) {
         return new ServerLinks.Entry(Either.left(p_344933_), p_343801_);
      }

      public static ServerLinks.Entry custom(Component p_343423_, URI p_343780_) {
         return new ServerLinks.Entry(Either.right(p_343423_), p_343780_);
      }

      public Component displayName() {
         return (Component)this.type.map(ServerLinks.KnownLinkType::displayName, p_342867_ -> p_342867_);
      }
   }

   public enum KnownLinkType {
      BUG_REPORT(0, "report_bug"),
      COMMUNITY_GUIDELINES(1, "community_guidelines"),
      SUPPORT(2, "support"),
      STATUS(3, "status"),
      FEEDBACK(4, "feedback"),
      COMMUNITY(5, "community"),
      WEBSITE(6, "website"),
      FORUMS(7, "forums"),
      NEWS(8, "news"),
      ANNOUNCEMENTS(9, "announcements");

      private static final IntFunction<ServerLinks.KnownLinkType> BY_ID = ByIdMap.continuous(
         p_344812_ -> p_344812_.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO
      );
      public static final StreamCodec<ByteBuf, ServerLinks.KnownLinkType> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, p_345122_ -> p_345122_.id);
      private final int id;
      private final String name;

      KnownLinkType(final int p_342655_, final String p_343354_) {
         this.id = p_342655_;
         this.name = p_343354_;
      }

      private Component displayName() {
         return Component.translatable("known_server_link." + this.name);
      }

      public ServerLinks.Entry create(URI p_344425_) {
         return ServerLinks.Entry.knownType(this, p_344425_);
      }
   }

   public record UntrustedEntry(Either<ServerLinks.KnownLinkType, Component> type, String link) {
      public static final StreamCodec<ByteBuf, ServerLinks.UntrustedEntry> STREAM_CODEC = StreamCodec.composite(
         ServerLinks.TYPE_STREAM_CODEC,
         ServerLinks.UntrustedEntry::type,
         ByteBufCodecs.STRING_UTF8,
         ServerLinks.UntrustedEntry::link,
         ServerLinks.UntrustedEntry::new
      );
   }
}
