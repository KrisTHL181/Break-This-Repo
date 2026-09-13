package net.minecraft.client.renderer.item;

import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BlockModelWrapper implements ItemModel {
   private static final Function<ItemStack, RenderType> ITEM_RENDER_TYPE_GETTER = p_448356_ -> Sheets.translucentItemSheet();
   private static final Function<ItemStack, RenderType> BLOCK_RENDER_TYPE_GETTER = p_448355_ -> {
      if (p_448355_.getItem() instanceof BlockItem blockitem) {
         ChunkSectionLayer chunksectionlayer = ItemBlockRenderTypes.getChunkRenderType(blockitem.getBlock().defaultBlockState());
         if (chunksectionlayer != ChunkSectionLayer.TRANSLUCENT) {
            return Sheets.cutoutBlockSheet();
         }
      }

      return Sheets.translucentBlockItemSheet();
   };
   private final List<ItemTintSource> tints;
   private final List<BakedQuad> quads;
   private final Supplier<Vector3fc[]> extents;
   private final ModelRenderProperties properties;
   private final boolean animated;
   private final Function<ItemStack, RenderType> renderType;

   BlockModelWrapper(List<ItemTintSource> p_377381_, List<BakedQuad> p_396453_, ModelRenderProperties p_395664_, Function<ItemStack, RenderType> p_460789_) {
      this.tints = p_377381_;
      this.quads = p_396453_;
      this.properties = p_395664_;
      this.renderType = p_460789_;
      this.extents = Suppliers.memoize(() -> computeExtents(this.quads));
      boolean flag = false;

      for (BakedQuad bakedquad : p_396453_) {
         if (bakedquad.sprite().contents().isAnimated()) {
            flag = true;
            break;
         }
      }

      this.animated = flag;
   }

   public static Vector3fc[] computeExtents(List<BakedQuad> p_397460_) {
      Set<Vector3fc> set = new HashSet<>();

      for (BakedQuad bakedquad : p_397460_) {
         for (int i = 0; i < 4; i++) {
            set.add(bakedquad.position(i));
         }
      }

      return set.toArray(Vector3fc[]::new);
   }

   @Override
   public void update(
      ItemStackRenderState p_377049_,
      ItemStack p_378482_,
      ItemModelResolver p_377214_,
      ItemDisplayContext p_375691_,
      @Nullable ClientLevel p_376532_,
      @Nullable ItemOwner p_425592_,
      int p_377340_
   ) {
      p_377049_.appendModelIdentityElement(this);
      ItemStackRenderState.LayerRenderState itemstackrenderstate$layerrenderstate = p_377049_.newLayer();
      if (p_378482_.hasFoil()) {
         ItemStackRenderState.FoilType itemstackrenderstate$foiltype = hasSpecialAnimatedTexture(p_378482_)
            ? ItemStackRenderState.FoilType.SPECIAL
            : ItemStackRenderState.FoilType.STANDARD;
         itemstackrenderstate$layerrenderstate.setFoilType(itemstackrenderstate$foiltype);
         p_377049_.setAnimated();
         p_377049_.appendModelIdentityElement(itemstackrenderstate$foiltype);
      }

      int k = this.tints.size();
      int[] aint = itemstackrenderstate$layerrenderstate.prepareTintLayers(k);

      for (int i = 0; i < k; i++) {
         int j = this.tints.get(i).calculate(p_378482_, p_376532_, p_425592_ == null ? null : p_425592_.asLivingEntity());
         aint[i] = j;
         p_377049_.appendModelIdentityElement(j);
      }

      itemstackrenderstate$layerrenderstate.setExtents(this.extents);
      itemstackrenderstate$layerrenderstate.setRenderType(this.renderType.apply(p_378482_));
      this.properties.applyToLayer(itemstackrenderstate$layerrenderstate, p_375691_);
      itemstackrenderstate$layerrenderstate.prepareQuadList().addAll(this.quads);
      if (this.animated) {
         p_377049_.setAnimated();
      }
   }

   static Function<ItemStack, RenderType> detectRenderType(List<BakedQuad> p_457890_) {
      Iterator<BakedQuad> iterator = p_457890_.iterator();
      if (!iterator.hasNext()) {
         return ITEM_RENDER_TYPE_GETTER;
      }

      Identifier identifier = iterator.next().sprite().atlasLocation();

      while (iterator.hasNext()) {
         BakedQuad bakedquad = iterator.next();
         Identifier identifier1 = bakedquad.sprite().atlasLocation();
         if (!identifier1.equals(identifier)) {
            throw new IllegalStateException("Multiple atlases used in model, expected " + identifier + ", but also got " + identifier1);
         }
      }

      if (identifier.equals(TextureAtlas.LOCATION_ITEMS)) {
         return ITEM_RENDER_TYPE_GETTER;
      } else if (identifier.equals(TextureAtlas.LOCATION_BLOCKS)) {
         return BLOCK_RENDER_TYPE_GETTER;
      } else {
         throw new IllegalArgumentException("Atlas " + identifier + " can't be usef for item models");
      }
   }

   private static boolean hasSpecialAnimatedTexture(ItemStack p_377482_) {
      return p_377482_.is(ItemTags.COMPASSES) || p_377482_.is(Items.CLOCK);
   }

   @OnlyIn(Dist.CLIENT)
   public record Unbaked(Identifier model, List<ItemTintSource> tints) implements ItemModel.Unbaked {
      public static final MapCodec<BlockModelWrapper.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_448357_ -> p_448357_.group(
               Identifier.CODEC.fieldOf("model").forGetter(BlockModelWrapper.Unbaked::model),
               ItemTintSources.CODEC.listOf().optionalFieldOf("tints", List.of()).forGetter(BlockModelWrapper.Unbaked::tints)
            )
            .apply(p_448357_, BlockModelWrapper.Unbaked::new)
      );

      @Override
      public void resolveDependencies(ResolvableModel.Resolver p_375708_) {
         p_375708_.markDependency(this.model);
      }

      @Override
      public ItemModel bake(ItemModel.BakingContext p_375857_) {
         ModelBaker modelbaker = p_375857_.blockModelBaker();
         ResolvedModel resolvedmodel = modelbaker.getModel(this.model);
         TextureSlots textureslots = resolvedmodel.getTopTextureSlots();
         List<BakedQuad> list = resolvedmodel.bakeTopGeometry(textureslots, modelbaker, BlockModelRotation.IDENTITY).getAll();
         ModelRenderProperties modelrenderproperties = ModelRenderProperties.fromResolvedModel(modelbaker, resolvedmodel, textureslots);
         Function<ItemStack, RenderType> function = BlockModelWrapper.detectRenderType(list);
         return new BlockModelWrapper(this.tints, list, modelrenderproperties, function);
      }

      @Override
      public MapCodec<BlockModelWrapper.Unbaked> type() {
         return MAP_CODEC;
      }
   }
}
