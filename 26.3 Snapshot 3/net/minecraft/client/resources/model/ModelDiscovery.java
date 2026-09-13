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
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.cuboid.MissingCuboidModel;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ModelDiscovery {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Object2ObjectMap<Identifier, ModelDiscovery.ModelWrapper> modelWrappers = new Object2ObjectOpenHashMap();
   private final ModelDiscovery.ModelWrapper missingModel;
   private final Object2ObjectFunction<Identifier, ModelDiscovery.ModelWrapper> uncachedResolver;
   private final ResolvableModel.Resolver resolver;
   private final Queue<ModelDiscovery.ModelWrapper> parentDiscoveryQueue = new ArrayDeque<>();

   public ModelDiscovery(final Map<Identifier, UnbakedModel> unbakedModels, final UnbakedModel missingUnbakedModel) {
      this.missingModel = new ModelDiscovery.ModelWrapper(MissingCuboidModel.LOCATION, missingUnbakedModel, true);
      this.modelWrappers.put(MissingCuboidModel.LOCATION, this.missingModel);
      this.uncachedResolver = rawId -> {
         Identifier id = (Identifier)rawId;
         UnbakedModel rawModel = unbakedModels.get(id);
         if (rawModel == null) {
            LOGGER.warn("Missing block model: {}", id);
            return this.missingModel;
         } else {
            return this.createAndQueueWrapper(id, rawModel);
         }
      };
      this.resolver = this::getOrCreateModel;
   }

   private static boolean isRoot(final UnbakedModel model) {
      return model.parent() == null;
   }

   private ModelDiscovery.ModelWrapper getOrCreateModel(final Identifier id) {
      return (ModelDiscovery.ModelWrapper)this.modelWrappers.computeIfAbsent(id, this.uncachedResolver);
   }

   private ModelDiscovery.ModelWrapper createAndQueueWrapper(final Identifier id, final UnbakedModel rawModel) {
      boolean isRoot = isRoot(rawModel);
      ModelDiscovery.ModelWrapper result = new ModelDiscovery.ModelWrapper(id, rawModel, isRoot);
      if (!isRoot) {
         this.parentDiscoveryQueue.add(result);
      }

      return result;
   }

   public void addRoot(final ResolvableModel model) {
      model.resolveDependencies(this.resolver);
   }

   public void addSpecialModel(final Identifier id, final UnbakedModel model) {
      if (!isRoot(model)) {
         LOGGER.warn("Trying to add non-root special model {}, ignoring", id);
      } else {
         ModelDiscovery.ModelWrapper previous = (ModelDiscovery.ModelWrapper)this.modelWrappers.put(id, this.createAndQueueWrapper(id, model));
         if (previous != null) {
            LOGGER.warn("Duplicate special model {}", id);
         }
      }
   }

   public ResolvedModel missingModel() {
      return this.missingModel;
   }

   public Map<Identifier, ResolvedModel> resolve() {
      List<ModelDiscovery.ModelWrapper> toValidate = new ArrayList<>();
      this.discoverDependencies(toValidate);
      propagateValidity(toValidate);
      Builder<Identifier, ResolvedModel> result = ImmutableMap.builder();
      this.modelWrappers.forEach((location, model) -> {
         if (model.valid) {
            result.put(location, model);
         } else {
            LOGGER.warn("Model {} ignored due to cyclic dependency", location);
         }
      });
      return result.build();
   }

   private void discoverDependencies(final List<ModelDiscovery.ModelWrapper> toValidate) {
      ModelDiscovery.ModelWrapper current;
      while ((current = this.parentDiscoveryQueue.poll()) != null) {
         Identifier parentLocation = Objects.requireNonNull(current.wrapped.parent());
         ModelDiscovery.ModelWrapper parent = this.getOrCreateModel(parentLocation);
         current.parent = parent;
         if (parent.valid) {
            current.valid = true;
         } else {
            toValidate.add(current);
         }
      }
   }

   private static void propagateValidity(final List<ModelDiscovery.ModelWrapper> toValidate) {
      boolean progressed = true;

      while (progressed) {
         progressed = false;
         Iterator<ModelDiscovery.ModelWrapper> iterator = toValidate.iterator();

         while (iterator.hasNext()) {
            ModelDiscovery.ModelWrapper model = iterator.next();
            if (Objects.requireNonNull(model.parent).valid) {
               model.valid = true;
               iterator.remove();
               progressed = true;
            }
         }
      }
   }

   private static class ModelWrapper implements ResolvedModel {
      private static final ModelDiscovery.Slot<Boolean> KEY_AMBIENT_OCCLUSION = slot(0);
      private static final ModelDiscovery.Slot<UnbakedModel.GuiLight> KEY_GUI_LIGHT = slot(1);
      private static final ModelDiscovery.Slot<UnbakedGeometry> KEY_GEOMETRY = slot(2);
      private static final ModelDiscovery.Slot<ItemTransforms> KEY_TRANSFORMS = slot(3);
      private static final ModelDiscovery.Slot<TextureSlots> KEY_TEXTURE_SLOTS = slot(4);
      private static final ModelDiscovery.Slot<Material.Baked> KEY_PARTICLE_SPRITE = slot(5);
      private static final ModelDiscovery.Slot<QuadCollection> KEY_DEFAULT_GEOMETRY = slot(6);
      private static final int SLOT_COUNT = 7;
      private final Identifier id;
      private boolean valid;
      private ModelDiscovery.@Nullable ModelWrapper parent;
      private final UnbakedModel wrapped;
      private final AtomicReferenceArray<@Nullable Object> fixedSlots = new AtomicReferenceArray<>(7);
      private final Map<ModelState, QuadCollection> modelBakeCache = new ConcurrentHashMap<>();

      private static <T> ModelDiscovery.Slot<T> slot(final int index) {
         Objects.checkIndex(index, 7);
         return new ModelDiscovery.Slot<>(index);
      }

      private ModelWrapper(final Identifier id, final UnbakedModel wrapped, final boolean valid) {
         this.id = id;
         this.wrapped = wrapped;
         this.valid = valid;
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

      private <T> @Nullable T getSlot(final ModelDiscovery.Slot<T> key) {
         return (T)this.fixedSlots.get(key.index);
      }

      private <T> T updateSlot(final ModelDiscovery.Slot<T> key, final T value) {
         T currentValue = (T)this.fixedSlots.compareAndExchange(key.index, null, value);
         return currentValue == null ? value : currentValue;
      }

      private <T> T getSimpleProperty(final ModelDiscovery.Slot<T> key, final Function<ResolvedModel, T> getter) {
         T result = this.getSlot(key);
         return result != null ? result : this.updateSlot(key, getter.apply(this));
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
      public Material.Baked resolveParticleMaterial(final TextureSlots textureSlots, final ModelBaker baker) {
         Material.Baked result = this.getSlot(KEY_PARTICLE_SPRITE);
         return result != null ? result : this.updateSlot(KEY_PARTICLE_SPRITE, ResolvedModel.resolveParticleMaterial(textureSlots, baker, this));
      }

      private QuadCollection bakeDefaultState(final TextureSlots textureSlots, final ModelBaker baker, final ModelState state) {
         QuadCollection result = this.getSlot(KEY_DEFAULT_GEOMETRY);
         return result != null ? result : this.updateSlot(KEY_DEFAULT_GEOMETRY, this.getTopGeometry().bake(textureSlots, baker, state, this));
      }

      @Override
      public QuadCollection bakeTopGeometry(final TextureSlots textureSlots, final ModelBaker baker, final ModelState state) {
         return state == BlockModelRotation.IDENTITY ? this.bakeDefaultState(textureSlots, baker, state) : this.modelBakeCache.computeIfAbsent(state, s -> {
            UnbakedGeometry topGeometry = this.getTopGeometry();
            return topGeometry.bake(textureSlots, baker, s, this);
         });
      }
   }

   private record Slot<T>(int index) {
   }
}
