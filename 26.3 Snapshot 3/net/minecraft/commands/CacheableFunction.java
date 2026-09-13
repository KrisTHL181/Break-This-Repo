package net.minecraft.commands;

import com.mojang.serialization.Codec;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerFunctionManager;

public class CacheableFunction {
   public static final Codec<CacheableFunction> CODEC = Identifier.CODEC.xmap(CacheableFunction::new, CacheableFunction::getId);
   private final Identifier id;
   private boolean resolved;
   private Optional<CommandFunction<CommandSourceStack>> function = Optional.empty();

   public CacheableFunction(final Identifier id) {
      this.id = id;
   }

   public Optional<CommandFunction<CommandSourceStack>> get(final ServerFunctionManager manager) {
      if (!this.resolved) {
         this.function = manager.get(this.id);
         this.resolved = true;
      }

      return this.function;
   }

   public Identifier getId() {
      return this.id;
   }

   @Override
   public boolean equals(final Object obj) {
      return obj == this ? true : obj instanceof CacheableFunction cacheableFunction && this.getId().equals(cacheableFunction.getId());
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.id);
   }
}
