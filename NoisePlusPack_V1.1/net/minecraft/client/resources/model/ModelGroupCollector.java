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
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModelGroupCollector {
   static final int SINGLETON_MODEL_GROUP = -1;
   private static final int INVISIBLE_MODEL_GROUP = 0;

   public static Object2IntMap<BlockState> build(BlockColors p_367669_, BlockStateModelLoader.LoadedModels p_368285_) {
      Map<Block, List<Property<?>>> map = new HashMap<>();
      Map<ModelGroupCollector.GroupKey, Set<BlockState>> map1 = new HashMap<>();
      p_368285_.models().forEach((p_389610_, p_389611_) -> {
         List<Property<?>> list = map.computeIfAbsent(p_389610_.getBlock(), p_361060_ -> List.copyOf(p_367669_.getColoringProperties(p_361060_)));
         ModelGroupCollector.GroupKey modelgroupcollector$groupkey = ModelGroupCollector.GroupKey.create(p_389610_, p_389611_, list);
         map1.computeIfAbsent(modelgroupcollector$groupkey, p_367245_ -> Sets.newIdentityHashSet()).add(p_389610_);
      });
      int i = 1;
      Object2IntMap<BlockState> object2intmap = new Object2IntOpenHashMap();
      object2intmap.defaultReturnValue(-1);

      for (Set<BlockState> set : map1.values()) {
         Iterator<BlockState> iterator = set.iterator();

         while (iterator.hasNext()) {
            BlockState blockstate = iterator.next();
            if (blockstate.getRenderShape() != RenderShape.MODEL) {
               iterator.remove();
               object2intmap.put(blockstate, 0);
            }
         }

         if (set.size() > 1) {
            int j = i++;
            set.forEach(p_362909_ -> object2intmap.put(p_362909_, j));
         }
      }

      return object2intmap;
   }

   @OnlyIn(Dist.CLIENT)
   record GroupKey(Object equalityGroup, List<Object> coloringValues) {
      public static ModelGroupCollector.GroupKey create(BlockState p_367993_, BlockStateModel.UnbakedRoot p_396888_, List<Property<?>> p_363265_) {
         List<Object> list = getColoringValues(p_367993_, p_363265_);
         Object object = p_396888_.visualEqualityGroup(p_367993_);
         return new ModelGroupCollector.GroupKey(object, list);
      }

      private static List<Object> getColoringValues(BlockState p_367197_, List<Property<?>> p_360879_) {
         Object[] aobject = new Object[p_360879_.size()];

         for (int i = 0; i < p_360879_.size(); i++) {
            aobject[i] = p_367197_.getValue(p_360879_.get(i));
         }

         return List.of(aobject);
      }
   }
}
