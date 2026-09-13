package net.minecraft.client.resources.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectFunction;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ModelDiscovery {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Object2ObjectMap<Identifier, ModelDiscovery.ModelWrapper> modelWrappers = new Object2ObjectOpenHashMap();
   private final ModelDiscovery.ModelWrapper missingModel;
   private final Object2ObjectFunction<Identifier, ModelDiscovery.ModelWrapper> uncachedResolver;
   private final ResolvableModel.Resolver resolver;
   private final Queue<ModelDiscovery.ModelWrapper> parentDiscoveryQueue = new ArrayDeque<>();

   public ModelDiscovery(Map<Identifier, UnbakedModel> p_362964_, UnbakedModel p_367385_) {
      this.missingModel = new ModelDiscovery.ModelWrapper(MissingBlockModel.LOCATION, p_367385_, true);
      this.modelWrappers.put(MissingBlockModel.LOCATION, this.missingModel);
      this.uncachedResolver = p_448446_ -> {
         Identifier identifier = (Identifier)p_448446_;
         UnbakedModel unbakedmodel = p_362964_.get(identifier);
         if (unbakedmodel == null) {
            LOGGER.warn("Missing block model: {}", identifier);
            return this.missingModel;
         } else {
            return this.createAndQueueWrapper(identifier, unbakedmodel);
         }
      };
      this.resolver = this::getOrCreateModel;
   }

   private static boolean isRoot(UnbakedModel p_394200_) {
      return p_394200_.parent() == null;
   }

   private ModelDiscovery.ModelWrapper getOrCreateModel(Identifier p_453016_) {
      return (ModelDiscovery.ModelWrapper)this.modelWrappers.computeIfAbsent(p_453016_, this.uncachedResolver);
   }

   private ModelDiscovery.ModelWrapper createAndQueueWrapper(Identifier p_459979_, UnbakedModel p_391978_) {
      boolean flag = isRoot(p_391978_);
      ModelDiscovery.ModelWrapper modeldiscovery$modelwrapper = new ModelDiscovery.ModelWrapper(p_459979_, p_391978_, flag);
      if (!flag) {
         this.parentDiscoveryQueue.add(modeldiscovery$modelwrapper);
      }

      return modeldiscovery$modelwrapper;
   }

   public void addRoot(ResolvableModel p_376215_) {
      p_376215_.resolveDependencies(this.resolver);
   }

   public void addSpecialModel(Identifier p_460236_, UnbakedModel p_391360_) {
      if (!isRoot(p_391360_)) {
         LOGGER.warn("Trying to add non-root special model {}, ignoring", p_460236_);
      } else {
         ModelDiscovery.ModelWrapper modeldiscovery$modelwrapper = (ModelDiscovery.ModelWrapper)this.modelWrappers
            .put(p_460236_, this.createAndQueueWrapper(p_460236_, p_391360_));
         if (modeldiscovery$modelwrapper != null) {
            LOGGER.warn("Duplicate special model {}", p_460236_);
         }
      }
   }

   public ResolvedModel missingModel() {
      return this.missingModel;
   }

   public Map<Identifier, ResolvedModel> resolve() {
      List<ModelDiscovery.ModelWrapper> list = new ArrayList<>();
      this.discoverDependencies(list);
      propagateValidity(list);
      Builder<Identifier, ResolvedModel> builder = ImmutableMap.builder();
      this.modelWrappers.forEach((p_460758_, p_389606_) -> {
         if (p_389606_.valid) {
            builder.put(p_460758_, p_389606_);
         } else {
            LOGGER.warn("Model {} ignored due to cyclic dependency", p_460758_);
         }
      });
      return builder.build();
   }

   private void discoverDependencies(List<ModelDiscovery.ModelWrapper> p_396534_) {
      ModelDiscovery.ModelWrapper modeldiscovery$modelwrapper;
      while ((modeldiscovery$modelwrapper = this.parentDiscoveryQueue.poll()) != null) {
         Identifier identifier = Objects.requireNonNull(modeldiscovery$modelwrapper.wrapped.parent());
         ModelDiscovery.ModelWrapper modeldiscovery$modelwrapper1 = this.getOrCreateModel(identifier);
         modeldiscovery$modelwrapper.parent = modeldiscovery$modelwrapper1;
         if (modeldiscovery$modelwrapper1.valid) {
            modeldiscovery$modelwrapper.valid = true;
         } else {
            p_396534_.add(modeldiscovery$modelwrapper);
         }
      }
   }

   private static void propagateValidity(List<ModelDiscovery.ModelWrapper> p_394425_) {
      boolean flag = true;

      while (flag) {
         flag = false;
         Iterator<ModelDiscovery.ModelWrapper> iterator = p_394425_.iterator();

         while (iterator.hasNext()) {
            ModelDiscovery.ModelWrapper modeldiscovery$modelwrapper = iterator.next();
            if (Objects.requireNonNull(modeldiscovery$modelwrapper.parent).valid) {
               modeldiscovery$modelwrapper.valid = true;
               iterator.remove();
               flag = true;
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class ModelWrapper implements ResolvedModel {
      private static final ModelDiscovery.Slot<Boolean> KEY_AMBIENT_OCCLUSION = slot(0);
      private static final ModelDiscovery.Slot<UnbakedModel.GuiLight> KEY_GUI_LIGHT = slot(1);
      private static final ModelDiscovery.Slot<UnbakedGeometry> KEY_GEOMETRY = slot(2);
      private static final ModelDiscovery.Slot<ItemTransforms> KEY_TRANSFORMS = slot(3);
      private static final ModelDiscovery.Slot<TextureSlots> KEY_TEXTURE_SLOTS = slot(4);
      private static final ModelDiscovery.Slot<TextureAtlasSprite> KEY_PARTICLE_SPRITE = slot(5);
      private static final ModelDiscovery.Slot<QuadCollection> KEY_DEFAULT_GEOMETRY = slot(6);
      private static final int SLOT_COUNT = 7;
      private final Identifier id;
      boolean valid;
      ModelDiscovery.@Nullable ModelWrapper parent;
      final UnbakedModel wrapped;
      private final AtomicReferenceArray<@Nullable Object> fixedSlots = new AtomicReferenceArray<>(7);
      private final Map<ModelState, QuadCollection> modelBakeCache = new ConcurrentHashMap<>();

      private static <T> ModelDiscovery.Slot<T> slot(int p_392332_) {
         Objects.checkIndex(p_392332_, 7);
         return new ModelDiscovery.Slot<>(p_392332_);
      }

      ModelWrapper(Identifier p_454615_, UnbakedModel p_394055_, boolean p_397832_) {
         this.id = p_454615_;
         this.wrapped = p_394055_;
         this.valid = p_397832_;
      }

      @Override
      public UnbakedModel wrapped() {
         return this.wrapped;
      }

      @Override
      public @Nullable ResolvedModel parent() {
         return this.parent;
      }

      @Override
      public String debugName() {
         return this.id.toString();
      }

      private <T> @Nullable T getSlot(ModelDiscovery.Slot<T> p_394981_) {
         return (T)this.fixedSlots.get(p_394981_.index);
      }

      private <T> T updateSlot(ModelDiscovery.Slot<T> p_391987_, T p_391757_) {
         T t = (T)this.fixedSlots.compareAndExchange(p_391987_.index, null, p_391757_);
         return t == null ? p_391757_ : t;
      }

      private <T> T getSimpleProperty(ModelDiscovery.Slot<T> p_397608_, Function<ResolvedModel, T> p_393296_) {
         T t = this.getSlot(p_397608_);
         return t != null ? t : this.updateSlot(p_397608_, p_393296_.apply(this));
      }

      @Override
      public boolean getTopAmbientOcclusion() {
         return this.getSimpleProperty(KEY_AMBIENT_OCCLUSION, ResolvedModel::findTopAmbientOcclusion);
      }

      @Override
      public UnbakedModel.GuiLight getTopGuiLight() {
         return this.getSimpleProperty(KEY_GUI_LIGHT, ResolvedModel::findTopGuiLight);
      }

      @Override
      public ItemTransforms getTopTransforms() {
         return this.getSimpleProperty(KEY_TRANSFORMS, ResolvedModel::findTopTransforms);
      }

      @Override
      public UnbakedGeometry getTopGeometry() {
         return this.getSimpleProperty(KEY_GEOMETRY, ResolvedModel::findTopGeometry);
      }

      @Override
      public TextureSlots getTopTextureSlots() {
         return this.getSimpleProperty(KEY_TEXTURE_SLOTS, ResolvedModel::findTopTextureSlots);
      }

      @Override
      public TextureAtlasSprite resolveParticleSprite(TextureSlots p_396706_, ModelBaker p_393999_) {
         TextureAtlasSprite textureatlassprite = this.getSlot(KEY_PARTICLE_SPRITE);
         return textureatlassprite != null
            ? textureatlassprite
            : this.updateSlot(KEY_PARTICLE_SPRITE, ResolvedModel.resolveParticleSprite(p_396706_, p_393999_, this));
      }

      private QuadCollection bakeDefaultState(TextureSlots p_392267_, ModelBaker p_393576_, ModelState p_391972_) {
         QuadCollection quadcollection = this.getSlot(KEY_DEFAULT_GEOMETRY);
         return quadcollection != null
            ? quadcollection
            : this.updateSlot(KEY_DEFAULT_GEOMETRY, this.getTopGeometry().bake(p_392267_, p_393576_, p_391972_, this));
      }

      @Override
      public QuadCollection bakeTopGeometry(TextureSlots p_396404_, ModelBaker p_391625_, ModelState p_396681_) {
         return p_396681_ == BlockModelRotation.IDENTITY
            ? this.bakeDefaultState(p_396404_, p_391625_, p_396681_)
            : this.modelBakeCache.computeIfAbsent(p_396681_, p_394933_ -> {
               UnbakedGeometry unbakedgeometry = this.getTopGeometry();
               return unbakedgeometry.bake(p_396404_, p_391625_, p_394933_, this);
            });
      }
   }

   @OnlyIn(Dist.CLIENT)
   record Slot<T>(int index) {
   }
}
