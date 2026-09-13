package net.minecraft.advancements;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.network.FriendlyByteBuf;

public record AdvancementRequirements(List<List<String>> requirements) {
   public static final Codec<AdvancementRequirements> CODEC = Codec.STRING
      .listOf()
      .listOf()
      .xmap(AdvancementRequirements::new, AdvancementRequirements::requirements);
   public static final AdvancementRequirements EMPTY = new AdvancementRequirements(List.of());

   public AdvancementRequirements(FriendlyByteBuf p_299417_) {
      this(p_299417_.readList(p_325185_ -> p_325185_.readList(FriendlyByteBuf::readUtf)));
   }

   public void write(FriendlyByteBuf p_299546_) {
      p_299546_.writeCollection(this.requirements, (p_325183_, p_325184_) -> p_325183_.writeCollection(p_325184_, FriendlyByteBuf::writeUtf));
   }

   public static AdvancementRequirements allOf(Collection<String> p_300431_) {
      return new AdvancementRequirements(p_300431_.stream().map(List::of).toList());
   }

   public static AdvancementRequirements anyOf(Collection<String> p_297776_) {
      return new AdvancementRequirements(List.of(List.copyOf(p_297776_)));
   }

   public int size() {
      return this.requirements.size();
   }

   public boolean test(Predicate<String> p_297982_) {
      if (this.requirements.isEmpty()) {
         return false;
      }

      for (List<String> list : this.requirements) {
         if (!anyMatch(list, p_297982_)) {
            return false;
         }
      }

      return true;
   }

   public int count(Predicate<String> p_300443_) {
      int i = 0;

      for (List<String> list : this.requirements) {
         if (anyMatch(list, p_300443_)) {
            i++;
         }
      }

      return i;
   }

   private static boolean anyMatch(List<String> p_309914_, Predicate<String> p_299134_) {
      for (String s : p_309914_) {
         if (p_299134_.test(s)) {
            return true;
         }
      }

      return false;
   }

   public DataResult<AdvancementRequirements> validate(Set<String> p_311051_) {
      Set<String> set = new ObjectOpenHashSet();

      for (List<String> list : this.requirements) {
         if (list.isEmpty() && p_311051_.isEmpty()) {
            return DataResult.error(() -> "Requirement entry cannot be empty");
         }

         set.addAll(list);
      }

      if (!p_311051_.equals(set)) {
         Set<String> set1 = Sets.difference(p_311051_, set);
         Set<String> set2 = Sets.difference(set, p_311051_);
         return DataResult.error(() -> "Advancement completion requirements did not exactly match specified criteria. Missing: " + set1 + ". Unknown: " + set2);
      } else {
         return DataResult.success(this);
      }
   }

   public boolean isEmpty() {
      return this.requirements.isEmpty();
   }

   @Override
   public String toString() {
      return this.requirements.toString();
   }

   public Set<String> names() {
      Set<String> set = new ObjectOpenHashSet();

      for (List<String> list : this.requirements) {
         set.addAll(list);
      }

      return set;
   }

   public interface Strategy {
      AdvancementRequirements.Strategy AND = AdvancementRequirements::allOf;
      AdvancementRequirements.Strategy OR = AdvancementRequirements::anyOf;

      AdvancementRequirements create(Collection<String> var1);
   }
}
