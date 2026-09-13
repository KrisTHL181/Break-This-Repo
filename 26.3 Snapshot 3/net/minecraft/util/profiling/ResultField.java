package net.minecraft.util.profiling;

public final class ResultField implements Comparable<ResultField> {
   public final double percentage;
   public final double globalPercentage;
   public final long count;
   public final String name;

   public ResultField(final String name, final double percentage, final double globalPercentage, final long count) {
      this.name = name;
      this.percentage = percentage;
      this.globalPercentage = globalPercentage;
      this.count = count;
   }

   public int compareTo(final ResultField resultField) {
      if (resultField.percentage < this.percentage) {
         return -1;
      } else {
         return resultField.percentage > this.percentage ? 1 : resultField.name.compareTo(this.name);
      }
   }

   public int getColor() {
      return (this.name.hashCode() & 11184810) + -12303292;
   }
}
