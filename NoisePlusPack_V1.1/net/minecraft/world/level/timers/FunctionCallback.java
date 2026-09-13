package net.minecraft.world.level.timers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;

public record FunctionCallback(Identifier functionId) implements TimerCallback<MinecraftServer> {
   public static final MapCodec<FunctionCallback> CODEC = RecordCodecBuilder.mapCodec(
      p_450122_ -> p_450122_.group(Identifier.CODEC.fieldOf("Name").forGetter(FunctionCallback::functionId)).apply(p_450122_, FunctionCallback::new)
   );

   public void handle(MinecraftServer p_82172_, TimerQueue<MinecraftServer> p_82173_, long p_82174_) {
      ServerFunctionManager serverfunctionmanager = p_82172_.getFunctions();
      serverfunctionmanager.get(this.functionId)
         .ifPresent(p_309355_ -> serverfunctionmanager.execute((CommandFunction<CommandSourceStack>)p_309355_, serverfunctionmanager.getGameLoopSender()));
   }

   @Override
   public MapCodec<FunctionCallback> codec() {
      return CODEC;
   }
}
