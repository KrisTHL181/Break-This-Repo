package net.minecraft.client.resources.model.sprite;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.mojang.logging.LogUtils;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class MaterialBaker {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final SpriteLoader.Preparations blockAtlas;
   private final SpriteLoader.Preparations itemAtlas;
   private final Material.Baked missingSprite;
   private final Material.Baked missingSpriteForceTranslucent;
   private final Multimap<String, Identifier> missingSprites = Multimaps.synchronizedMultimap(HashMultimap.create());
   private final Multimap<String, String> missingReferences = Multimaps.synchronizedMultimap(HashMultimap.create());
   private final Map<Material, Material.@Nullable Baked> bakedMaterials = new ConcurrentHashMap<>();
   private final Function<Material, Material.@Nullable Baked> bakerFunction = this::bake;

   public MaterialBaker(final SpriteLoader.Preparations blockAtlas, final SpriteLoader.Preparations itemAtlas) {
      this.blockAtlas = blockAtlas;
      this.itemAtlas = itemAtlas;
      this.missingSprite = new Material.Baked(blockAtlas.missing(), false);
      this.missingSpriteForceTranslucent = new Material.Baked(blockAtlas.missing(), true);
   }

   private Material.Baked replacementForMissingMaterial(final Material material) {
      return material.forceTranslucent() ? this.missingSpriteForceTranslucent : this.missingSprite;
   }

   public Material.Baked get(final Material material, final ModelDebugName name) {
      if (material.sprite().equals(MissingTextureAtlasSprite.getLocation())) {
         return this.replacementForMissingMaterial(material);
      } else {
         Material.Baked baked = this.bakedMaterials.computeIfAbsent(material, this.bakerFunction);
         if (baked == null) {
            this.missingSprites.put(name.debugName(), material.sprite());
            return this.replacementForMissingMaterial(material);
         } else {
            return baked;
         }
      }
   }

   private Material.@Nullable Baked bake(final Material material) {
      Material.Baked itemMaterial = bakeForAtlas(material, this.itemAtlas);
      return itemMaterial != null ? itemMaterial : bakeForAtlas(material, this.blockAtlas);
   }

   private static Material.@Nullable Baked bakeForAtlas(final Material material, final SpriteLoader.Preparations atlas) {
      TextureAtlasSprite sprite = atlas.getSprite(material.sprite());
      return sprite != null ? new Material.Baked(sprite, material.forceTranslucent()) : null;
   }

   public Material.Baked resolveSlot(final TextureSlots slots, final String id, final ModelDebugName name) {
      Material resolvedMaterial = slots.getMaterial(id);
      return resolvedMaterial != null ? this.get(resolvedMaterial, name) : this.reportMissingReference(id, name);
   }

   public Material.Baked reportMissingReference(final String reference, final ModelDebugName responsibleModel) {
      this.missingReferences.put(responsibleModel.debugName(), reference);
      return this.missingSprite;
   }

   public void logMissingTextures() {
      this.missingSprites
         .asMap()
         .forEach(
            (location, sprites) -> LOGGER.warn(
               "Missing textures in model {}:\n{}", location, sprites.stream().sorted().map(sprite -> "    " + sprite).collect(Collectors.joining("\n"))
            )
         );
      this.missingReferences
         .asMap()
         .forEach(
            (location, references) -> LOGGER.warn(
               "Missing texture references in model {}:\n{}",
               location,
               references.stream().sorted().map(reference -> "    " + reference).collect(Collectors.joining("\n"))
            )
         );
   }
}
