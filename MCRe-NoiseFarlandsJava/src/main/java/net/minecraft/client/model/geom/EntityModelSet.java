package net.minecraft.client.model.geom;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EntityModelSet {
    public static final EntityModelSet EMPTY = new EntityModelSet(Map.of());
    private final Map<ModelLayerLocation, LayerDefinition> roots;

    public EntityModelSet(final Map<ModelLayerLocation, LayerDefinition> roots) {
        this.roots = roots;
    }

    public ModelPart bakeLayer(final ModelLayerLocation id) {
        LayerDefinition result = this.roots.get(id);
        if (result == null) {
            throw new IllegalArgumentException("No model for layer " + id);
        } else {
            return result.bakeRoot();
        }
    }

    public static EntityModelSet vanilla() {
        return new EntityModelSet(ImmutableMap.copyOf(LayerDefinitions.createRoots()));
    }
}