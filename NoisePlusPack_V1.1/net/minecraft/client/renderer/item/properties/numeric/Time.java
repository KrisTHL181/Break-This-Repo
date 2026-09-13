package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.MoonPhase;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Time extends NeedleDirectionHelper implements RangeSelectItemModelProperty {
   public static final MapCodec<Time> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_378406_ -> p_378406_.group(
            Codec.BOOL.optionalFieldOf("wobble", true).forGetter(NeedleDirectionHelper::wobble),
            Time.TimeSource.CODEC.fieldOf("source").forGetter(p_377822_ -> p_377822_.source)
         )
         .apply(p_378406_, Time::new)
   );
   private final Time.TimeSource source;
   private final RandomSource randomSource = RandomSource.create();
   private final NeedleDirectionHelper.Wobbler wobbler;

   public Time(boolean p_378592_, Time.TimeSource p_376677_) {
      super(p_378592_);
      this.source = p_376677_;
      this.wobbler = this.newWobbler(0.9F);
   }

   @Override
   protected float calculate(ItemStack p_378177_, ClientLevel p_378594_, int p_378007_, ItemOwner p_424452_) {
      float f = this.source.get(p_378594_, p_378177_, p_424452_, this.randomSource);
      long i = p_378594_.getGameTime();
      if (this.wobbler.shouldUpdate(i)) {
         this.wobbler.update(i, f);
      }

      return this.wobbler.rotation();
   }

   @Override
   public MapCodec<Time> type() {
      return MAP_CODEC;
   }

   @OnlyIn(Dist.CLIENT)
   public enum TimeSource implements StringRepresentable {
      RANDOM("random") {
         @Override
         public float get(ClientLevel p_378410_, ItemStack p_375483_, ItemOwner p_426428_, RandomSource p_376828_) {
            return p_376828_.nextFloat();
         }
      },
      DAYTIME("daytime") {
         @Override
         public float get(ClientLevel p_376912_, ItemStack p_377893_, ItemOwner p_427146_, RandomSource p_375920_) {
            return p_376912_.environmentAttributes().getValue(EnvironmentAttributes.SUN_ANGLE, p_427146_.position()) / 360.0F;
         }
      },
      MOON_PHASE("moon_phase") {
         @Override
         public float get(ClientLevel p_377401_, ItemStack p_376961_, ItemOwner p_422855_, RandomSource p_377203_) {
            return (float)p_377401_.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, p_422855_.position()).index() / MoonPhase.COUNT;
         }
      };

      public static final Codec<Time.TimeSource> CODEC = StringRepresentable.fromEnum(Time.TimeSource::values);
      private final String name;

      TimeSource(final String p_376225_) {
         this.name = p_376225_;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      abstract float get(ClientLevel var1, ItemStack var2, ItemOwner var3, RandomSource var4);
   }
}
