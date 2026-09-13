package net.minecraft.commands;

import java.util.Map;
import net.minecraft.network.chat.PlayerChatMessage;
import org.jspecify.annotations.Nullable;

public interface CommandSigningContext {
   CommandSigningContext ANONYMOUS = new CommandSigningContext() {
      @Override
      public @Nullable PlayerChatMessage getArgument(final String name) {
         return null;
      }
   };

   @Nullable PlayerChatMessage getArgument(String name);

   record SignedArguments(Map<String, PlayerChatMessage> arguments) implements CommandSigningContext {
      @Override
      public @Nullable PlayerChatMessage getArgument(final String name) {
         return this.arguments.get(name);
      }
   }
}
