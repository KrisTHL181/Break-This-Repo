package net.minecraft.client.resources.model;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class QuadCollection {
   public static final QuadCollection EMPTY = new QuadCollection(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
   private final List<BakedQuad> all;
   private final List<BakedQuad> unculled;
   private final List<BakedQuad> north;
   private final List<BakedQuad> south;
   private final List<BakedQuad> east;
   private final List<BakedQuad> west;
   private final List<BakedQuad> up;
   private final List<BakedQuad> down;

   QuadCollection(
      List<BakedQuad> p_391548_,
      List<BakedQuad> p_397628_,
      List<BakedQuad> p_392216_,
      List<BakedQuad> p_397515_,
      List<BakedQuad> p_391585_,
      List<BakedQuad> p_393386_,
      List<BakedQuad> p_393693_,
      List<BakedQuad> p_393927_
   ) {
      this.all = p_391548_;
      this.unculled = p_397628_;
      this.north = p_392216_;
      this.south = p_397515_;
      this.east = p_391585_;
      this.west = p_393386_;
      this.up = p_393693_;
      this.down = p_393927_;
   }

   public List<BakedQuad> getQuads(@Nullable Direction p_394582_) {
      return switch (p_394582_) {
         case null -> this.unculled;
         case NORTH -> this.north;
         case SOUTH -> this.south;
         case EAST -> this.east;
         case WEST -> this.west;
         case UP -> this.up;
         case DOWN -> this.down;
      };
   }

   public List<BakedQuad> getAll() {
      return this.all;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder {
      private final com.google.common.collect.ImmutableList.Builder<BakedQuad> unculledFaces = ImmutableList.builder();
      private final Multimap<Direction, BakedQuad> culledFaces = ArrayListMultimap.create();

      public QuadCollection.Builder addCulledFace(Direction p_396778_, BakedQuad p_396647_) {
         this.culledFaces.put(p_396778_, p_396647_);
         return this;
      }

      public QuadCollection.Builder addUnculledFace(BakedQuad p_397122_) {
         this.unculledFaces.add(p_397122_);
         return this;
      }

      private static QuadCollection createFromSublists(
         List<BakedQuad> p_393861_, int p_393519_, int p_394001_, int p_391425_, int p_397077_, int p_396687_, int p_395872_, int p_395332_
      ) {
         int i = 0;
         int j;
         List<BakedQuad> list = p_393861_.subList(i, j = i + p_393519_);
         List<BakedQuad> list1 = p_393861_.subList(j, i = j + p_394001_);
         int k;
         List<BakedQuad> list2 = p_393861_.subList(i, k = i + p_391425_);
         List<BakedQuad> list3 = p_393861_.subList(k, i = k + p_397077_);
         int l;
         List<BakedQuad> list4 = p_393861_.subList(i, l = i + p_396687_);
         List<BakedQuad> list5 = p_393861_.subList(l, i = l + p_395872_);
         List<BakedQuad> list6 = p_393861_.subList(i, i + p_395332_);
         return new QuadCollection(p_393861_, list, list1, list2, list3, list4, list5, list6);
      }

      public QuadCollection build() {
         ImmutableList<BakedQuad> immutablelist = this.unculledFaces.build();
         if (this.culledFaces.isEmpty()) {
            return immutablelist.isEmpty()
               ? QuadCollection.EMPTY
               : new QuadCollection(immutablelist, immutablelist, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
         }

         com.google.common.collect.ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
         builder.addAll(immutablelist);
         Collection<BakedQuad> collection = this.culledFaces.get(Direction.NORTH);
         builder.addAll(collection);
         Collection<BakedQuad> collection1 = this.culledFaces.get(Direction.SOUTH);
         builder.addAll(collection1);
         Collection<BakedQuad> collection2 = this.culledFaces.get(Direction.EAST);
         builder.addAll(collection2);
         Collection<BakedQuad> collection3 = this.culledFaces.get(Direction.WEST);
         builder.addAll(collection3);
         Collection<BakedQuad> collection4 = this.culledFaces.get(Direction.UP);
         builder.addAll(collection4);
         Collection<BakedQuad> collection5 = this.culledFaces.get(Direction.DOWN);
         builder.addAll(collection5);
         return createFromSublists(
            builder.build(),
            immutablelist.size(),
            collection.size(),
            collection1.size(),
            collection2.size(),
            collection3.size(),
            collection4.size(),
            collection5.size()
         );
      }
   }
}
