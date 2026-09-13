package net.minecraft.client.renderer.block.dispatch;

import java.util.List;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

public interface BlockStateModelPart {
   List<BakedQuad> getQuads(@Nullable Direction direction);

   boolean useAmbientOcclusion();

   Material.Baked particleMaterial();

   @BakedQuad.MaterialFlags int materialFlags();

   interface Unbaked extends ResolvableModel {
      BlockStateModelPart bake(ModelBaker modelBakery);
   }
}
