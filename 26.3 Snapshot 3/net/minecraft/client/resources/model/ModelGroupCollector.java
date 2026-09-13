package net.minecraft.client.resources.model;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class ModelGroupCollector {
   static final int SINGLETON_MODEL_GROUP = -1;
   private static final int INVISIBLE_MODEL_GROUP = 0;

   public static Object2IntMap<BlockState> build(final BlockColors blockColors, final BlockStateModelLoader.LoadedModels input) {
      Map<Block, List<Property<?>>> coloringPropertiesCache = new HashMap<>();
      Map<ModelGroupCollector.GroupKey, Set<BlockState>> modelGroups = new HashMap<>();
      input.models()
         .forEach(
            (state, loadedModel) -> {
               List<Property<?>> coloringProperties = coloringPropertiesCache.computeIfAbsent(
                  state.getBlock(), block -> List.copyOf(blockColors.getColoringProperties(block))
               );
               ModelGroupCollector.GroupKey key = ModelGroupCollector.GroupKey.create(state, loadedModel, coloringProperties);
               modelGroups.computeIfAbsent(key, k -> Sets.newIdentityHashSet()).add(state);
            }
         );
      int nextModelGroup = 1;
      Object2IntMap<BlockState> result = new Object2IntOpenHashMap();
      result.defaultReturnValue(-1);

      for (Set<BlockState> states : modelGroups.values()) {
         Iterator<BlockState> it = states.iterator();

         while (it.hasNext()) {
            BlockState state = it.next();
            if (state.getRenderShape() != RenderShape.MODEL) {
               it.remove();
               result.put(state, 0);
            }
         }

         if (states.size() > 1) {
            int modelGroup = nextModelGroup++;
            states.forEach(blockState -> result.put(blockState, modelGroup));
         }
      }

      return result;
   }

   private record GroupKey(Object equalityGroup, List<Object> coloringValues) {
      public static ModelGroupCollector.GroupKey create(
         final BlockState state, final BlockStateModel.UnbakedRoot model, final List<Property<?>> coloringProperties
      ) {
         List<Object> coloringValues = getColoringValues(state, coloringProperties);
         Object equalityGroup = model.visualEqualityGroup(state);
         return new ModelGroupCollector.GroupKey(equalityGroup, coloringValues);
      }

      private static List<Object> getColoringValues(final BlockState state, final List<Property<?>> coloringProperties) {
         Object[] coloringValues = new Object[coloringProperties.size()];

         for (int i = 0; i < coloringProperties.size(); i++) {
            coloringValues[i] = state.getValue(coloringProperties.get(i));
         }

         return List.of(coloringValues);
      }
   }
}
