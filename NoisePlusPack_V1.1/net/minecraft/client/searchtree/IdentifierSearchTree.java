package net.minecraft.client.searchtree;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface IdentifierSearchTree<T> {
   static <T> IdentifierSearchTree<T> empty() {
      return new IdentifierSearchTree<T>() {
         @Override
         public List<T> searchNamespace(String p_452154_) {
            return List.of();
         }

         @Override
         public List<T> searchPath(String p_461083_) {
            return List.of();
         }
      };
   }

   static <T> IdentifierSearchTree<T> create(List<T> p_460010_, Function<T, Stream<Identifier>> p_454313_) {
      if (p_460010_.isEmpty()) {
         return empty();
      }

      final SuffixArray<T> suffixarray = new SuffixArray<>();
      final SuffixArray<T> suffixarray1 = new SuffixArray<>();

      for (T t : p_460010_) {
         p_454313_.apply(t).forEach(p_454112_ -> {
            suffixarray.add(t, p_454112_.getNamespace().toLowerCase(Locale.ROOT));
            suffixarray1.add(t, p_454112_.getPath().toLowerCase(Locale.ROOT));
         });
      }

      suffixarray.generate();
      suffixarray1.generate();
      return new IdentifierSearchTree<T>() {
         @Override
         public List<T> searchNamespace(String p_454374_) {
            return suffixarray.search(p_454374_);
         }

         @Override
         public List<T> searchPath(String p_450951_) {
            return suffixarray1.search(p_450951_);
         }
      };
   }

   List<T> searchNamespace(String var1);

   List<T> searchPath(String var1);
}
