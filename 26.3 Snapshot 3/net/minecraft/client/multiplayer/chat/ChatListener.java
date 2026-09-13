package net.minecraft.client.multiplayer.chat;

import com.google.common.collect.Queues;
import com.mojang.authlib.GameProfile;
import java.time.Instant;
import java.util.Deque;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.util.StringDecomposer;
import net.minecraft.util.Util;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

public class ChatListener {
   private static final Component CHAT_VALIDATION_ERROR = Component.translatable("chat.validation_error").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC);
   private final Minecraft minecraft;
   private final Deque<ChatListener.Message> delayedMessageQueue = Queues.newArrayDeque();
   private long messageDelay;
   private long previousMessageTime;

   public ChatListener(final Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   public void tick() {
      if (this.minecraft.isPaused()) {
         if (this.messageDelay > 0L) {
            this.previousMessageTime += 50L;
         }
      } else {
         if (this.messageDelay == 0L) {
            if (!this.delayedMessageQueue.isEmpty()) {
               this.flushQueue();
            }
         } else {
            ChatListener.Message message;
            if (Util.getMillis() >= this.previousMessageTime + this.messageDelay) {
               do {
                  message = this.delayedMessageQueue.poll();
               } while (message != null && !message.accept());
            }
         }
      }
   }

   public void setMessageDelay(final double messageDelaySeconds) {
      long messageDelay = (long)(messageDelaySeconds * 1000.0);
      if (messageDelay == 0L && this.messageDelay > 0L && !this.minecraft.isPaused()) {
         this.flushQueue();
      }

      this.messageDelay = messageDelay;
   }

   public void acceptNextDelayedMessage() {
      this.delayedMessageQueue.remove().accept();
   }

   public long queueSize() {
      return this.delayedMessageQueue.size();
   }

   public void flushQueue() {
      this.delayedMessageQueue.forEach(ChatListener.Message::accept);
      this.delayedMessageQueue.clear();
      this.previousMessageTime = 0L;
   }

   public boolean removeFromDelayedMessageQueue(final MessageSignature signature) {
      return this.delayedMessageQueue.removeIf(message -> signature.equals(message.signature()));
   }

   private boolean willDelayMessages() {
      return this.messageDelay > 0L && Util.getMillis() < this.previousMessageTime + this.messageDelay;
   }

   private void handleMessage(final @Nullable MessageSignature signature, final BooleanSupplier handler) {
      if (this.willDelayMessages()) {
         this.delayedMessageQueue.add(new ChatListener.Message(signature, handler));
      } else {
         handler.getAsBoolean();
      }
   }

   public void handlePlayerChatMessage(final PlayerChatMessage message, final GameProfile sender, final ChatType.Bound boundChatType) {
      boolean onlyShowSecure = this.minecraft.options.onlyShowSecureChat().get();
      PlayerChatMessage displayedMessage = onlyShowSecure ? message.removeUnsignedContent() : message;
      Component decoratedMessage = boundChatType.decorate(displayedMessage.decoratedContent());
      Instant received = Instant.now();
      this.handleMessage(message.signature(), () -> {
         boolean wasShown = this.showMessageToPlayer(boundChatType, message, decoratedMessage, sender, onlyShowSecure, received);
         ClientPacketListener connection = this.minecraft.getConnection();
         if (connection != null && message.signature() != null) {
            connection.markMessageAsProcessed(message.signature(), wasShown);
         }

         return wasShown;
      });
   }

   public void handleChatMessageError(final UUID senderId, final @Nullable MessageSignature invalidSignature, final ChatType.Bound boundChatType) {
      this.handleMessage(null, () -> {
         ClientPacketListener connection = this.minecraft.getConnection();
         if (connection != null && invalidSignature != null) {
            connection.markMessageAsProcessed(invalidSignature, false);
         }

         if (this.minecraft.isBlocked(senderId)) {
            return false;
         } else if (this.minecraft.isFriendOnlyRestricted(senderId)) {
            return false;
         } else {
            LocalPlayer receiver = this.minecraft.player;
            if (receiver != null && receiver.chatAbilities().canReceivePlayerMessages()) {
               Component decoratedMessage = boundChatType.decorate(CHAT_VALIDATION_ERROR);
               this.minecraft.gui.hud.getChat().addPlayerMessage(decoratedMessage, null, GuiMessageTag.chatError());
               this.minecraft.getNarrator().saySystemChatQueued(boundChatType.decorateNarration(CHAT_VALIDATION_ERROR));
               this.previousMessageTime = Util.getMillis();
               return true;
            } else {
               return false;
            }
         }
      });
   }

   public void handleDisguisedChatMessage(final Component message, final ChatType.Bound boundChatType) {
      Instant received = Instant.now();
      this.handleMessage(null, () -> {
         LocalPlayer receiver = this.minecraft.player;
         if (receiver != null && receiver.chatAbilities().canReceivePlayerMessages()) {
            Component decoratedMessage = boundChatType.decorate(message);
            this.minecraft.gui.hud.getChat().addPlayerMessage(decoratedMessage, null, GuiMessageTag.system());
            this.narrateChatMessage(boundChatType, message);
            this.logSystemMessage(decoratedMessage, received);
            this.previousMessageTime = Util.getMillis();
            return true;
         } else {
            return false;
         }
      });
   }

   private boolean showMessageToPlayer(
      final ChatType.Bound boundChatType,
      final PlayerChatMessage message,
      final Component decoratedMessage,
      final GameProfile sender,
      final boolean onlyShowSecure,
      final Instant received
   ) {
      ChatTrustLevel trustLevel = this.evaluateTrustLevel(message, decoratedMessage, received);
      if (onlyShowSecure && trustLevel.isNotSecure()) {
         return false;
      }

      if (!this.minecraft.isBlocked(message.sender()) && !message.isFullyFiltered()) {
         if (this.minecraft.isFriendOnlyRestricted(message.sender())) {
            return false;
         }

         LocalPlayer receiver = this.minecraft.player;
         if (receiver != null && receiver.chatAbilities().canReceivePlayerMessages()) {
            GuiMessageTag tag = trustLevel.createTag(message);
            MessageSignature signature = message.signature();
            FilterMask filterMask = message.filterMask();
            if (filterMask.isEmpty()) {
               this.minecraft.gui.hud.getChat().addPlayerMessage(decoratedMessage, signature, tag);
               this.narrateChatMessage(boundChatType, message.decoratedContent());
            } else {
               Component filteredContent = filterMask.applyWithFormatting(message.signedContent());
               if (filteredContent != null) {
                  this.minecraft.gui.hud.getChat().addPlayerMessage(boundChatType.decorate(filteredContent), signature, tag);
                  this.narrateChatMessage(boundChatType, filteredContent);
               }
            }

            this.logPlayerMessage(message, sender, trustLevel);
            this.previousMessageTime = Util.getMillis();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void narrateChatMessage(final ChatType.Bound boundChatType, final Component content) {
      this.minecraft.getNarrator().sayChatQueued(boundChatType.decorateNarration(content));
   }

   private ChatTrustLevel evaluateTrustLevel(final PlayerChatMessage message, final Component decoratedMessage, final Instant received) {
      return this.isSenderLocalPlayer(message.sender()) ? ChatTrustLevel.SECURE : ChatTrustLevel.evaluate(message, decoratedMessage, received);
   }

   private void logPlayerMessage(final PlayerChatMessage message, final GameProfile sender, final ChatTrustLevel trustLevel) {
      ChatLog chatLog = this.minecraft.getReportingContext().chatLog();
      chatLog.push(LoggedChatMessage.player(sender, message, trustLevel));
   }

   private void logSystemMessage(final Component message, final Instant timeStamp) {
      ChatLog chatLog = this.minecraft.getReportingContext().chatLog();
      chatLog.push(LoggedChatMessage.system(message, timeStamp));
   }

   public void handleSystemMessage(final Component message, final boolean remote) {
      UUID guessedUUID = this.guessChatUUID(message);
      if (!this.minecraft.options.hideMatchedNames().get() || !this.minecraft.isBlocked(guessedUUID)) {
         if (guessedUUID == Util.NIL_UUID || !this.minecraft.isFriendOnlyRestricted(guessedUUID)) {
            LocalPlayer receiver = this.minecraft.player;
            if (receiver != null && receiver.chatAbilities().canReceiveSystemMessages()) {
               if (remote) {
                  this.minecraft.gui.hud.getChat().addServerSystemMessage(message);
                  this.logSystemMessage(message, Instant.now());
               } else {
                  this.minecraft.gui.hud.getChat().addClientSystemMessage(message);
               }

               this.minecraft.getNarrator().saySystemChatQueued(message);
            }
         }
      }
   }

   public void handleOverlay(final Component message) {
      this.minecraft.gui.hud.setOverlayMessage(message, false);
      this.minecraft.getNarrator().saySystemQueued(message);
   }

   private UUID guessChatUUID(final Component message) {
      String noFormatMessage = StringDecomposer.getPlainText(message);
      String possibleMention = StringUtils.substringBetween(noFormatMessage, "<", ">");
      return possibleMention == null ? Util.NIL_UUID : this.minecraft.getPlayerSocialManager().getDiscoveredUUID(possibleMention);
   }

   private boolean isSenderLocalPlayer(final UUID senderProfileId) {
      if (this.minecraft.isLocalServer() && this.minecraft.player != null) {
         UUID localProfileId = this.minecraft.player.getGameProfile().id();
         return localProfileId.equals(senderProfileId);
      } else {
         return false;
      }
   }

   private record Message(@Nullable MessageSignature signature, BooleanSupplier handler) {
      public boolean accept() {
         return this.handler.getAsBoolean();
      }
   }
}
