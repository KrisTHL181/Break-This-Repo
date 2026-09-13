package net.minecraft.client.resources.model;

import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ResolvableModel {
    void resolveDependencies(ResolvableModel.Resolver resolver);

    @OnlyIn(Dist.CLIENT)
    interface Resolver {
        void markDependency(Identifier id);
    }
}