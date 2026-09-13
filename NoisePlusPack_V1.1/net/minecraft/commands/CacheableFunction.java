package net.minecraft.commands;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerFunctionManager;

public class CacheableFunction {
   public static final Codec<CacheableFunction> CODEC = Identifier.CODEC.xmap(CacheableFunction::new, CacheableFunction::getId);
   private final Identifier id;
   private boolean resolved;
   private Optional<CommandFunction<CommandSourceStack>> function = Optional.empty();

   public CacheableFunction(Identifier p_460517_) {
      this.id = p_460517_;
   }

   public Optional<CommandFunction<CommandSourceStack>> get(ServerFunctionManager p_310125_) {
      if (!this.resolved) {
         this.function = p_310125_.get(this.id);
         this.resolved = true;
      }

      return this.function;
   }

   public Identifier getId() {
      return this.id;
   }

   @Override
   public boolean equals(Object p_313210_) {
      return p_313210_ == this ? true : p_313210_ instanceof CacheableFunction cacheablefunction && this.getId().equals(cacheablefunction.getId());
   }
}
