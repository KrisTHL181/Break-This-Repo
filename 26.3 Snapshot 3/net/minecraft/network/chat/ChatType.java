package net.minecraft.network.chat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;

public record ChatType(ChatTypeDecoration chat, ChatTypeDecoration narration) {
   public static final Codec<ChatType> DIRECT_CODEC = RecordCodecBuilder.create(
      i -> i.group(
            ChatTypeDecoration.CODEC.fieldOf("chat").forGetter(ChatType::chat), ChatTypeDecoration.CODEC.fieldOf("narration").forGetter(ChatType::narration)
         )
         .apply(i, ChatType::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ChatType> DIRECT_STREAM_CODEC = StreamCodec.composite(
      ChatTypeDecoration.STREAM_CODEC, ChatType::chat, ChatTypeDecoration.STREAM_CODEC, ChatType::narration, ChatType::new
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<ChatType>> STREAM_CODEC = ByteBufCodecs.holder(Registries.CHAT_TYPE, DIRECT_STREAM_CODEC);
   public static final ChatTypeDecoration DEFAULT_CHAT_DECORATION = ChatTypeDecoration.withSender("chat.type.text");
   public static final ResourceKey<ChatType> CHAT = create("chat");
   public static final ResourceKey<ChatType> SAY_COMMAND = create("say_command");
   public static final ResourceKey<ChatType> MSG_COMMAND_INCOMING = create("msg_command_incoming");
   public static final ResourceKey<ChatType> MSG_COMMAND_OUTGOING = create("msg_command_outgoing");
   public static final ResourceKey<ChatType> TEAM_MSG_COMMAND_INCOMING = create("team_msg_command_incoming");
   public static final ResourceKey<ChatType> TEAM_MSG_COMMAND_OUTGOING = create("team_msg_command_outgoing");
   public static final ResourceKey<ChatType> EMOTE_COMMAND = create("emote_command");

   private static ResourceKey<ChatType> create(final String name) {
      return ResourceKey.create(Registries.CHAT_TYPE, Identifier.withDefaultNamespace(name));
   }

   public static void bootstrap(final BootstrapContext<ChatType> context) {
      context.register(CHAT, new ChatType(DEFAULT_CHAT_DECORATION, ChatTypeDecoration.withSender("chat.type.text.narrate")));
      context.register(
         SAY_COMMAND, new ChatType(ChatTypeDecoration.withSender("chat.type.announcement"), ChatTypeDecoration.withSender("chat.type.text.narrate"))
      );
      context.register(
         MSG_COMMAND_INCOMING,
         new ChatType(ChatTypeDecoration.incomingDirectMessage("commands.message.display.incoming"), ChatTypeDecoration.withSender("chat.type.text.narrate"))
      );
      context.register(
         MSG_COMMAND_OUTGOING,
         new ChatType(ChatTypeDecoration.outgoingDirectMessage("commands.message.display.outgoing"), ChatTypeDecoration.withSender("chat.type.text.narrate"))
      );
      context.register(
         TEAM_MSG_COMMAND_INCOMING,
         new ChatType(ChatTypeDecoration.teamMessage("chat.type.team.text"), ChatTypeDecoration.withSender("chat.type.text.narrate"))
      );
      context.register(
         TEAM_MSG_COMMAND_OUTGOING,
         new ChatType(ChatTypeDecoration.teamMessage("chat.type.team.sent"), ChatTypeDecoration.withSender("chat.type.text.narrate"))
      );
      context.register(EMOTE_COMMAND, new ChatType(ChatTypeDecoration.withSender("chat.type.emote"), ChatTypeDecoration.withSender("chat.type.emote")));
   }

   public static ChatType.Bound bind(final ResourceKey<ChatType> chatType, final Entity entity) {
      return bind(chatType, entity.level().registryAccess(), entity.getDisplayName());
   }

   public static ChatType.Bound bind(final ResourceKey<ChatType> chatType, final CommandSourceStack source) {
      return bind(chatType, source.registryAccess(), source.getDisplayName());
   }

   public static ChatType.Bound bind(final ResourceKey<ChatType> chatType, final RegistryAccess registryAccess, final Component name) {
      Registry<ChatType> registry = registryAccess.lookupOrThrow(Registries.CHAT_TYPE);
      return new ChatType.Bound(registry.getOrThrow(chatType), name);
   }

   public record Bound(Holder<ChatType> chatType, Component name, Optional<Component> targetName) {
      public static final StreamCodec<RegistryFriendlyByteBuf, ChatType.Bound> STREAM_CODEC = StreamCodec.composite(
         ChatType.STREAM_CODEC,
         ChatType.Bound::chatType,
         ComponentSerialization.TRUSTED_STREAM_CODEC,
         ChatType.Bound::name,
         ComponentSerialization.TRUSTED_OPTIONAL_STREAM_CODEC,
         ChatType.Bound::targetName,
         ChatType.Bound::new
      );

      private Bound(final Holder<ChatType> chatType, final Component name) {
         this(chatType, name, Optional.empty());
      }

      public Component decorate(final Component content) {
         return this.chatType.value().chat().decorate(content, this);
      }

      public Component decorateNarration(final Component content) {
         return this.chatType.value().narration().decorate(content, this);
      }

      public ChatType.Bound withTargetName(final Component targetName) {
         return new ChatType.Bound(this.chatType, this.name, Optional.of(targetName));
      }
   }
}
