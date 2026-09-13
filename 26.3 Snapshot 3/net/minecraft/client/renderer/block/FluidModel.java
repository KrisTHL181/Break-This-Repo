package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.platform.Transparency;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import org.jspecify.annotations.Nullable;

public record FluidModel(
   ChunkSectionLayer layer,
   Material.Baked stillMaterial,
   Material.Baked flowingMaterial,
   Material.@Nullable Baked overlayMaterial,
   @Nullable BlockTintSource tintSource
) {
   public record Unbaked(Material stillMaterial, Material flowingMaterial, @Nullable Material overlayMaterial, @Nullable BlockTintSource tintSource) {
      public FluidModel bake(final MaterialBaker materials, final ModelDebugName modelName) {
         Material.Baked stillMaterial = this.getAndValidateMaterial(this.stillMaterial, materials, "still", modelName);
         Material.Baked flowingMaterial = this.getAndValidateMaterial(this.flowingMaterial, materials, "flowing", modelName);
         Material.Baked overlayMaterial = this.overlayMaterial != null
            ? this.getAndValidateMaterial(this.overlayMaterial, materials, "overlay", modelName)
            : null;
         Transparency transparency = getTransparency(stillMaterial).or(getTransparency(flowingMaterial));
         if (overlayMaterial != null) {
            transparency = transparency.or(getTransparency(overlayMaterial));
         }

         return new FluidModel(ChunkSectionLayer.byTransparency(transparency), stillMaterial, flowingMaterial, overlayMaterial, this.tintSource);
      }

      private Material.Baked getAndValidateMaterial(
         final Material material, final MaterialBaker materials, final String textureName, final ModelDebugName modelName
      ) {
         Material.Baked baked = materials.get(material, modelName);
         return !baked.sprite().atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS) ? materials.reportMissingReference(textureName, modelName) : baked;
      }

      private static Transparency getTransparency(final Material.Baked material) {
         return material.forceTranslucent() ? Transparency.TRANSLUCENT : material.sprite().transparency();
      }
   }
}
