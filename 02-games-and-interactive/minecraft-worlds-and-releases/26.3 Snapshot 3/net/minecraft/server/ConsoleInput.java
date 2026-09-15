package net.minecraft.server;

import net.minecraft.commands.CommandSourceStack;

public class ConsoleInput {
   public final String msg;
   public final CommandSourceStack source;

   public ConsoleInput(final String msg, final CommandSourceStack source) {
      this.msg = msg;
      this.source = source;
   }
}
