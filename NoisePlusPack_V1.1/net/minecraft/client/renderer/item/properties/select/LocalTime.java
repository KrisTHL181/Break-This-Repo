package net.minecraft.client.renderer.item.properties.select;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class LocalTime implements SelectItemModelProperty<String> {
   public static final String ROOT_LOCALE = "";
   private static final long UPDATE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(1L);
   public static final Codec<String> VALUE_CODEC = Codec.STRING;
   private static final Codec<TimeZone> TIME_ZONE_CODEC = VALUE_CODEC.comapFlatMap(p_376253_ -> {
      TimeZone timezone = TimeZone.getTimeZone(p_376253_);
      return timezone.equals(TimeZone.UNKNOWN_ZONE) ? DataResult.error(() -> "Unknown timezone: " + p_376253_) : DataResult.success(timezone);
   }, TimeZone::getID);
   private static final MapCodec<LocalTime.Data> DATA_MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_378756_ -> p_378756_.group(
            Codec.STRING.fieldOf("pattern").forGetter(p_378804_ -> p_378804_.format),
            Codec.STRING.optionalFieldOf("locale", "").forGetter(p_375710_ -> p_375710_.localeId),
            TIME_ZONE_CODEC.optionalFieldOf("time_zone").forGetter(p_375894_ -> p_375894_.timeZone)
         )
         .apply(p_378756_, LocalTime.Data::new)
   );
   public static final SelectItemModelProperty.Type<LocalTime, String> TYPE = SelectItemModelProperty.Type.create(
      DATA_MAP_CODEC.flatXmap(LocalTime::create, p_377409_ -> DataResult.success(p_377409_.data)), VALUE_CODEC
   );
   private final LocalTime.Data data;
   private final DateFormat parsedFormat;
   private long nextUpdateTimeMs;
   private String lastResult = "";

   private LocalTime(LocalTime.Data p_375944_, DateFormat p_378503_) {
      this.data = p_375944_;
      this.parsedFormat = p_378503_;
   }

   public static LocalTime create(String p_377930_, String p_378328_, Optional<TimeZone> p_375461_) {
      return (LocalTime)create(new LocalTime.Data(p_377930_, p_378328_, p_375461_))
         .getOrThrow(p_376916_ -> new IllegalStateException("Failed to validate format: " + p_376916_));
   }

   private static DataResult<LocalTime> create(LocalTime.Data p_378543_) {
      ULocale ulocale = new ULocale(p_378543_.localeId);
      Calendar calendar = p_378543_.timeZone
         .<Calendar>map(p_377754_ -> Calendar.getInstance(p_377754_, ulocale))
         .orElseGet(() -> Calendar.getInstance(ulocale));
      SimpleDateFormat simpledateformat = new SimpleDateFormat(p_378543_.format, ulocale);
      simpledateformat.setCalendar(calendar);

      try {
         simpledateformat.format(new Date());
      } catch (Exception exception) {
         return DataResult.error(() -> "Invalid time format '" + simpledateformat + "': " + exception.getMessage());
      }

      return DataResult.success(new LocalTime(p_378543_, simpledateformat));
   }

   public @Nullable String get(
      ItemStack p_378462_, @Nullable ClientLevel p_377341_, @Nullable LivingEntity p_377996_, int p_376733_, ItemDisplayContext p_377284_
   ) {
      long i = Util.getMillis();
      if (i > this.nextUpdateTimeMs) {
         this.lastResult = this.update();
         this.nextUpdateTimeMs = i + UPDATE_INTERVAL_MS;
      }

      return this.lastResult;
   }

   private String update() {
      return this.parsedFormat.format(new Date());
   }

   @Override
   public SelectItemModelProperty.Type<LocalTime, String> type() {
      return TYPE;
   }

   @Override
   public Codec<String> valueCodec() {
      return VALUE_CODEC;
   }

   @OnlyIn(Dist.CLIENT)
   record Data(String format, String localeId, Optional<TimeZone> timeZone) {
   }
}
