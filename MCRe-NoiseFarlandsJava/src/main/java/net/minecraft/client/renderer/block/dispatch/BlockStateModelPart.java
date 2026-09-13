package net.minecraft.client.renderer.block.dispatch;

import java.util.List;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface BlockStateModelPart {
    List<BakedQuad> getQuads(@Nullable Direction direction);

    boolean useAmbientOcclusion();

    Material.Baked particleMaterial();

    @BakedQuad.MaterialFlags int materialFlags();

    @OnlyIn(Dist.CLIENT)
    interface Unbaked extends ResolvableModel {
        BlockStateModelPart bake(ModelBaker modelBakery);
    }
}