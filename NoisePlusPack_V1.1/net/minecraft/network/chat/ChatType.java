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
      p_240514_ -> p_240514_.group(
            ChatTypeDecoration.CODEC.fieldOf("chat").forGetter(ChatType::chat), ChatTypeDecoration.CODEC.fieldOf("narration").forGetter(ChatType::narration)
         )
         .apply(p_240514_, ChatType::new)
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

   private static ResourceKey<ChatType> create(String p_237024_) {
      return ResourceKey.create(Registries.CHAT_TYPE, Identifier.withDefaultNamespace(p_237024_));
   }

   public static void bootstrap(BootstrapContext<ChatType> p_335852_) {
      p_335852_.register(CHAT, new ChatType(DEFAULT_CHAT_DECORATION, ChatTypeDecoration.withSender("chat.type.text.narrate")));
      p_335852_.register(
         SAY_COMMAND, new ChatType(ChatTypeDecoration.withSender("chat.type.announcement"), ChatTypeDecoration.withSender("chat.type.text.narrate"))
      );
      p_335852_.register(
         MSG_COMMAND_INCOMING,
         new ChatType(ChatTypeDecoration.incomingDirectMessage("commands.message.display.incoming"), ChatTypeDecoration.withSender("chat.type.text.narrate"))
      );
      p_335852_.register(
         MSG_COMMAND_OUTGOING,
         new ChatType(ChatTypeDecoration.outgoingDirectMessage("commands.message.display.outgoing"), ChatTypeDecoration.withSender("chat.type.text.narrate"))
      );
      p_335852_.register(
         TEAM_MSG_COMMAND_INCOMING,
         new ChatType(ChatTypeDecoration.teamMessage("chat.type.team.text"), ChatTypeDecoration.withSender("chat.type.text.narrate"))
      );
      p_335852_.register(
         TEAM_MSG_COMMAND_OUTGOING,
         new ChatType(ChatTypeDecoration.teamMessage("chat.type.team.sent"), ChatTypeDecoration.withSender("chat.type.text.narrate"))
      );
      p_335852_.register(EMOTE_COMMAND, new ChatType(ChatTypeDecoration.withSender("chat.type.emote"), ChatTypeDecoration.withSender("chat.type.emote")));
   }

   public static ChatType.Bound bind(ResourceKey<ChatType> p_241279_, Entity p_241483_) {
      return bind(p_241279_, p_241483_.level().registryAccess(), p_241483_.getDisplayName());
   }

   public static ChatType.Bound bind(ResourceKey<ChatType> p_241345_, CommandSourceStack p_241466_) {
      return bind(p_241345_, p_241466_.registryAccess(), p_241466_.getDisplayName());
   }

   public static ChatType.Bound bind(ResourceKey<ChatType> p_241284_, RegistryAccess p_241373_, Component p_241455_) {
      Registry<ChatType> registry = p_241373_.lookupOrThrow(Registries.CHAT_TYPE);
      return new ChatType.Bound(registry.getOrThrow(p_241284_), p_241455_);
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

      Bound(Holder<ChatType> p_334588_, Component p_241447_) {
         this(p_334588_, p_241447_, Optional.empty());
      }

      public Component decorate(Component p_241411_) {
         return this.chatType.value().chat().decorate(p_241411_, this);
      }

      public Component decorateNarration(Component p_241354_) {
         return this.chatType.value().narration().decorate(p_241354_, this);
      }

      public ChatType.Bound withTargetName(Component p_241530_) {
         return new ChatType.Bound(this.chatType, this.name, Optional.of(p_241530_));
      }
   }
}
