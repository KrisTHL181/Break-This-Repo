package net.minecraft.server.dedicated;

import com.google.common.base.MoreObjects;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.RegistryAccess;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class Settings<T extends Settings<T>> {
   private static final Logger LOGGER = LogUtils.getLogger();
   protected final Properties properties;

   public Settings(Properties p_139801_) {
      this.properties = p_139801_;
   }

   public static Properties loadFromFile(Path p_139840_) {
      try {
         try (InputStream inputstream = Files.newInputStream(p_139840_)) {
            CharsetDecoder charsetdecoder = StandardCharsets.UTF_8
               .newDecoder()
               .onMalformedInput(CodingErrorAction.REPORT)
               .onUnmappableCharacter(CodingErrorAction.REPORT);
            Properties properties2 = new Properties();
            properties2.load(new InputStreamReader(inputstream, charsetdecoder));
            return properties2;
         } catch (CharacterCodingException charactercodingexception) {
            LOGGER.info("Failed to load properties as UTF-8 from file {}, trying ISO_8859_1", p_139840_);

            try (Reader reader = Files.newBufferedReader(p_139840_, StandardCharsets.ISO_8859_1)) {
               Properties properties = new Properties();
               properties.load(reader);
               return properties;
            }
         }
      } catch (IOException ioexception) {
         LOGGER.error("Failed to load properties from file: {}", p_139840_, ioexception);
         return new Properties();
      }
   }

   public void store(Path p_139877_) {
      try (Writer writer = Files.newBufferedWriter(p_139877_, StandardCharsets.UTF_8)) {
         this.properties.store(writer, "Minecraft server properties");
      } catch (IOException ioexception) {
         LOGGER.error("Failed to store properties to file: {}", p_139877_);
      }
   }

   private static <V extends Number> Function<String, @Nullable V> wrapNumberDeserializer(Function<String, V> p_139842_) {
      return p_139845_ -> {
         try {
            return p_139842_.apply(p_139845_);
         } catch (NumberFormatException numberformatexception) {
            return null;
         }
      };
   }

   protected static <V> Function<String, @Nullable V> dispatchNumberOrString(IntFunction<@Nullable V> p_139851_, Function<String, @Nullable V> p_139852_) {
      return p_139856_ -> {
         try {
            return p_139851_.apply(Integer.parseInt(p_139856_));
         } catch (NumberFormatException numberformatexception) {
            return p_139852_.apply(p_139856_);
         }
      };
   }

   private @Nullable String getStringRaw(String p_139879_) {
      return (String)this.properties.get(p_139879_);
   }

   protected <V> @Nullable V getLegacy(String p_139815_, Function<String, V> p_139816_) {
      String s = this.getStringRaw(p_139815_);
      if (s == null) {
         return null;
      }

      this.properties.remove(p_139815_);
      return p_139816_.apply(s);
   }

   protected <V> V get(String p_139822_, Function<String, @Nullable V> p_139823_, Function<V, String> p_139824_, V p_139825_) {
      String s = this.getStringRaw(p_139822_);
      V v = (V)MoreObjects.firstNonNull(s != null ? p_139823_.apply(s) : null, p_139825_);
      this.properties.put(p_139822_, p_139824_.apply(v));
      return v;
   }

   protected <V> Settings<T>.MutableValue<V> getMutable(String p_139869_, Function<String, @Nullable V> p_139870_, Function<V, String> p_139871_, V p_139872_) {
      String s = this.getStringRaw(p_139869_);
      V v = (V)MoreObjects.firstNonNull(s != null ? p_139870_.apply(s) : null, p_139872_);
      this.properties.put(p_139869_, p_139871_.apply(v));
      return new Settings.MutableValue<>(p_139869_, v, p_139871_);
   }

   protected <V> V get(String p_139827_, Function<String, @Nullable V> p_139828_, UnaryOperator<V> p_139829_, Function<V, String> p_139830_, V p_139831_) {
      return this.get(p_139827_, p_139849_ -> {
         V v = p_139828_.apply(p_139849_);
         return v != null ? p_139829_.apply(v) : null;
      }, p_139830_, p_139831_);
   }

   protected <V> V get(String p_139818_, Function<String, V> p_139819_, V p_139820_) {
      return this.get(p_139818_, p_139819_, Objects::toString, p_139820_);
   }

   protected <V> Settings<T>.MutableValue<V> getMutable(String p_139865_, Function<String, V> p_139866_, V p_139867_) {
      return this.getMutable(p_139865_, p_139866_, Objects::toString, p_139867_);
   }

   protected String get(String p_139812_, String p_139813_) {
      return this.get(p_139812_, Function.identity(), Function.identity(), p_139813_);
   }

   protected @Nullable String getLegacyString(String p_139804_) {
      return this.getLegacy(p_139804_, Function.identity());
   }

   protected int get(String p_139806_, int p_139807_) {
      return this.get(p_139806_, wrapNumberDeserializer(Integer::parseInt), Integer.valueOf(p_139807_));
   }

   protected Settings<T>.MutableValue<Integer> getMutable(String p_139862_, int p_139863_) {
      return this.getMutable(p_139862_, wrapNumberDeserializer(Integer::parseInt), p_139863_);
   }

   protected Settings<T>.MutableValue<String> getMutable(String p_427791_, String p_431229_) {
      return this.getMutable(p_427791_, String::new, p_431229_);
   }

   protected int get(String p_139833_, UnaryOperator<Integer> p_139834_, int p_139835_) {
      return this.get(p_139833_, wrapNumberDeserializer(Integer::parseInt), p_139834_, Objects::toString, p_139835_);
   }

   protected long get(String p_139809_, long p_139810_) {
      return this.get(p_139809_, wrapNumberDeserializer(Long::parseLong), p_139810_);
   }

   protected boolean get(String p_139837_, boolean p_139838_) {
      return this.get(p_139837_, Boolean::valueOf, p_139838_);
   }

   protected Settings<T>.MutableValue<Boolean> getMutable(String p_139874_, boolean p_139875_) {
      return this.getMutable(p_139874_, Boolean::valueOf, p_139875_);
   }

   protected @Nullable Boolean getLegacyBoolean(String p_139860_) {
      return this.getLegacy(p_139860_, Boolean::valueOf);
   }

   protected Properties cloneProperties() {
      Properties properties = new Properties();
      properties.putAll(this.properties);
      return properties;
   }

   protected abstract T reload(RegistryAccess var1, Properties var2);

   public class MutableValue<V> implements Supplier<V> {
      private final String key;
      private final V value;
      private final Function<V, String> serializer;

      MutableValue(final String p_139886_, final V p_139887_, final Function<V, String> p_139888_) {
         this.key = p_139886_;
         this.value = p_139887_;
         this.serializer = p_139888_;
      }

      @Override
      public V get() {
         return this.value;
      }

      public T update(RegistryAccess p_139896_, V p_139897_) {
         Properties properties = Settings.this.cloneProperties();
         properties.put(this.key, this.serializer.apply(p_139897_));
         return Settings.this.reload(p_139896_, properties);
      }
   }
}
