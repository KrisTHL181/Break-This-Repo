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

   public AdvancementRequirements(final FriendlyByteBuf input) {
      this(input.readList(in -> in.readList(FriendlyByteBuf::readUtf)));
   }

   public void write(final FriendlyByteBuf output) {
      output.writeCollection(this.requirements, (out, set) -> out.writeCollection(set, FriendlyByteBuf::writeUtf));
   }

   public static AdvancementRequirements allOf(final Collection<String> criteria) {
      return new AdvancementRequirements(criteria.stream().map(List::of).toList());
   }

   public static AdvancementRequirements anyOf(final Collection<String> criteria) {
      return new AdvancementRequirements(List.of(List.copyOf(criteria)));
   }

   public int size() {
      return this.requirements.size();
   }

   public boolean test(final Predicate<String> predicate) {
      if (this.requirements.isEmpty()) {
         return false;
      }

      for (List<String> set : this.requirements) {
         if (!anyMatch(set, predicate)) {
            return false;
         }
      }

      return true;
   }

   public int count(final Predicate<String> predicate) {
      int count = 0;

      for (List<String> set : this.requirements) {
         if (anyMatch(set, predicate)) {
            count++;
         }
      }

      return count;
   }

   private static boolean anyMatch(final List<String> criteria, final Predicate<String> predicate) {
      for (String criterion : criteria) {
         if (predicate.test(criterion)) {
            return true;
         }
      }

      return false;
   }

   public DataResult<AdvancementRequirements> validate(final Set<String> expectedCriteria) {
      Set<String> referencedCriteria = new ObjectOpenHashSet();

      for (List<String> set : this.requirements) {
         if (set.isEmpty() && expectedCriteria.isEmpty()) {
            return DataResult.error(() -> "Requirement entry cannot be empty");
         }

         referencedCriteria.addAll(set);
      }

      if (!expectedCriteria.equals(referencedCriteria)) {
         Set<String> missingCriteria = Sets.difference(expectedCriteria, referencedCriteria);
         Set<String> unknownCriteria = Sets.difference(referencedCriteria, expectedCriteria);
         return DataResult.error(
            () -> "Advancement completion requirements did not exactly match specified criteria. Missing: " + missingCriteria + ". Unknown: " + unknownCriteria
         );
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
      Set<String> names = new ObjectOpenHashSet();

      for (List<String> set : this.requirements) {
         names.addAll(set);
      }

      return names;
   }

   public interface Strategy {
      AdvancementRequirements.Strategy AND = AdvancementRequirements::allOf;
      AdvancementRequirements.Strategy OR = AdvancementRequirements::anyOf;

      AdvancementRequirements create(Collection<String> criteria);
   }
}
