package net.minecraft.client.resources.metadata.gui;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.OptionalInt;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface GuiSpriteScaling {
   Codec<GuiSpriteScaling> CODEC = GuiSpriteScaling.Type.CODEC.dispatch(GuiSpriteScaling::type, GuiSpriteScaling.Type::codec);
   GuiSpriteScaling DEFAULT = new GuiSpriteScaling.Stretch();

   GuiSpriteScaling.Type type();

   @OnlyIn(Dist.CLIENT)
   record NineSlice(int width, int height, GuiSpriteScaling.NineSlice.Border border, boolean stretchInner) implements GuiSpriteScaling {
      public static final MapCodec<GuiSpriteScaling.NineSlice> CODEC = RecordCodecBuilder.mapCodec(
            p_358030_ -> p_358030_.group(
                  ExtraCodecs.POSITIVE_INT.fieldOf("width").forGetter(GuiSpriteScaling.NineSlice::width),
                  ExtraCodecs.POSITIVE_INT.fieldOf("height").forGetter(GuiSpriteScaling.NineSlice::height),
                  GuiSpriteScaling.NineSlice.Border.CODEC.fieldOf("border").forGetter(GuiSpriteScaling.NineSlice::border),
                  Codec.BOOL.optionalFieldOf("stretch_inner", false).forGetter(GuiSpriteScaling.NineSlice::stretchInner)
               )
               .apply(p_358030_, GuiSpriteScaling.NineSlice::new)
         )
         .validate(GuiSpriteScaling.NineSlice::validate);

      private static DataResult<GuiSpriteScaling.NineSlice> validate(GuiSpriteScaling.NineSlice p_298579_) {
         GuiSpriteScaling.NineSlice.Border guispritescaling$nineslice$border = p_298579_.border();
         if (guispritescaling$nineslice$border.left() + guispritescaling$nineslice$border.right() >= p_298579_.width()) {
            return DataResult.error(
               () -> "Nine-sliced texture has no horizontal center slice: "
                  + guispritescaling$nineslice$border.left()
                  + " + "
                  + guispritescaling$nineslice$border.right()
                  + " >= "
                  + p_298579_.width()
            );
         } else {
            return guispritescaling$nineslice$border.top() + guispritescaling$nineslice$border.bottom() >= p_298579_.height()
               ? DataResult.error(
                  () -> "Nine-sliced texture has no vertical center slice: "
                     + guispritescaling$nineslice$border.top()
                     + " + "
                     + guispritescaling$nineslice$border.bottom()
                     + " >= "
                     + p_298579_.height()
               )
               : DataResult.success(p_298579_);
         }
      }

      @Override
      public GuiSpriteScaling.Type type() {
         return GuiSpriteScaling.Type.NINE_SLICE;
      }

      @OnlyIn(Dist.CLIENT)
      public record Border(int left, int top, int right, int bottom) {
         private static final Codec<GuiSpriteScaling.NineSlice.Border> VALUE_CODEC = ExtraCodecs.POSITIVE_INT
            .flatComapMap(p_299885_ -> new GuiSpriteScaling.NineSlice.Border(p_299885_, p_299885_, p_299885_, p_299885_), p_299528_ -> {
               OptionalInt optionalint = p_299528_.unpackValue();
               return optionalint.isPresent() ? DataResult.success(optionalint.getAsInt()) : DataResult.error(() -> "Border has different side sizes");
            });
         private static final Codec<GuiSpriteScaling.NineSlice.Border> RECORD_CODEC = RecordCodecBuilder.create(
            p_297306_ -> p_297306_.group(
                  ExtraCodecs.NON_NEGATIVE_INT.fieldOf("left").forGetter(GuiSpriteScaling.NineSlice.Border::left),
                  ExtraCodecs.NON_NEGATIVE_INT.fieldOf("top").forGetter(GuiSpriteScaling.NineSlice.Border::top),
                  ExtraCodecs.NON_NEGATIVE_INT.fieldOf("right").forGetter(GuiSpriteScaling.NineSlice.Border::right),
                  ExtraCodecs.NON_NEGATIVE_INT.fieldOf("bottom").forGetter(GuiSpriteScaling.NineSlice.Border::bottom)
               )
               .apply(p_297306_, GuiSpriteScaling.NineSlice.Border::new)
         );
         static final Codec<GuiSpriteScaling.NineSlice.Border> CODEC = Codec.either(VALUE_CODEC, RECORD_CODEC)
            .xmap(Either::unwrap, p_297509_ -> p_297509_.unpackValue().isPresent() ? Either.left(p_297509_) : Either.right(p_297509_));

         private OptionalInt unpackValue() {
            return this.left() == this.top() && this.top() == this.right() && this.right() == this.bottom() ? OptionalInt.of(this.left()) : OptionalInt.empty();
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   record Stretch() implements GuiSpriteScaling {
      public static final MapCodec<GuiSpriteScaling.Stretch> CODEC = MapCodec.unit(GuiSpriteScaling.Stretch::new);

      @Override
      public GuiSpriteScaling.Type type() {
         return GuiSpriteScaling.Type.STRETCH;
      }
   }

   @OnlyIn(Dist.CLIENT)
   record Tile(int width, int height) implements GuiSpriteScaling {
      public static final MapCodec<GuiSpriteScaling.Tile> CODEC = RecordCodecBuilder.mapCodec(
         p_297832_ -> p_297832_.group(
               ExtraCodecs.POSITIVE_INT.fieldOf("width").forGetter(GuiSpriteScaling.Tile::width),
               ExtraCodecs.POSITIVE_INT.fieldOf("height").forGetter(GuiSpriteScaling.Tile::height)
            )
            .apply(p_297832_, GuiSpriteScaling.Tile::new)
      );

      @Override
      public GuiSpriteScaling.Type type() {
         return GuiSpriteScaling.Type.TILE;
      }
   }

   @OnlyIn(Dist.CLIENT)
   enum Type implements StringRepresentable {
      STRETCH("stretch", GuiSpriteScaling.Stretch.CODEC),
      TILE("tile", GuiSpriteScaling.Tile.CODEC),
      NINE_SLICE("nine_slice", GuiSpriteScaling.NineSlice.CODEC);

      public static final Codec<GuiSpriteScaling.Type> CODEC = StringRepresentable.fromEnum(GuiSpriteScaling.Type::values);
      private final String key;
      private final MapCodec<? extends GuiSpriteScaling> codec;

      Type(final String p_299685_, final MapCodec<? extends GuiSpriteScaling> p_329674_) {
         this.key = p_299685_;
         this.codec = p_329674_;
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
