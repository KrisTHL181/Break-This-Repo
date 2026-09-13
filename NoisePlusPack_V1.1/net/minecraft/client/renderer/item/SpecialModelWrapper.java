package net.minecraft.client.renderer.item;

import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SpecialModelWrapper<T> implements ItemModel {
   private final SpecialModelRenderer<T> specialRenderer;
   private final ModelRenderProperties properties;
   private final Supplier<Vector3fc[]> extents;

   public SpecialModelWrapper(SpecialModelRenderer<T> p_375554_, ModelRenderProperties p_393945_) {
      this.specialRenderer = p_375554_;
      this.properties = p_393945_;
      this.extents = Suppliers.memoize(() -> {
         Set<Vector3fc> set = new HashSet<>();
         p_375554_.getExtents(set::add);
         return set.toArray(new Vector3fc[0]);
      });
   }

   @Override
   public void update(
      ItemStackRenderState p_376096_,
      ItemStack p_376294_,
      ItemModelResolver p_377226_,
      ItemDisplayContext p_377206_,
      @Nullable ClientLevel p_375445_,
      @Nullable ItemOwner p_428988_,
      int p_375847_
   ) {
      p_376096_.appendModelIdentityElement(this);
      ItemStackRenderState.LayerRenderState itemstackrenderstate$layerrenderstate = p_376096_.newLayer();
      if (p_376294_.hasFoil()) {
         ItemStackRenderState.FoilType itemstackrenderstate$foiltype = ItemStackRenderState.FoilType.STANDARD;
         itemstackrenderstate$layerrenderstate.setFoilType(itemstackrenderstate$foiltype);
         p_376096_.setAnimated();
         p_376096_.appendModelIdentityElement(itemstackrenderstate$foiltype);
      }

      T t = this.specialRenderer.extractArgument(p_376294_);
      itemstackrenderstate$layerrenderstate.setExtents(this.extents);
      itemstackrenderstate$layerrenderstate.setupSpecialModel(this.specialRenderer, t);
      if (t != null) {
         p_376096_.appendModelIdentityElement(t);
      }

      this.properties.applyToLayer(itemstackrenderstate$layerrenderstate, p_377206_);
   }

   @OnlyIn(Dist.CLIENT)
   public record Unbaked(Identifier base, SpecialModelRenderer.Unbaked specialModel) implements ItemModel.Unbaked {
      public static final MapCodec<SpecialModelWrapper.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_448365_ -> p_448365_.group(
               Identifier.CODEC.fieldOf("base").forGetter(SpecialModelWrapper.Unbaked::base),
               SpecialModelRenderers.CODEC.fieldOf("model").forGetter(SpecialModelWrapper.Unbaked::specialModel)
            )
            .apply(p_448365_, SpecialModelWrapper.Unbaked::new)
      );

      @Override
      public void resolveDependencies(ResolvableModel.Resolver p_377714_) {
         p_377714_.markDependency(this.base);
      }

      @Override
      public ItemModel bake(ItemModel.BakingContext p_378066_) {
         SpecialModelRenderer<?> specialmodelrenderer = this.specialModel.bake(p_378066_);
         if (specialmodelrenderer == null) {
            return p_378066_.missingItemModel();
         }

         ModelRenderProperties modelrenderproperties = this.getProperties(p_378066_);
         return new SpecialModelWrapper<>(specialmodelrenderer, modelrenderproperties);
      }

      private ModelRenderProperties getProperties(ItemModel.BakingContext p_393172_) {
         ModelBaker modelbaker = p_393172_.blockModelBaker();
         ResolvedModel resolvedmodel = modelbaker.getModel(this.base);
         TextureSlots textureslots = resolvedmodel.getTopTextureSlots();
         return ModelRenderProperties.fromResolvedModel(modelbaker, resolvedmodel, textureslots);
      }

      @Override
      public MapCodec<SpecialModelWrapper.Unbaked> type() {
         return MAP_CODEC;
      }
   }
}
