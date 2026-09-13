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
   private static final MapCodec<FilterMask> PASS_THROUGH_CODEC = MapCodec.unit(PASS_THROUGH);
   private static final MapCodec<FilterMask> FULLY_FILTERED_CODEC = MapCodec.unit(FULLY_FILTERED);
   private static final MapCodec<FilterMask> PARTIALLY_FILTERED_CODEC = ExtraCodecs.BIT_SET.xmap(FilterMask::new, FilterMask::mask).fieldOf("value");
   private static final char HASH = '#';
   private final BitSet mask;
   private final FilterMask.Type type;

   private FilterMask(final BitSet mask, final FilterMask.Type type) {
      this.mask = mask;
      this.type = type;
   }

   private FilterMask(final BitSet mask) {
      this.mask = mask;
      this.type = FilterMask.Type.PARTIALLY_FILTERED;
   }

   public FilterMask(final int length) {
      this(new BitSet(length), FilterMask.Type.PARTIALLY_FILTERED);
   }

   private FilterMask.Type type() {
      return this.type;
   }

   private BitSet mask() {
      return this.mask;
   }

   public static FilterMask read(final FriendlyByteBuf input) {
      FilterMask.Type type = input.readEnum(FilterMask.Type.class);

      return switch (type) {
         case PASS_THROUGH -> PASS_THROUGH;
         case FULLY_FILTERED -> FULLY_FILTERED;
         case PARTIALLY_FILTERED -> new FilterMask(input.readBitSet(), FilterMask.Type.PARTIALLY_FILTERED);
      };
   }

   public static void write(final FriendlyByteBuf output, final FilterMask mask) {
      output.writeEnum(mask.type);
      if (mask.type == FilterMask.Type.PARTIALLY_FILTERED) {
         output.writeBitSet(mask.mask);
      }
   }

   public void setFiltered(final int index) {
      this.mask.set(index);
   }

   public @Nullable String apply(final String text) {
      return switch (this.type) {
         case PASS_THROUGH -> text;
         case FULLY_FILTERED -> null;
         case PARTIALLY_FILTERED -> {
            char[] chars = text.toCharArray();

            for (int i = 0; i < chars.length && i < this.mask.length(); i++) {
               if (this.mask.get(i)) {
                  chars[i] = '#';
               }
            }

            yield new String(chars);
         }
      };
   }

   public @Nullable Component applyWithFormatting(final String text) {
      return switch (this.type) {
         case PASS_THROUGH -> Component.literal(text);
         case FULLY_FILTERED -> null;
         case PARTIALLY_FILTERED -> {
            MutableComponent result = Component.empty();
            int previousIndex = 0;
            boolean filtered = this.mask.get(0);

            while (true) {
               int nextIndex = filtered ? this.mask.nextClearBit(previousIndex) : this.mask.nextSetBit(previousIndex);
               nextIndex = nextIndex < 0 ? text.length() : nextIndex;
               if (nextIndex == previousIndex) {
                  yield result;
               }

               if (filtered) {
                  result.append(Component.literal(StringUtils.repeat('#', nextIndex - previousIndex)).withStyle(FILTERED_STYLE));
               } else {
                  result.append(text.substring(previousIndex, nextIndex));
               }

               filtered = !filtered;
               previousIndex = nextIndex;
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
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         FilterMask that = (FilterMask)o;
         return this.mask.equals(that.mask) && this.type == that.type;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = this.mask.hashCode();
      return 31 * result + this.type.hashCode();
   }

   private enum Type implements StringRepresentable {
      PASS_THROUGH("pass_through", () -> FilterMask.PASS_THROUGH_CODEC),
      FULLY_FILTERED("fully_filtered", () -> FilterMask.FULLY_FILTERED_CODEC),
      PARTIALLY_FILTERED("partially_filtered", () -> FilterMask.PARTIALLY_FILTERED_CODEC);

      private final String serializedName;
      private final Supplier<MapCodec<FilterMask>> codec;

      Type(final String serializedName, final Supplier<MapCodec<FilterMask>> codec) {
         this.serializedName = serializedName;
         this.codec = codec;
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
