package net.minecraft.core;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public class IdMapper<T> implements IdMap<T> {
   private int nextId;
   private final Reference2IntMap<T> tToId;
   private final List<T> idToT;

   public IdMapper() {
      this(512);
   }

   public IdMapper(int p_122658_) {
      this.idToT = Lists.newArrayListWithExpectedSize(p_122658_);
      this.tToId = new Reference2IntOpenHashMap(p_122658_);
      this.tToId.defaultReturnValue(-1);
   }

   public void addMapping(T p_122665_, int p_122666_) {
      this.tToId.put(p_122665_, p_122666_);

      while (this.idToT.size() <= p_122666_) {
         this.idToT.add(null);
      }

      this.idToT.set(p_122666_, p_122665_);
      if (this.nextId <= p_122666_) {
         this.nextId = p_122666_ + 1;
      }
   }

   public void add(T p_122668_) {
      this.addMapping(p_122668_, this.nextId);
   }

   @Override
   public int getId(T p_122663_) {
      return this.tToId.getInt(p_122663_);
   }

   @Override
   public final @Nullable T byId(int p_122661_) {
      return p_122661_ >= 0 && p_122661_ < this.idToT.size() ? this.idToT.get(p_122661_) : null;
   }

   @Override
   public Iterator<T> iterator() {
      return Iterators.filter(this.idToT.iterator(), Objects::nonNull);
   }

   public boolean contains(int p_175381_) {
      return this.byId(p_175381_) != null;
   }

   @Override
   public int size() {
      return this.tToId.size();
   }
}
