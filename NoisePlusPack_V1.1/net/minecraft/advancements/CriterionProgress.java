package net.minecraft.advancements;

import java.time.Instant;
import net.minecraft.network.FriendlyByteBuf;
import org.jspecify.annotations.Nullable;

public class CriterionProgress {
   private @Nullable Instant obtained;

   public CriterionProgress() {
   }

   public CriterionProgress(Instant p_299196_) {
      this.obtained = p_299196_;
   }

   public boolean isDone() {
      return this.obtained != null;
   }

   public void grant() {
      this.obtained = Instant.now();
   }

   public void revoke() {
      this.obtained = null;
   }

   public @Nullable Instant getObtained() {
      return this.obtained;
   }

   @Override
   public String toString() {
      return "CriterionProgress{obtained=" + (this.obtained == null ? "false" : this.obtained) + "}";
   }

   public void serializeToNetwork(FriendlyByteBuf p_12915_) {
      p_12915_.writeNullable(this.obtained, FriendlyByteBuf::writeInstant);
   }

   public static CriterionProgress fromNetwork(FriendlyByteBuf p_12918_) {
      CriterionProgress criterionprogress = new CriterionProgress();
      criterionprogress.obtained = p_12918_.readNullable(FriendlyByteBuf::readInstant);
      return criterionprogress;
   }
}
