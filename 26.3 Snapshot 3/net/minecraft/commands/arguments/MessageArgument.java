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
   private static final Dynamic2CommandExceptionType TOO_LONG = new Dynamic2CommandExceptionType(
      (length, maxLength) -> Component.translatableEscape("argument.message.too_long", length, maxLength)
   );

   public static MessageArgument message() {
      return new MessageArgument();
   }

   public static Component getMessage(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      MessageArgument.Message message = (MessageArgument.Message)context.getArgument(name, MessageArgument.Message.class);
      return message.resolveComponent((CommandSourceStack)context.getSource());
   }

   public static void resolveChatMessage(final CommandContext<CommandSourceStack> context, final String name, final Consumer<PlayerChatMessage> task) throws CommandSyntaxException {
      MessageArgument.Message message = (MessageArgument.Message)context.getArgument(name, MessageArgument.Message.class);
      CommandSourceStack sender = (CommandSourceStack)context.getSource();
      Component formatted = message.resolveComponent(sender);
      CommandSigningContext signingContext = sender.getSigningContext();
      PlayerChatMessage signedArgument = signingContext.getArgument(name);
      if (signedArgument != null) {
         resolveSignedMessage(task, sender, signedArgument.withUnsignedContent(formatted));
      } else {
         resolveDisguisedMessage(task, sender, PlayerChatMessage.system(message.text).withUnsignedContent(formatted));
      }
   }

   private static void resolveSignedMessage(final Consumer<PlayerChatMessage> task, final CommandSourceStack sender, final PlayerChatMessage signedArgument) {
      MinecraftServer server = sender.getServer();
      CompletableFuture<FilteredText> filteredFuture = filterPlainText(sender, signedArgument);
      Component decorated = server.getChatDecorator().decorate(sender.getPlayer(), signedArgument.decoratedContent());
      sender.getChatMessageChainer().append(filteredFuture, filtered -> {
         PlayerChatMessage filteredMessage = signedArgument.withUnsignedContent(decorated).filter(filtered.mask());
         task.accept(filteredMessage);
      });
   }

   private static void resolveDisguisedMessage(final Consumer<PlayerChatMessage> task, final CommandSourceStack sender, final PlayerChatMessage argument) {
      ChatDecorator decorator = sender.getServer().getChatDecorator();
      Component decorated = decorator.decorate(sender.getPlayer(), argument.decoratedContent());
      task.accept(argument.withUnsignedContent(decorated));
   }

   private static CompletableFuture<FilteredText> filterPlainText(final CommandSourceStack sender, final PlayerChatMessage message) {
      ServerPlayer player = sender.getPlayer();
      return player != null && message.hasSignatureFrom(player.getUUID())
         ? player.getTextFilter().processStreamMessage(message.signedContent())
         : CompletableFuture.completedFuture(FilteredText.passThrough(message.signedContent()));
   }

   public MessageArgument.Message parse(final StringReader reader) throws CommandSyntaxException {
      return MessageArgument.Message.parseText(reader, true);
   }

   public <S> MessageArgument.Message parse(final StringReader reader, final @Nullable S source) throws CommandSyntaxException {
      return MessageArgument.Message.parseText(reader, EntitySelectorParser.allowSelectors(source));
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public record Message(String text, MessageArgument.Part[] parts) {
      private Component resolveComponent(final CommandSourceStack sender) throws CommandSyntaxException {
         return this.toComponent(sender, sender.permissions().hasPermission(Permissions.COMMANDS_ENTITY_SELECTORS));
      }

      public Component toComponent(final CommandSourceStack sender, final boolean allowSelectors) throws CommandSyntaxException {
         if (this.parts.length != 0 && allowSelectors) {
            MutableComponent result = Component.literal(this.text.substring(0, this.parts[0].start()));
            int readTo = this.parts[0].start();

            for (MessageArgument.Part part : this.parts) {
               Component component = part.toComponent(sender);
               if (readTo < part.start()) {
                  result.append(this.text.substring(readTo, part.start()));
               }

               result.append(component);
               readTo = part.end();
            }

            if (readTo < this.text.length()) {
               result.append(this.text.substring(readTo));
            }

            return result;
         } else {
            return Component.literal(this.text);
         }
      }

      public static MessageArgument.Message parseText(final StringReader reader, final boolean allowSelectors) throws CommandSyntaxException {
         if (reader.getRemainingLength() > 256) {
            throw MessageArgument.TOO_LONG.create(reader.getRemainingLength(), 256);
         }

         String text = reader.getRemaining();
         if (!allowSelectors) {
            reader.setCursor(reader.getTotalLength());
            return new MessageArgument.Message(text, new MessageArgument.Part[0]);
         }

         List<MessageArgument.Part> result = Lists.newArrayList();
         int offset = reader.getCursor();

         while (true) {
            int start;
            EntitySelector parse;
            while (true) {
               if (!reader.canRead()) {
                  return new MessageArgument.Message(text, result.toArray(new MessageArgument.Part[0]));
               }

               if (reader.peek() == '@') {
                  start = reader.getCursor();

                  try {
                     EntitySelectorParser parser = new EntitySelectorParser(reader, true);
                     parse = parser.parse();
                     break;
                  } catch (CommandSyntaxException ex) {
                     if (ex.getType() != EntitySelectorParser.ERROR_MISSING_SELECTOR_TYPE && ex.getType() != EntitySelectorParser.ERROR_UNKNOWN_SELECTOR_TYPE) {
                        throw ex;
                     }

                     reader.setCursor(start + 1);
                  }
               } else {
                  reader.skip();
               }
            }

            result.add(new MessageArgument.Part(start - offset, reader.getCursor() - offset, parse));
         }
      }
   }

   public record Part(int start, int end, EntitySelector selector) {
      public Component toComponent(final CommandSourceStack sender) throws CommandSyntaxException {
         return EntitySelector.joinNames(this.selector.findEntities(sender));
      }
   }
}
