package net.minecraft.commands.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSigningContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.permissions.Permissions;
import org.jspecify.annotations.Nullable;

public class MessageArgument implements SignedArgument<MessageArgument.Message> {
   private static final Collection<String> EXAMPLES = Arrays.asList("Hello world!", "foo", "@e", "Hello @p :)");
   static final Dynamic2CommandExceptionType TOO_LONG = new Dynamic2CommandExceptionType(
      (p_325588_, p_325589_) -> Component.translatableEscape("argument.message.too_long", p_325588_, p_325589_)
   );

   public static MessageArgument message() {
      return new MessageArgument();
   }

   public static Component getMessage(CommandContext<CommandSourceStack> p_96836_, String p_96837_) throws CommandSyntaxException {
      MessageArgument.Message messageargument$message = (MessageArgument.Message)p_96836_.getArgument(p_96837_, MessageArgument.Message.class);
      return messageargument$message.resolveComponent((CommandSourceStack)p_96836_.getSource());
   }

   public static void resolveChatMessage(CommandContext<CommandSourceStack> p_249433_, String p_248718_, Consumer<PlayerChatMessage> p_249460_) throws CommandSyntaxException {
      MessageArgument.Message messageargument$message = (MessageArgument.Message)p_249433_.getArgument(p_248718_, MessageArgument.Message.class);
      CommandSourceStack commandsourcestack = (CommandSourceStack)p_249433_.getSource();
      Component component = messageargument$message.resolveComponent(commandsourcestack);
      CommandSigningContext commandsigningcontext = commandsourcestack.getSigningContext();
      PlayerChatMessage playerchatmessage = commandsigningcontext.getArgument(p_248718_);
      if (playerchatmessage != null) {
         resolveSignedMessage(p_249460_, commandsourcestack, playerchatmessage.withUnsignedContent(component));
      } else {
         resolveDisguisedMessage(p_249460_, commandsourcestack, PlayerChatMessage.system(messageargument$message.text).withUnsignedContent(component));
      }
   }

   private static void resolveSignedMessage(Consumer<PlayerChatMessage> p_250000_, CommandSourceStack p_252335_, PlayerChatMessage p_249420_) {
      MinecraftServer minecraftserver = p_252335_.getServer();
      CompletableFuture<FilteredText> completablefuture = filterPlainText(p_252335_, p_249420_);
      Component component = minecraftserver.getChatDecorator().decorate(p_252335_.getPlayer(), p_249420_.decoratedContent());
      p_252335_.getChatMessageChainer().append(completablefuture, p_296325_ -> {
         PlayerChatMessage playerchatmessage = p_249420_.withUnsignedContent(component).filter(p_296325_.mask());
         p_250000_.accept(playerchatmessage);
      });
   }

   private static void resolveDisguisedMessage(Consumer<PlayerChatMessage> p_249162_, CommandSourceStack p_248759_, PlayerChatMessage p_252332_) {
      ChatDecorator chatdecorator = p_248759_.getServer().getChatDecorator();
      Component component = chatdecorator.decorate(p_248759_.getPlayer(), p_252332_.decoratedContent());
      p_249162_.accept(p_252332_.withUnsignedContent(component));
   }

   private static CompletableFuture<FilteredText> filterPlainText(CommandSourceStack p_252063_, PlayerChatMessage p_251184_) {
      ServerPlayer serverplayer = p_252063_.getPlayer();
      return serverplayer != null && p_251184_.hasSignatureFrom(serverplayer.getUUID())
         ? serverplayer.getTextFilter().processStreamMessage(p_251184_.signedContent())
         : CompletableFuture.completedFuture(FilteredText.passThrough(p_251184_.signedContent()));
   }

   public MessageArgument.Message parse(StringReader p_96834_) throws CommandSyntaxException {
      return MessageArgument.Message.parseText(p_96834_, true);
   }

   public <S> MessageArgument.Message parse(StringReader p_345550_, @Nullable S p_345556_) throws CommandSyntaxException {
      return MessageArgument.Message.parseText(p_345550_, EntitySelectorParser.allowSelectors(p_345556_));
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public record Message(String text, MessageArgument.Part[] parts) {
      Component resolveComponent(CommandSourceStack p_232197_) throws CommandSyntaxException {
         return this.toComponent(p_232197_, p_232197_.permissions().hasPermission(Permissions.COMMANDS_ENTITY_SELECTORS));
      }

      public Component toComponent(CommandSourceStack p_96850_, boolean p_96851_) throws CommandSyntaxException {
         if (this.parts.length != 0 && p_96851_) {
            MutableComponent mutablecomponent = Component.literal(this.text.substring(0, this.parts[0].start()));
            int i = this.parts[0].start();

            for (MessageArgument.Part messageargument$part : this.parts) {
               Component component = messageargument$part.toComponent(p_96850_);
               if (i < messageargument$part.start()) {
                  mutablecomponent.append(this.text.substring(i, messageargument$part.start()));
               }

               mutablecomponent.append(component);
               i = messageargument$part.end();
            }

            if (i < this.text.length()) {
               mutablecomponent.append(this.text.substring(i));
            }

            return mutablecomponent;
         } else {
            return Component.literal(this.text);
         }
      }

      public static MessageArgument.Message parseText(StringReader p_96847_, boolean p_96848_) throws CommandSyntaxException {
         if (p_96847_.getRemainingLength() > 256) {
            throw MessageArgument.TOO_LONG.create(p_96847_.getRemainingLength(), 256);
         }

         String s = p_96847_.getRemaining();
         if (!p_96848_) {
            p_96847_.setCursor(p_96847_.getTotalLength());
            return new MessageArgument.Message(s, new MessageArgument.Part[0]);
         }

         List<MessageArgument.Part> list = Lists.newArrayList();
         int i = p_96847_.getCursor();

         while (true) {
            int j;
            EntitySelector entityselector;
            while (true) {
               if (!p_96847_.canRead()) {
                  return new MessageArgument.Message(s, list.toArray(new MessageArgument.Part[0]));
               }

               if (p_96847_.peek() == '@') {
                  j = p_96847_.getCursor();

                  try {
                     EntitySelectorParser entityselectorparser = new EntitySelectorParser(p_96847_, true);
                     entityselector = entityselectorparser.parse();
                     break;
                  } catch (CommandSyntaxException commandsyntaxexception) {
                     if (commandsyntaxexception.getType() != EntitySelectorParser.ERROR_MISSING_SELECTOR_TYPE
                        && commandsyntaxexception.getType() != EntitySelectorParser.ERROR_UNKNOWN_SELECTOR_TYPE) {
                        throw commandsyntaxexception;
                     }

                     p_96847_.setCursor(j + 1);
                  }
               } else {
                  p_96847_.skip();
               }
            }

            list.add(new MessageArgument.Part(j - i, p_96847_.getCursor() - i, entityselector));
         }
      }
   }

   public record Part(int start, int end, EntitySelector selector) {
      public Component toComponent(CommandSourceStack p_96861_) throws CommandSyntaxException {
         return EntitySelector.joinNames(this.selector.findEntities(p_96861_));
      }
   }
}
