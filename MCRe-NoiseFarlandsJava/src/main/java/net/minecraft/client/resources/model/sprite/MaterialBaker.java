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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public abstract class MaterialBaker {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Material.Baked missingSprite;
    private final Material.Baked missingSpriteForceTranslucent;
    private final Multimap<String, Identifier> missingSprites = Multimaps.synchronizedMultimap(HashMultimap.create());
    private final Multimap<String, String> missingReferences = Multimaps.synchronizedMultimap(HashMultimap.create());
    private final Map<Material, Material.@Nullable Baked> bakedMaterials = new ConcurrentHashMap<>();
    private final Function<Material, Material.@Nullable Baked> bakerFunction = this::bake;

    public MaterialBaker(final TextureAtlasSprite missingSprite) {
        this.missingSprite = new Material.Baked(missingSprite, false);
        this.missingSpriteForceTranslucent = new Material.Baked(missingSprite, true);
    }

    public Material.Baked replacementForMissingMaterial(final Material material) {
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

    protected abstract Material.@Nullable Baked bake(Material material);

    protected static Material.@Nullable Baked bakeForAtlas(final Material material, final SpriteLoader.Preparations atlas) {
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