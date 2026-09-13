package net.minecraft.client.renderer.item;

import com.google.common.base.Suppliers;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class MissingItemModel implements ItemModel {
   private final List<BakedQuad> quads;
   private final Supplier<Vector3fc[]> extents;
   private final ModelRenderProperties properties;
   private final Matrix4fc transform;

   public MissingItemModel(final List<BakedQuad> quads, final ModelRenderProperties properties) {
      this(quads, Suppliers.memoize(() -> CuboidItemModelWrapper.computeExtents(quads)), properties, new Matrix4f());
   }

   private MissingItemModel(final List<BakedQuad> quads, final Supplier<Vector3fc[]> extents, final ModelRenderProperties properties, final Matrix4fc transform) {
      this.quads = quads;
      this.extents = extents;
      this.properties = properties;
      this.transform = transform;
   }

   @Override
   public void update(
      final ItemStackRenderState output,
      final ItemStack item,
      final ItemModelResolver resolver,
      final ItemDisplayContext displayContext,
      final @Nullable ClientLevel level,
      final @Nullable ItemOwner owner,
      final int seed
   ) {
      output.appendModelIdentityElement(this);
      ItemStackRenderState.LayerRenderState layer = output.newLayer();
      this.properties.applyToLayer(layer, displayContext);
      layer.setExtents(this.extents);
      layer.setLocalTransform(this.transform);
      layer.prepareQuadList().addAll(this.quads);
   }

   public MissingItemModel withTransform(final Matrix4fc transform) {
      return transform.equals(this.transform) ? this : new MissingItemModel(this.quads, this.extents, this.properties, transform);
   }
}
