package net.minecraft.advancements;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class AdvancementProgress implements Comparable<AdvancementProgress> {
   private static final DateTimeFormatter OBTAINED_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT);
   private static final Codec<Instant> OBTAINED_TIME_CODEC = ExtraCodecs.temporalCodec(OBTAINED_TIME_FORMAT)
      .xmap(Instant::from, p_296108_ -> p_296108_.atZone(ZoneId.systemDefault()));
   private static final Codec<Map<String, CriterionProgress>> CRITERIA_CODEC = Codec.unboundedMap(Codec.STRING, OBTAINED_TIME_CODEC)
      .xmap(
         p_447776_ -> Util.mapValues(p_447776_, CriterionProgress::new),
         p_296101_ -> p_296101_.entrySet()
            .stream()
            .filter(p_296109_ -> ((CriterionProgress)p_296109_.getValue()).isDone())
            .collect(Collectors.toMap(Entry::getKey, p_296103_ -> Objects.requireNonNull(((CriterionProgress)p_296103_.getValue()).getObtained())))
      );
   public static final Codec<AdvancementProgress> CODEC = RecordCodecBuilder.create(
      p_325180_ -> p_325180_.group(
            CRITERIA_CODEC.optionalFieldOf("criteria", Map.of()).forGetter(p_296102_ -> p_296102_.criteria),
            Codec.BOOL.fieldOf("done").orElse(true).forGetter(AdvancementProgress::isDone)
         )
         .apply(p_325180_, (p_296106_, p_296107_) -> new AdvancementProgress(new HashMap<>(p_296106_)))
   );
   private final Map<String, CriterionProgress> criteria;
   private AdvancementRequirements requirements = AdvancementRequirements.EMPTY;

   private AdvancementProgress(Map<String, CriterionProgress> p_144358_) {
      this.criteria = p_144358_;
   }

   public AdvancementProgress() {
      this.criteria = Maps.newHashMap();
   }

   public void update(AdvancementRequirements p_300626_) {
      Set<String> set = p_300626_.names();
      this.criteria.entrySet().removeIf(p_8203_ -> !set.contains(p_8203_.getKey()));

      for (String s : set) {
         this.criteria.putIfAbsent(s, new CriterionProgress());
      }

      this.requirements = p_300626_;
   }

   public boolean isDone() {
      return this.requirements.test(this::isCriterionDone);
   }

   public boolean hasProgress() {
      for (CriterionProgress criterionprogress : this.criteria.values()) {
         if (criterionprogress.isDone()) {
            return true;
         }
      }

      return false;
   }

   public boolean grantProgress(String p_8197_) {
      CriterionProgress criterionprogress = this.criteria.get(p_8197_);
      if (criterionprogress != null && !criterionprogress.isDone()) {
         criterionprogress.grant();
         return true;
      } else {
         return false;
      }
   }

   public boolean revokeProgress(String p_8210_) {
      CriterionProgress criterionprogress = this.criteria.get(p_8210_);
      if (criterionprogress != null && criterionprogress.isDone()) {
         criterionprogress.revoke();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public String toString() {
      return "AdvancementProgress{criteria=" + this.criteria + ", requirements=" + this.requirements + "}";
   }

   public void serializeToNetwork(FriendlyByteBuf p_8205_) {
      p_8205_.writeMap(this.criteria, FriendlyByteBuf::writeUtf, (p_144360_, p_144361_) -> p_144361_.serializeToNetwork(p_144360_));
   }

   public static AdvancementProgress fromNetwork(FriendlyByteBuf p_8212_) {
      Map<String, CriterionProgress> map = p_8212_.readMap(FriendlyByteBuf::readUtf, CriterionProgress::fromNetwork);
      return new AdvancementProgress(map);
   }

   public @Nullable CriterionProgress getCriterion(String p_8215_) {
      return this.criteria.get(p_8215_);
   }

   private boolean isCriterionDone(String p_301316_) {
      CriterionProgress criterionprogress = this.getCriterion(p_301316_);
      return criterionprogress != null && criterionprogress.isDone();
   }

   public float getPercent() {
      if (this.criteria.isEmpty()) {
         return 0.0F;
      }

      float f = this.requirements.size();
      float f1 = this.countCompletedRequirements();
      return f1 / f;
   }

   public @Nullable Component getProgressText() {
      if (this.criteria.isEmpty()) {
         return null;
      }

      int i = this.requirements.size();
      if (i <= 1) {
         return null;
      }

      int j = this.countCompletedRequirements();
      return Component.translatable("advancements.progress", j, i);
   }

   private int countCompletedRequirements() {
      return this.requirements.count(this::isCriterionDone);
   }

   public Iterable<String> getRemainingCriteria() {
      List<String> list = Lists.newArrayList();

      for (Entry<String, CriterionProgress> entry : this.criteria.entrySet()) {
         if (!entry.getValue().isDone()) {
            list.add(entry.getKey());
         }
      }

      return list;
   }

   public Iterable<String> getCompletedCriteria() {
      List<String> list = Lists.newArrayList();

      for (Entry<String, CriterionProgress> entry : this.criteria.entrySet()) {
         if (entry.getValue().isDone()) {
            list.add(entry.getKey());
         }
      }

      return list;
   }

   public @Nullable Instant getFirstProgressDate() {
      return this.criteria.values().stream().map(CriterionProgress::getObtained).filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
   }

   public int compareTo(AdvancementProgress p_8195_) {
      Instant instant = this.getFirstProgressDate();
      Instant instant1 = p_8195_.getFirstProgressDate();
      if (instant == null && instant1 != null) {
         return 1;
      } else if (instant != null && instant1 == null) {
         return -1;
      } else {
         return instant == null && instant1 == null ? 0 : instant.compareTo(instant1);
      }
   }
}
