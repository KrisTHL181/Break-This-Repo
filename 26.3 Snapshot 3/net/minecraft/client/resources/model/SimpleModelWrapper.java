package net.minecraft.client.resources.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public record SimpleModelWrapper(QuadCollection quads, boolean useAmbientOcclusion, Material.Baked particleMaterial) implements BlockStateModelPart {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static BlockStateModelPart bake(final ModelBaker modelBakery, final Identifier location, final ModelState state) {
      ResolvedModel model = modelBakery.getModel(location);
      TextureSlots textureSlots = model.getTopTextureSlots();
      boolean hasAmbientOcclusion = model.getTopAmbientOcclusion();
      Material.Baked particleMaterial = model.resolveParticleMaterial(textureSlots, modelBakery);
      QuadCollection geometry = model.bakeTopGeometry(textureSlots, modelBakery, state);
      Multimap<Identifier, Identifier> forbiddenSprites = findNonBlockSprites(geometry);
      if (forbiddenSprites != null) {
         LOGGER.warn("Rejecting block model {}, since it contains sprites from outside of supported atlas: {}", location, forbiddenSprites);
         return modelBakery.missingBlockModelPart();
      } else {
         return new SimpleModelWrapper(geometry, hasAmbientOcclusion, particleMaterial);
      }
   }

   public static @Nullable Multimap<Identifier, Identifier> findNonBlockSprites(final QuadCollection geometry) {
      Multimap<Identifier, Identifier> forbiddenSprites = null;

      for (BakedQuad bakedQuad : geometry.getAll()) {
         TextureAtlasSprite sprite = bakedQuad.materialInfo().sprite();
         if (!sprite.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)) {
            if (forbiddenSprites == null) {
               forbiddenSprites = HashMultimap.create();
            }

            forbiddenSprites.put(sprite.atlasLocation(), sprite.contents().name());
         }
      }

      return forbiddenSprites;
   }

   @Override
   public List<BakedQuad> getQuads(final @Nullable Direction direction) {
      return this.quads.getQuads(direction);
   }

   @Override
   public int materialFlags() {
      return this.quads.materialFlags();
   }
}
