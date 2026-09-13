package net.minecraft.world.level.timers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;

public record FunctionTagCallback(Identifier tagId) implements TimerCallback<MinecraftServer> {
   public static final MapCodec<FunctionTagCallback> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Identifier.CODEC.fieldOf("id").forGetter(FunctionTagCallback::tagId)).apply(i, FunctionTagCallback::new)
   );

   public void handle(final MinecraftServer server, final TimerQueue<MinecraftServer> queue, final long time) {
      ServerFunctionManager functionManager = server.getFunctions();

      for (CommandFunction<CommandSourceStack> function : functionManager.getTag(this.tagId)) {
         functionManager.execute(function, functionManager.getGameLoopSender());
      }
   }

   @Override
   public MapCodec<FunctionTagCallback> codec() {
      return CODEC;
   }
}
