package net.minecraft.world.level.timers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ExtraCodecs;

public class TimerCallbacks<C> {
   public static final TimerCallbacks<MinecraftServer> SERVER_CALLBACKS = new TimerCallbacks<MinecraftServer>()
      .register(Identifier.withDefaultNamespace("function"), FunctionCallback.CODEC)
      .register(Identifier.withDefaultNamespace("function_tag"), FunctionTagCallback.CODEC);
   private final ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends TimerCallback<C>>> idMapper = new ExtraCodecs.LateBoundIdMapper<>();
   private final Codec<TimerCallback<C>> codec = this.idMapper.codec(Identifier.CODEC).dispatch("Type", TimerCallback::codec, Function.identity());

   public TimerCallbacks<C> register(Identifier p_452177_, MapCodec<? extends TimerCallback<C>> p_393545_) {
      this.idMapper.put(p_452177_, p_393545_);
      return this;
   }

   public Codec<TimerCallback<C>> codec() {
      return this.codec;
   }
}
