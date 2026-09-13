package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.platform.Transparency;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record FluidModel(
    ChunkSectionLayer layer,
    Material.Baked stillMaterial,
    Material.Baked flowingMaterial,
    Material.@Nullable Baked overlayMaterial,
    @Nullable BlockTintSource tintSource
) {
    @OnlyIn(Dist.CLIENT)
    public record Unbaked(Material stillMaterial, Material flowingMaterial, @Nullable Material overlayMaterial, @Nullable BlockTintSource tintSource) {
        public FluidModel bake(final MaterialBaker materials, final ModelDebugName modelName) {
            Material.Baked stillMaterial = materials.get(this.stillMaterial, modelName);
            Material.Baked flowingMaterial = materials.get(this.flowingMaterial, modelName);
            Material.Baked overlayMaterial = this.overlayMaterial != null ? materials.get(this.overlayMaterial, modelName) : null;
            Transparency transparency = getTransparency(stillMaterial).or(getTransparency(flowingMaterial));
            if (overlayMaterial != null) {
                transparency = transparency.or(getTransparency(overlayMaterial));
            }

            return new FluidModel(ChunkSectionLayer.byTransparency(transparency), stillMaterial, flowingMaterial, overlayMaterial, this.tintSource);
        }

        private static Transparency getTransparency(final Material.Baked material) {
            return material.forceTranslucent() ? Transparency.TRANSLUCENT : material.sprite().transparency();
        }
    }
}