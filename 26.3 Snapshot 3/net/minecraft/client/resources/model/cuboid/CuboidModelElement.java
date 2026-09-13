package net.minecraft.client.resources.model.cuboid;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public record CuboidModelElement(
   Vector3fc from,
   Vector3fc to,
   Map<Direction, CuboidFace> faces,
   @Nullable CuboidRotation rotation,
   @Nullable Direction shadeDirectionOverride,
   int lightEmission
) {
   private static final boolean DEFAULT_RESCALE = false;
   private static final float MIN_EXTENT = -16.0F;
   private static final float MAX_EXTENT = 32.0F;

   public CuboidModelElement(final Vector3fc from, final Vector3fc to, final Map<Direction, CuboidFace> faces) {
      this(from, to, faces, null, null, 0);
   }

   protected static class Deserializer implements JsonDeserializer<CuboidModelElement> {
      private static final int DEFAULT_LIGHT_EMISSION = 0;
      private static final String FIELD_SHADE_DIRECTION_OVERRIDE = "shade_direction_override";
      private static final String FIELD_LIGHT_EMISSION = "light_emission";
      private static final String FIELD_ROTATION = "rotation";
      private static final String FIELD_ORIGIN = "origin";
      private static final String FIELD_ANGLE = "angle";
      private static final String FIELD_X = "x";
      private static final String FIELD_Y = "y";
      private static final String FIELD_Z = "z";
      private static final String FIELD_AXIS = "axis";
      private static final String FIELD_RESCALE = "rescale";
      private static final String FIELD_FACES = "faces";
      private static final String FIELD_TO = "to";
      private static final String FIELD_FROM = "from";

      public CuboidModelElement deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
         JsonObject object = json.getAsJsonObject();
         Vector3f from = getPosition(object, "from");
         Vector3f to = getPosition(object, "to");
         CuboidRotation rotation = this.getRotation(object);
         Map<Direction, CuboidFace> faces = this.getFaces(context, object);
         Direction shadeDirectionOverride = this.getShadeDirectionOverride(object);
         int lightEmission = 0;
         if (object.has("light_emission")) {
            boolean isNumber = GsonHelper.isNumberValue(object, "light_emission");
            if (isNumber) {
               lightEmission = GsonHelper.getAsInt(object, "light_emission");
            }

            if (!isNumber || lightEmission < 0 || lightEmission > 15) {
               throw new JsonParseException("Expected 'light_emission' to be an Integer between (inclusive) 0 and 15");
            }
         }

         return new CuboidModelElement(from, to, faces, rotation, shadeDirectionOverride, lightEmission);
      }

      private @Nullable Direction getShadeDirectionOverride(final JsonObject object) {
         if (!object.has("shade_direction_override")) {
            return null;
         } else {
            String shadeDirectionName = GsonHelper.getAsString(object, "shade_direction_override");
            Direction shadeDirectionOverride = Direction.byName(shadeDirectionName);
            if (shadeDirectionOverride == null) {
               throw new JsonParseException("Unknown shade direction override: " + shadeDirectionName);
            } else {
               return shadeDirectionOverride;
            }
         }
      }

      private @Nullable CuboidRotation getRotation(final JsonObject object) {
         if (!object.has("rotation")) {
            return null;
         }

         JsonObject rotationObject = GsonHelper.getAsJsonObject(object, "rotation");
         Vector3f origin = getVector3f(rotationObject, "origin");
         origin.mul(0.0625F);
         CuboidRotation.RotationValue rotationValue;
         if (!rotationObject.has("axis") && !rotationObject.has("angle")) {
            if (!rotationObject.has("x") && !rotationObject.has("y") && !rotationObject.has("z")) {
               throw new JsonParseException("Missing rotation value, expected either 'axis' and 'angle' or 'x', 'y' and 'z'");
            }

            float x = GsonHelper.getAsFloat(rotationObject, "x", 0.0F);
            float y = GsonHelper.getAsFloat(rotationObject, "y", 0.0F);
            float z = GsonHelper.getAsFloat(rotationObject, "z", 0.0F);
            rotationValue = new CuboidRotation.EulerXYZRotation(x, y, z);
         } else {
            Direction.Axis axis = this.getAxis(rotationObject);
            float angle = GsonHelper.getAsFloat(rotationObject, "angle");
            rotationValue = new CuboidRotation.SingleAxisRotation(axis, angle);
         }

         boolean rescale = GsonHelper.getAsBoolean(rotationObject, "rescale", false);
         return new CuboidRotation(origin, rotationValue, rescale);
      }

      private Direction.Axis getAxis(final JsonObject object) {
         String axisName = GsonHelper.getAsString(object, "axis");
         Direction.Axis axis = Direction.Axis.byName(axisName.toLowerCase(Locale.ROOT));
         if (axis == null) {
            throw new JsonParseException("Invalid rotation axis: " + axisName);
         } else {
            return axis;
         }
      }

      private Map<Direction, CuboidFace> getFaces(final JsonDeserializationContext context, final JsonObject object) {
         Map<Direction, CuboidFace> faces = this.filterNullFromFaces(context, object);
         if (faces.isEmpty()) {
            throw new JsonParseException("Expected between 1 and 6 unique faces, got 0");
         } else {
            return faces;
         }
      }

      private Map<Direction, CuboidFace> filterNullFromFaces(final JsonDeserializationContext context, final JsonObject object) {
         Map<Direction, CuboidFace> result = Maps.newEnumMap(Direction.class);
         JsonObject faceObjects = GsonHelper.getAsJsonObject(object, "faces");

         for (Entry<String, JsonElement> entry : faceObjects.entrySet()) {
            Direction direction = this.getFacing(entry.getKey());
            result.put(direction, (CuboidFace)context.deserialize(entry.getValue(), CuboidFace.class));
         }

         return result;
      }

      private Direction getFacing(final String name) {
         Direction direction = Direction.byName(name);
         if (direction == null) {
            throw new JsonParseException("Unknown facing: " + name);
         } else {
            return direction;
         }
      }

      private static Vector3f getPosition(final JsonObject object, final String key) {
         Vector3f from = getVector3f(object, key);
         if (!(from.x() < -16.0F) && !(from.y() < -16.0F) && !(from.z() < -16.0F) && !(from.x() > 32.0F) && !(from.y() > 32.0F) && !(from.z() > 32.0F)) {
            return from;
         } else {
            throw new JsonParseException("'" + key + "' specifier exceeds the allowed boundaries: " + from);
         }
      }

      private static Vector3f getVector3f(final JsonObject object, final String key) {
         JsonArray vecArray = GsonHelper.getAsJsonArray(object, key);
         if (vecArray.size() != 3) {
            throw new JsonParseException("Expected 3 " + key + " values, found: " + vecArray.size());
         }

         float[] elements = new float[3];

         for (int i = 0; i < elements.length; i++) {
            elements[i] = GsonHelper.convertToFloat(vecArray.get(i), key + "[" + i + "]");
         }

         return new Vector3f(elements[0], elements[1], elements[2]);
      }
   }
}
