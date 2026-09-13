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
import org.apache.commons.lang3.Validate;

public class SoundEventRegistrationSerializer implements JsonDeserializer<SoundEventRegistration> {
   private static final FloatProvider DEFAULT_FLOAT = ConstantFloat.of(1.0F);

   public SoundEventRegistration deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
      JsonObject object = GsonHelper.convertToJsonObject(json, "entry");
      boolean replace = GsonHelper.getAsBoolean(object, "replace", false);
      String subtitle = GsonHelper.getAsString(object, "subtitle", null);
      List<Sound> sounds = this.getSounds(object);
      return new SoundEventRegistration(sounds, replace, subtitle);
   }

   private List<Sound> getSounds(final JsonObject object) {
      List<Sound> result = Lists.newArrayList();
      if (object.has("sounds")) {
         JsonArray array = GsonHelper.getAsJsonArray(object, "sounds");

         for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (GsonHelper.isStringValue(element)) {
               Identifier name = Identifier.parse(GsonHelper.convertToString(element, "sound"));
               result.add(new Sound(name, DEFAULT_FLOAT, DEFAULT_FLOAT, 1, Sound.Type.FILE, false, false, 16));
            } else {
               result.add(this.getSound(GsonHelper.convertToJsonObject(element, "sound")));
            }
         }
      }

      return result;
   }

   private Sound getSound(final JsonObject object) {
      Identifier name = Identifier.parse(GsonHelper.getAsString(object, "name"));
      Sound.Type type = this.getType(object, Sound.Type.FILE);
      float volume = GsonHelper.getAsFloat(object, "volume", 1.0F);
      Validate.isTrue(volume > 0.0F, "Invalid volume", new Object[0]);
      float pitch = GsonHelper.getAsFloat(object, "pitch", 1.0F);
      Validate.isTrue(pitch > 0.0F, "Invalid pitch", new Object[0]);
      int weight = GsonHelper.getAsInt(object, "weight", 1);
      Validate.isTrue(weight > 0, "Invalid weight", new Object[0]);
      boolean preload = GsonHelper.getAsBoolean(object, "preload", false);
      boolean stream = GsonHelper.getAsBoolean(object, "stream", false);
      int attenuationDistance = GsonHelper.getAsInt(object, "attenuation_distance", 16);
      return new Sound(name, ConstantFloat.of(volume), ConstantFloat.of(pitch), weight, type, stream, preload, attenuationDistance);
   }

   private Sound.Type getType(final JsonObject sound, final Sound.Type fallback) {
      Sound.Type type = fallback;
      if (sound.has("type")) {
         type = Sound.Type.getByName(GsonHelper.getAsString(sound, "type"));
         Objects.requireNonNull(type, "Invalid type");
      }

      return type;
   }
}
