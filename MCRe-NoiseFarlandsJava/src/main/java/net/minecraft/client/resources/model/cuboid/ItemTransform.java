package net.minecraft.client.resources.model.cuboid;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Type;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public record ItemTransform(Vector3fc rotation, Vector3fc translation, Vector3fc scale) {
    public static final ItemTransform NO_TRANSFORM = new ItemTransform(new Vector3f(), new Vector3f(), new Vector3f(1.0F, 1.0F, 1.0F));

    public void apply(final boolean applyLeftHandFix, final PoseStack.Pose pose) {
        if (this == NO_TRANSFORM) {
            pose.translate(-0.5F, -0.5F, -0.5F);
        } else {
            float translationX;
            float rotY;
            float rotZ;
            if (applyLeftHandFix) {
                translationX = -this.translation.x();
                rotY = -this.rotation.y();
                rotZ = -this.rotation.z();
            } else {
                translationX = this.translation.x();
                rotY = this.rotation.y();
                rotZ = this.rotation.z();
            }

            pose.translate(translationX, this.translation.y(), this.translation.z());
            pose.rotate(
                new Quaternionf()
                    .rotationXYZ(this.rotation.x() * (float) (Math.PI / 180.0), rotY * (float) (Math.PI / 180.0), rotZ * (float) (Math.PI / 180.0))
            );
            pose.scale(this.scale.x(), this.scale.y(), this.scale.z());
            pose.translate(-0.5F, -0.5F, -0.5F);
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected static class Deserializer implements JsonDeserializer<ItemTransform> {
        private static final Vector3fc DEFAULT_ROTATION = new Vector3f();
        private static final Vector3fc DEFAULT_TRANSLATION = new Vector3f();
        private static final Vector3fc DEFAULT_SCALE = new Vector3f(1.0F, 1.0F, 1.0F);
        public static final float MAX_TRANSLATION = 5.0F;
        public static final float MAX_SCALE = 4.0F;

        public ItemTransform deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = json.getAsJsonObject();
            Vector3f rotation = getVector3f(object, "rotation", DEFAULT_ROTATION);
            Vector3f translation = getVector3f(object, "translation", DEFAULT_TRANSLATION);
            translation.mul(0.0625F);
            translation.set(Mth.clamp(translation.x, -5.0F, 5.0F), Mth.clamp(translation.y, -5.0F, 5.0F), Mth.clamp(translation.z, -5.0F, 5.0F));
            Vector3f scale = getVector3f(object, "scale", DEFAULT_SCALE);
            scale.set(Mth.clamp(scale.x, -4.0F, 4.0F), Mth.clamp(scale.y, -4.0F, 4.0F), Mth.clamp(scale.z, -4.0F, 4.0F));
            return new ItemTransform(rotation, translation, scale);
        }

        private static Vector3f getVector3f(final JsonObject object, final String key, final Vector3fc def) {
            if (!object.has(key)) {
                return new Vector3f(def);
            }

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