package net.minecraft.client.resources.model;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public interface ModelBaker {
    ResolvedModel getModel(Identifier location);

    BlockStateModelPart missingBlockModelPart();

    MaterialBaker materials();

    ModelBaker.Interner interner();

    <T> T compute(ModelBaker.SharedOperationKey<T> key);

    @OnlyIn(Dist.CLIENT)
    interface Interner {
        Vector3fc vector(Vector3fc vector);

        BakedQuad.MaterialInfo materialInfo(BakedQuad.MaterialInfo material);
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    interface SharedOperationKey<T> {
        T compute(ModelBaker modelBakery);
    }
}