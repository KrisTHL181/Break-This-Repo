package net.minecraft.network.chat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.BitSet;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

public class FilterMask {
   public static final Codec<FilterMask> CODEC = StringRepresentable.fromEnum(FilterMask.Type::values).dispatch(FilterMask::type, FilterMask.Type::codec);
   public static final FilterMask FULLY_FILTERED = new FilterMask(new BitSet(0), FilterMask.Type.FULLY_FILTERED);
   public static final FilterMask PASS_THROUGH = new FilterMask(new BitSet(0), FilterMask.Type.PASS_THROUGH);
   public static final Style FILTERED_STYLE = Style.EMPTY
      .withColor(ChatFormatting.DARK_GRAY)
      .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.filtered")));
   static final MapCodec<FilterMask> PASS_THROUGH_CODEC = MapCodec.unit(PASS_THROUGH);
   static final MapCodec<FilterMask> FULLY_FILTERED_CODEC = MapCodec.unit(FULLY_FILTERED);
   static final MapCodec<FilterMask> PARTIALLY_FILTERED_CODEC = ExtraCodecs.BIT_SET.xmap(FilterMask::new, FilterMask::mask).fieldOf("value");
   private static final char HASH = '#';
   private final BitSet mask;
   private final FilterMask.Type type;

   private FilterMask(BitSet p_243243_, FilterMask.Type p_243249_) {
      this.mask = p_243243_;
      this.type = p_243249_;
   }

   private FilterMask(BitSet p_253780_) {
      this.mask = p_253780_;
      this.type = FilterMask.Type.PARTIALLY_FILTERED;
   }

   public FilterMask(int p_243210_) {
      this(new BitSet(p_243210_), FilterMask.Type.PARTIALLY_FILTERED);
   }

   private FilterMask.Type type() {
      return this.type;
   }

   private BitSet mask() {
      return this.mask;
   }

   public static FilterMask read(FriendlyByteBuf p_243205_) {
      FilterMask.Type filtermask$type = p_243205_.readEnum(FilterMask.Type.class);

      return switch (filtermask$type) {
         case PASS_THROUGH -> PASS_THROUGH;
         case FULLY_FILTERED -> FULLY_FILTERED;
         case PARTIALLY_FILTERED -> new FilterMask(p_243205_.readBitSet(), FilterMask.Type.PARTIALLY_FILTERED);
      };
   }

   public static void write(FriendlyByteBuf p_243308_, FilterMask p_243231_) {
      p_243308_.writeEnum(p_243231_.type);
      if (p_243231_.type == FilterMask.Type.PARTIALLY_FILTERED) {
         p_243308_.writeBitSet(p_243231_.mask);
      }
   }

   public void setFiltered(int p_243202_) {
      this.mask.set(p_243202_);
   }

   public @Nullable String apply(String p_243317_) {
      return switch (this.type) {
         case PASS_THROUGH -> p_243317_;
         case FULLY_FILTERED -> null;
         case PARTIALLY_FILTERED -> {
            char[] achar = p_243317_.toCharArray();

            for (int i = 0; i < achar.length && i < this.mask.length(); i++) {
               if (this.mask.get(i)) {
                  achar[i] = '#';
               }
            }

            yield new String(achar);
         }
      };
   }

   public @Nullable Component applyWithFormatting(String p_251709_) {
      return switch (this.type) {
         case PASS_THROUGH -> Component.literal(p_251709_);
         case FULLY_FILTERED -> null;
         case PARTIALLY_FILTERED -> {
            MutableComponent mutablecomponent = Component.empty();
            int i = 0;
            boolean flag = this.mask.get(0);

            while (true) {
               int j = flag ? this.mask.nextClearBit(i) : this.mask.nextSetBit(i);
               j = j < 0 ? p_251709_.length() : j;
               if (j == i) {
                  yield mutablecomponent;
               }

               if (flag) {
                  mutablecomponent.append(Component.literal(StringUtils.repeat('#', j - i)).withStyle(FILTERED_STYLE));
               } else {
                  mutablecomponent.append(p_251709_.substring(i, j));
               }

               flag = !flag;
               i = j;
            }
         }
      };
   }

   public boolean isEmpty() {
      return this.type == FilterMask.Type.PASS_THROUGH;
   }

   public boolean isFullyFiltered() {
      return this.type == FilterMask.Type.FULLY_FILTERED;
   }

   @Override
   public boolean equals(Object p_254275_) {
      if (this == p_254275_) {
         return true;
      } else if (p_254275_ != null && this.getClass() == p_254275_.getClass()) {
         FilterMask filtermask = (FilterMask)p_254275_;
         return this.mask.equals(filtermask.mask) && this.type == filtermask.type;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int i = this.mask.hashCode();
      return 31 * i + this.type.hashCode();
   }

   enum Type implements StringRepresentable {
      PASS_THROUGH("pass_through", () -> FilterMask.PASS_THROUGH_CODEC),
      FULLY_FILTERED("fully_filtered", () -> FilterMask.FULLY_FILTERED_CODEC),
      PARTIALLY_FILTERED("partially_filtered", () -> FilterMask.PARTIALLY_FILTERED_CODEC);

      private final String serializedName;
      private final Supplier<MapCodec<FilterMask>> codec;

      Type(final String p_253679_, final Supplier<MapCodec<FilterMask>> p_253988_) {
         this.serializedName = p_253679_;
         this.codec = p_253988_;
      }

      @Override
      public String getSerializedName() {
         return this.serializedName;
      }

      private MapCodec<FilterMask> codec() {
         return this.codec.get();
      }
   }
}
