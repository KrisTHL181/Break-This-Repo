package net.minecraft.client.resources.metadata.gui;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.OptionalInt;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

public interface GuiSpriteScaling {
   Codec<GuiSpriteScaling> CODEC = GuiSpriteScaling.Type.CODEC.dispatch(GuiSpriteScaling::type, GuiSpriteScaling.Type::codec);
   GuiSpriteScaling DEFAULT = new GuiSpriteScaling.Stretch();

   GuiSpriteScaling.Type type();

   record NineSlice(int width, int height, GuiSpriteScaling.NineSlice.Border border, boolean stretchInner) implements GuiSpriteScaling {
      // ===== 修改：mapCodec 显式类型参数，validate 改为 lambda =====
      public static final MapCodec<GuiSpriteScaling.NineSlice> CODEC = RecordCodecBuilder.<GuiSpriteScaling.NineSlice>mapCodec(
            i -> i.group(
                  ExtraCodecs.POSITIVE_INT.fieldOf("width").forGetter(GuiSpriteScaling.NineSlice::width),
                  ExtraCodecs.POSITIVE_INT.fieldOf("height").forGetter(GuiSpriteScaling.NineSlice::height),
                  GuiSpriteScaling.NineSlice.Border.CODEC.fieldOf("border").forGetter(GuiSpriteScaling.NineSlice::border),
                  Codec.BOOL.optionalFieldOf("stretch_inner", false).forGetter(GuiSpriteScaling.NineSlice::stretchInner)
               )
               .apply(i, GuiSpriteScaling.NineSlice::new)
         )
         .validate((GuiSpriteScaling.NineSlice nineSlice) -> GuiSpriteScaling.NineSlice.validate(nineSlice));

      private static DataResult<GuiSpriteScaling.NineSlice> validate(final GuiSpriteScaling.NineSlice nineSlice) {
         GuiSpriteScaling.NineSlice.Border border = nineSlice.border();
         if (border.left() + border.right() >= nineSlice.width()) {
            return DataResult.error(
               () -> "Nine-sliced texture has no horizontal center slice: " + border.left() + " + " + border.right() + " >= " + nineSlice.width()
            );
         } else {
            return border.top() + border.bottom() >= nineSlice.height()
               ? DataResult.error(
                  () -> "Nine-sliced texture has no vertical center slice: " + border.top() + " + " + border.bottom() + " >= " + nineSlice.height()
               )
               : DataResult.success(nineSlice);
         }
      }

      @Override
      public GuiSpriteScaling.Type type() {
         return GuiSpriteScaling.Type.NINE_SLICE;
      }

      public record Border(int left, int top, int right, int bottom) {
         private static final Codec<GuiSpriteScaling.NineSlice.Border> VALUE_CODEC = ExtraCodecs.POSITIVE_INT
            .flatComapMap(size -> new GuiSpriteScaling.NineSlice.Border(size, size, size, size), border -> {
               OptionalInt size = border.unpackValue();
               return size.isPresent() ? DataResult.success(size.getAsInt()) : DataResult.error(() -> "Border has different side sizes");
            });
         // ===== 修改：RecordCodecBuilder.create 显式类型参数 =====
         private static final Codec<GuiSpriteScaling.NineSlice.Border> RECORD_CODEC = RecordCodecBuilder.<GuiSpriteScaling.NineSlice.Border>create(
            i -> i.group(
                  ExtraCodecs.NON_NEGATIVE_INT.fieldOf("left").forGetter(GuiSpriteScaling.NineSlice.Border::left),
                  ExtraCodecs.NON_NEGATIVE_INT.fieldOf("top").forGetter(GuiSpriteScaling.NineSlice.Border::top),
                  ExtraCodecs.NON_NEGATIVE_INT.fieldOf("right").forGetter(GuiSpriteScaling.NineSlice.Border::right),
                  ExtraCodecs.NON_NEGATIVE_INT.fieldOf("bottom").forGetter(GuiSpriteScaling.NineSlice.Border::bottom)
               )
               .apply(i, GuiSpriteScaling.NineSlice.Border::new)
         );
         private static final Codec<GuiSpriteScaling.NineSlice.Border> CODEC = Codec.either(VALUE_CODEC, RECORD_CODEC)
            .xmap(Either::unwrap, border -> border.unpackValue().isPresent() ? Either.left(border) : Either.right(border));

         private OptionalInt unpackValue() {
            return this.left() == this.top() && this.top() == this.right() && this.right() == this.bottom() ? OptionalInt.of(this.left()) : OptionalInt.empty();
         }
      }
   }

   record Stretch() implements GuiSpriteScaling {
      public static final MapCodec<GuiSpriteScaling.Stretch> CODEC = MapCodec.unit(GuiSpriteScaling.Stretch::new);

      @Override
      public GuiSpriteScaling.Type type() {
         return GuiSpriteScaling.Type.STRETCH;
      }
   }

   record Tile(int width, int height) implements GuiSpriteScaling {
      // ===== 修改：mapCodec 显式类型参数 =====
      public static final MapCodec<GuiSpriteScaling.Tile> CODEC = RecordCodecBuilder.<GuiSpriteScaling.Tile>mapCodec(
         i -> i.group(
               ExtraCodecs.POSITIVE_INT.fieldOf("width").forGetter(GuiSpriteScaling.Tile::width),
               ExtraCodecs.POSITIVE_INT.fieldOf("height").forGetter(GuiSpriteScaling.Tile::height)
            )
            .apply(i, GuiSpriteScaling.Tile::new)
      );

      @Override
      public GuiSpriteScaling.Type type() {
         return GuiSpriteScaling.Type.TILE;
      }
   }

   enum Type implements StringRepresentable {
      STRETCH("stretch", GuiSpriteScaling.Stretch.CODEC),
      TILE("tile", GuiSpriteScaling.Tile.CODEC),
      NINE_SLICE("nine_slice", GuiSpriteScaling.NineSlice.CODEC);

      public static final Codec<GuiSpriteScaling.Type> CODEC = StringRepresentable.fromEnum(GuiSpriteScaling.Type::values);
      private final String key;
      private final MapCodec<? extends GuiSpriteScaling> codec;

      Type(final String key, final MapCodec<? extends GuiSpriteScaling> codec) {
         this.key = key;
         this.codec = codec;
      }

      @Override
      public String getSerializedName() {
         return this.key;
      }

      public MapCodec<? extends GuiSpriteScaling> codec() {
         return this.codec;
      }
   }
}