package net.minecraft.client.resources.model;

import net.minecraft.resources.Identifier;

public interface ResolvableModel {
   void resolveDependencies(ResolvableModel.Resolver resolver);

   interface Resolver {
      void markDependency(Identifier id);
   }
}
