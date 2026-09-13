package net.minecraft.client.renderer.block.model;

import com.google.common.base.Splitter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class VariantSelector {
   private static final Splitter COMMA_SPLITTER = Splitter.on(',');
   private static final Splitter EQUAL_SPLITTER = Splitter.on('=').limit(2);

   public static <O, S extends StateHolder<O, S>> Predicate<StateHolder<O, S>> predicate(StateDefinition<O, S> p_365883_, String p_368935_) {
      Map<Property<?>, Comparable<?>> map = new HashMap<>();

      for (String s : COMMA_SPLITTER.split(p_368935_)) {
         Iterator<String> iterator = EQUAL_SPLITTER.split(s).iterator();
         if (iterator.hasNext()) {
            String s1 = iterator.next();
            Property<?> property = p_365883_.getProperty(s1);
            if (property != null && iterator.hasNext()) {
               String s2 = iterator.next();
               Comparable<?> comparable = getValueHelper((Property<Comparable<?>>)property, s2);
               if (comparable == null) {
                  throw new RuntimeException("Unknown value: '" + s2 + "' for blockstate property: '" + s1 + "' " + property.getPossibleValues());
               }

               map.put(property, comparable);
            } else if (!s1.isEmpty()) {
               throw new RuntimeException("Unknown blockstate property: '" + s1 + "'");
            }
         }
      }

      return p_368300_ -> {
         for (Entry<Property<?>, Comparable<?>> entry : map.entrySet()) {
            if (!Objects.equals(p_368300_.getValue(entry.getKey()), entry.getValue())) {
               return false;
            }
         }

         return true;
      };
   }

   private static <T extends Comparable<T>> @Nullable T getValueHelper(Property<T> p_365874_, String p_369794_) {
      return p_365874_.getValue(p_369794_).orElse(null);
   }
}
