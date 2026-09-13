package net.minecraft.client.searchtree;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface SearchTree<T> {
   static <T> SearchTree<T> empty() {
      return p_344644_ -> List.of();
   }

   static <T> SearchTree<T> plainText(List<T> p_344984_, Function<T, Stream<String>> p_343350_) {
      if (p_344984_.isEmpty()) {
         return empty();
      }

      SuffixArray<T> suffixarray = new SuffixArray<>();

      for (T t : p_344984_) {
         p_343350_.apply(t).forEach(p_342612_ -> suffixarray.add(t, p_342612_.toLowerCase(Locale.ROOT)));
      }

      suffixarray.generate();
      return suffixarray::search;
   }

   List<T> search(String var1);
}
