package net.minecraft.client.resources.sounds;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.Validate;

@OnlyIn(Dist.CLIENT)
public class SoundEventRegistrationSerializer implements JsonDeserializer<SoundEventRegistration> {
   private static final FloatProvider DEFAULT_FLOAT = ConstantFloat.of(1.0F);

   public SoundEventRegistration deserialize(JsonElement p_119827_, Type p_119828_, JsonDeserializationContext p_119829_) throws JsonParseException {
      JsonObject jsonobject = GsonHelper.convertToJsonObject(p_119827_, "entry");
      boolean flag = GsonHelper.getAsBoolean(jsonobject, "replace", false);
      String s = GsonHelper.getAsString(jsonobject, "subtitle", null);
      List<Sound> list = this.getSounds(jsonobject);
      return new SoundEventRegistration(list, flag, s);
   }

   private List<Sound> getSounds(JsonObject p_119831_) {
      List<Sound> list = Lists.newArrayList();
      if (p_119831_.has("sounds")) {
         JsonArray jsonarray = GsonHelper.getAsJsonArray(p_119831_, "sounds");

         for (int i = 0; i < jsonarray.size(); i++) {
            JsonElement jsonelement = jsonarray.get(i);
            if (GsonHelper.isStringValue(jsonelement)) {
               Identifier identifier = Identifier.parse(GsonHelper.convertToString(jsonelement, "sound"));
               list.add(new Sound(identifier, DEFAULT_FLOAT, DEFAULT_FLOAT, 1, Sound.Type.FILE, false, false, 16));
            } else {
               list.add(this.getSound(GsonHelper.convertToJsonObject(jsonelement, "sound")));
            }
         }
      }

      return list;
   }

   private Sound getSound(JsonObject p_119836_) {
      Identifier identifier = Identifier.parse(GsonHelper.getAsString(p_119836_, "name"));
      Sound.Type sound$type = this.getType(p_119836_, Sound.Type.FILE);
      float f = GsonHelper.getAsFloat(p_119836_, "volume", 1.0F);
      Validate.isTrue(f > 0.0F, "Invalid volume", new Object[0]);
      float f1 = GsonHelper.getAsFloat(p_119836_, "pitch", 1.0F);
      Validate.isTrue(f1 > 0.0F, "Invalid pitch", new Object[0]);
      int i = GsonHelper.getAsInt(p_119836_, "weight", 1);
      Validate.isTrue(i > 0, "Invalid weight", new Object[0]);
      boolean flag = GsonHelper.getAsBoolean(p_119836_, "preload", false);
      boolean flag1 = GsonHelper.getAsBoolean(p_119836_, "stream", false);
      int j = GsonHelper.getAsInt(p_119836_, "attenuation_distance", 16);
      return new Sound(identifier, ConstantFloat.of(f), ConstantFloat.of(f1), i, sound$type, flag1, flag, j);
   }

   private Sound.Type getType(JsonObject p_119833_, Sound.Type p_119834_) {
      Sound.Type sound$type = p_119834_;
      if (p_119833_.has("type")) {
         sound$type = Sound.Type.getByName(GsonHelper.getAsString(p_119833_, "type"));
         Objects.requireNonNull(sound$type, "Invalid type");
      }

      return sound$type;
   }
}
