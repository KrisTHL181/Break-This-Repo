package net.minecraft.client.renderer.item;

import com.google.common.base.Suppliers;
import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class CuboidItemModelWrapper implements ItemModel {
   private final List<ItemTintSource> tints;
   private final QuadCollection quads;
   private final Supplier<Vector3fc[]> extents;
   private final ModelRenderProperties properties;
   private final Matrix4fc transformation;

   private CuboidItemModelWrapper(
      final List<ItemTintSource> tints, final QuadCollection quads, final ModelRenderProperties properties, final Matrix4fc transformation
   ) {
      this.tints = tints;
      this.quads = quads;
      this.properties = properties;
      this.transformation = transformation;
      this.extents = Suppliers.memoize(() -> computeExtents(quads.getAll()));
   }

   public static Vector3fc[] computeExtents(final List<BakedQuad> quads) {
      Set<Vector3fc> result = new HashSet<>();

      for (BakedQuad quad : quads) {
         for (int vertex = 0; vertex < 4; vertex++) {
            result.add(quad.position(vertex));
         }
      }

      return result.toArray(Vector3fc[]::new);
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
      if (item.hasFoil()) {
         ItemStackRenderState.FoilType foilType = hasSpecialAnimatedTexture(item)
            ? ItemStackRenderState.FoilType.SPECIAL
            : ItemStackRenderState.FoilType.STANDARD;
         layer.setFoilType(foilType);
         output.setAnimated();
         output.appendModelIdentityElement(foilType);
      }

      if (!this.tints.isEmpty()) {
         IntList tintLayers = layer.tintLayers();

         for (ItemTintSource tintSource : this.tints) {
            int tint = tintSource.calculate(item, level, owner == null ? null : owner.asLivingEntity());
            tintLayers.add(tint);
            output.appendModelIdentityElement(tint);
         }
      }

      layer.setExtents(this.extents);
      layer.setLocalTransform(this.transformation);
      this.properties.applyToLayer(layer, displayContext);
      layer.prepareQuadList().addAll(this.quads.getAll());
      if (this.quads.hasMaterialFlag(2)) {
         output.setAnimated();
      }
   }

   private static void validateAtlasUsage(final List<BakedQuad> quads) {
      Iterator<BakedQuad> quadIterator = quads.iterator();
      if (quadIterator.hasNext()) {
         Identifier expectedAtlas = quadIterator.next().materialInfo().sprite().atlasLocation();

         while (quadIterator.hasNext()) {
            BakedQuad quad = quadIterator.next();
            Identifier quadAtlas = quad.materialInfo().sprite().atlasLocation();
            if (!quadAtlas.equals(expectedAtlas)) {
               throw new IllegalStateException("Multiple atlases used in model, expected " + expectedAtlas + ", but also got " + quadAtlas);
            }
         }

         if (!expectedAtlas.equals(TextureAtlas.LOCATION_ITEMS) && !expectedAtlas.equals(TextureAtlas.LOCATION_BLOCKS)) {
            throw new IllegalArgumentException("Atlas " + expectedAtlas + " can't be usef for item models");
         }
      }
   }

   private static boolean hasSpecialAnimatedTexture(final ItemStack itemStack) {
      return itemStack.is(ItemTags.COMPASSES) || itemStack.is(Items.CLOCK);
   }

   public record Unbaked(Identifier model, Optional<Transformation> transformation, List<ItemTintSource> tints) implements ItemModel.Unbaked {
      public static final MapCodec<CuboidItemModelWrapper.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               Identifier.CODEC.fieldOf("model").forGetter(CuboidItemModelWrapper.Unbaked::model),
               Transformation.EXTENDED_CODEC.optionalFieldOf("transformation").forGetter(CuboidItemModelWrapper.Unbaked::transformation),
               ItemTintSources.CODEC.listOf().optionalFieldOf("tints", List.of()).forGetter(CuboidItemModelWrapper.Unbaked::tints)
            )
            .apply(i, CuboidItemModelWrapper.Unbaked::new)
      );

      @Override
      public void resolveDependencies(final ResolvableModel.Resolver resolver) {
         resolver.markDependency(this.model);
      }

      @Override
      public ItemModel bake(final ItemModel.BakingContext context, final Matrix4fc transformation) {
         ModelBaker baker = context.blockModelBaker();
         ResolvedModel resolvedModel = baker.getModel(this.model);
         TextureSlots textureSlots = resolvedModel.getTopTextureSlots();
         QuadCollection quads = resolvedModel.bakeTopGeometry(textureSlots, baker, BlockModelRotation.IDENTITY);
         ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(baker, resolvedModel, textureSlots);
         CuboidItemModelWrapper.validateAtlasUsage(quads.getAll());
         Matrix4fc modelTransform = Transformation.compose(transformation, this.transformation);
         return new CuboidItemModelWrapper(this.tints, quads, properties, modelTransform);
      }

      @Override
      public MapCodec<CuboidItemModelWrapper.Unbaked> type() {
         return MAP_CODEC;
      }
   }
}
