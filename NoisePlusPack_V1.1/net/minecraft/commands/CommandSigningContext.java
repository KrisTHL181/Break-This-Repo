package net.minecraft.commands;

import java.util.Map;
import net.minecraft.network.chat.PlayerChatMessage;
import org.jspecify.annotations.Nullable;

public interface CommandSigningContext {
   CommandSigningContext ANONYMOUS = new CommandSigningContext() {
      @Override
      public @Nullable PlayerChatMessage getArgument(String p_242898_) {
         return null;
      }
   };

   @Nullable PlayerChatMessage getArgument(String var1);

   record SignedArguments(Map<String, PlayerChatMessage> arguments) implements CommandSigningContext {
      @Override
      public @Nullable PlayerChatMessage getArgument(String p_242852_) {
         return this.arguments.get(p_242852_);
      }
   }
}
