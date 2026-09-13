package net.minecraft.util.datafix.fixes;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ParticleUnflatteningFix extends DataFix {
   private static final Logger LOGGER = LogUtils.getLogger();

   public ParticleUnflatteningFix(Schema p_328690_) {
      super(p_328690_, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.PARTICLE);
      Type<?> type1 = this.getOutputSchema().getType(References.PARTICLE);
      return this.writeFixAndRead("ParticleUnflatteningFix", type, type1, this::fix);
   }

   private <T> Dynamic<T> fix(Dynamic<T> p_329773_) {
      Optional<String> optional = p_329773_.asString().result();
      if (optional.isEmpty()) {
         return p_329773_;
      }

      String s = optional.get();
      String[] astring = s.split(" ", 2);
      String s1 = NamespacedSchema.ensureNamespaced(astring[0]);
      Dynamic<T> dynamic = p_329773_.createMap(Map.of(p_329773_.createString("type"), p_329773_.createString(s1)));

      return switch (s1) {
         case "minecraft:item" -> astring.length > 1 ? this.updateItem(dynamic, astring[1]) : dynamic;
         case "minecraft:block", "minecraft:block_marker", "minecraft:falling_dust", "minecraft:dust_pillar" -> astring.length > 1
            ? this.updateBlock(dynamic, astring[1])
            : dynamic;
         case "minecraft:dust" -> astring.length > 1 ? this.updateDust(dynamic, astring[1]) : dynamic;
         case "minecraft:dust_color_transition" -> astring.length > 1 ? this.updateDustTransition(dynamic, astring[1]) : dynamic;
         case "minecraft:sculk_charge" -> astring.length > 1 ? this.updateSculkCharge(dynamic, astring[1]) : dynamic;
         case "minecraft:vibration" -> astring.length > 1 ? this.updateVibration(dynamic, astring[1]) : dynamic;
         case "minecraft:shriek" -> astring.length > 1 ? this.updateShriek(dynamic, astring[1]) : dynamic;
         default -> dynamic;
      };
   }

   private <T> Dynamic<T> updateItem(Dynamic<T> p_336140_, String p_328811_) {
      int i = p_328811_.indexOf("{");
      Dynamic<T> dynamic = p_336140_.createMap(Map.of(p_336140_.createString("Count"), p_336140_.createInt(1)));
      if (i == -1) {
         dynamic = dynamic.set("id", p_336140_.createString(p_328811_));
      } else {
         dynamic = dynamic.set("id", p_336140_.createString(p_328811_.substring(0, i)));
         Dynamic<T> dynamic1 = parseTag(p_336140_.getOps(), p_328811_.substring(i));
         if (dynamic1 != null) {
            dynamic = dynamic.set("tag", dynamic1);
         }
      }

      return p_336140_.set("item", dynamic);
   }

   private static <T> @Nullable Dynamic<T> parseTag(DynamicOps<T> p_393424_, String p_328375_) {
      try {
         return new Dynamic(p_393424_, TagParser.create(p_393424_).parseFully(p_328375_));
      } catch (Exception exception) {
         LOGGER.warn("Failed to parse tag: {}", p_328375_, exception);
         return null;
      }
   }

   private <T> Dynamic<T> updateBlock(Dynamic<T> p_330510_, String p_334026_) {
      int i = p_334026_.indexOf("[");
      Dynamic<T> dynamic = p_330510_.emptyMap();
      if (i == -1) {
         dynamic = dynamic.set("Name", p_330510_.createString(NamespacedSchema.ensureNamespaced(p_334026_)));
      } else {
         dynamic = dynamic.set("Name", p_330510_.createString(NamespacedSchema.ensureNamespaced(p_334026_.substring(0, i))));
         Map<Dynamic<T>, Dynamic<T>> map = parseBlockProperties(p_330510_, p_334026_.substring(i));
         if (!map.isEmpty()) {
            dynamic = dynamic.set("Properties", p_330510_.createMap(map));
         }
      }

      return p_330510_.set("block_state", dynamic);
   }

   private static <T> Map<Dynamic<T>, Dynamic<T>> parseBlockProperties(Dynamic<T> p_328751_, String p_332614_) {
      try {
         Map<Dynamic<T>, Dynamic<T>> map = new HashMap<>();
         StringReader stringreader = new StringReader(p_332614_);
         stringreader.expect('[');
         stringreader.skipWhitespace();

         while (stringreader.canRead() && stringreader.peek() != ']') {
            stringreader.skipWhitespace();
            String s = stringreader.readString();
            stringreader.skipWhitespace();
            stringreader.expect('=');
            stringreader.skipWhitespace();
            String s1 = stringreader.readString();
            stringreader.skipWhitespace();
            map.put(p_328751_.createString(s), p_328751_.createString(s1));
            if (stringreader.canRead()) {
               if (stringreader.peek() != ',') {
                  break;
               }

               stringreader.skip();
            }
         }

         stringreader.expect(']');
         return map;
      } catch (Exception exception) {
         LOGGER.warn("Failed to parse block properties: {}", p_332614_, exception);
         return Map.of();
      }
   }

   private static <T> Dynamic<T> readVector(Dynamic<T> p_333163_, StringReader p_332387_) throws CommandSyntaxException {
      float f = p_332387_.readFloat();
      p_332387_.expect(' ');
      float f1 = p_332387_.readFloat();
      p_332387_.expect(' ');
      float f2 = p_332387_.readFloat();
      return p_333163_.createList(Stream.of(f, f1, f2).map(p_333163_::createFloat));
   }

   private <T> Dynamic<T> updateDust(Dynamic<T> p_335964_, String p_335720_) {
      try {
         StringReader stringreader = new StringReader(p_335720_);
         Dynamic<T> dynamic = readVector(p_335964_, stringreader);
         stringreader.expect(' ');
         float f = stringreader.readFloat();
         return p_335964_.set("color", dynamic).set("scale", p_335964_.createFloat(f));
      } catch (Exception exception) {
         LOGGER.warn("Failed to parse particle options: {}", p_335720_, exception);
         return p_335964_;
      }
   }

   private <T> Dynamic<T> updateDustTransition(Dynamic<T> p_328807_, String p_329378_) {
      try {
         StringReader stringreader = new StringReader(p_329378_);
         Dynamic<T> dynamic = readVector(p_328807_, stringreader);
         stringreader.expect(' ');
         float f = stringreader.readFloat();
         stringreader.expect(' ');
         Dynamic<T> dynamic1 = readVector(p_328807_, stringreader);
         return p_328807_.set("from_color", dynamic).set("to_color", dynamic1).set("scale", p_328807_.createFloat(f));
      } catch (Exception exception) {
         LOGGER.warn("Failed to parse particle options: {}", p_329378_, exception);
         return p_328807_;
      }
   }

   private <T> Dynamic<T> updateSculkCharge(Dynamic<T> p_335265_, String p_329894_) {
      try {
         StringReader stringreader = new StringReader(p_329894_);
         float f = stringreader.readFloat();
         return p_335265_.set("roll", p_335265_.createFloat(f));
      } catch (Exception exception) {
         LOGGER.warn("Failed to parse particle options: {}", p_329894_, exception);
         return p_335265_;
      }
   }

   private <T> Dynamic<T> updateVibration(Dynamic<T> p_333915_, String p_333536_) {
      try {
         StringReader stringreader = new StringReader(p_333536_);
         float f = (float)stringreader.readDouble();
         stringreader.expect(' ');
         float f1 = (float)stringreader.readDouble();
         stringreader.expect(' ');
         float f2 = (float)stringreader.readDouble();
         stringreader.expect(' ');
         int i = stringreader.readInt();
         Dynamic<T> dynamic = p_333915_.createIntList(IntStream.of(Mth.floor(f), Mth.floor(f1), Mth.floor(f2)));
         Dynamic<T> dynamic1 = p_333915_.createMap(
            Map.of(p_333915_.createString("type"), p_333915_.createString("minecraft:block"), p_333915_.createString("pos"), dynamic)
         );
         return p_333915_.set("destination", dynamic1).set("arrival_in_ticks", p_333915_.createInt(i));
      } catch (Exception exception) {
         LOGGER.warn("Failed to parse particle options: {}", p_333536_, exception);
         return p_333915_;
      }
   }

   private <T> Dynamic<T> updateShriek(Dynamic<T> p_330969_, String p_330514_) {
      try {
         StringReader stringreader = new StringReader(p_330514_);
         int i = stringreader.readInt();
         return p_330969_.set("delay", p_330969_.createInt(i));
      } catch (Exception exception) {
         LOGGER.warn("Failed to parse particle options: {}", p_330514_, exception);
         return p_330969_;
      }
   }
}
