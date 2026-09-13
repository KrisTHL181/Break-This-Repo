package com.mojang.blaze3d.vertex;

import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public interface VertexConsumer {
    VertexConsumer addVertex(float x, float y, float z);

    VertexConsumer setColor(int r, int g, int b, int a);

    VertexConsumer setColor(int color);

    VertexConsumer setUv(float u, float v);

    VertexConsumer setUv1(int u, int v);

    VertexConsumer setUv2(int u, int v);

    VertexConsumer setNormal(float x, float y, float z);

    VertexConsumer setLineWidth(float width);

    default void addVertex(
        final float x,
        final float y,
        final float z,
        final int color,
        final float u,
        final float v,
        final int overlayCoords,
        final int lightCoords,
        final float nx,
        final float ny,
        final float nz
    ) {
        this.addVertex(x, y, z);
        this.setColor(color);
        this.setUv(u, v);
        this.setOverlay(overlayCoords);
        this.setLight(lightCoords);
        this.setNormal(nx, ny, nz);
    }

    default VertexConsumer setColor(final float r, final float g, final float b, final float a) {
        return this.setColor((int)(r * 255.0F), (int)(g * 255.0F), (int)(b * 255.0F), (int)(a * 255.0F));
    }

    default VertexConsumer setLight(final int packedLightCoords) {
        return this.setUv2(packedLightCoords & 65535, packedLightCoords >> 16 & 65535);
    }

    default VertexConsumer setOverlay(final int packedOverlayCoords) {
        return this.setUv1(packedOverlayCoords & 65535, packedOverlayCoords >> 16 & 65535);
    }

    default void putBlockBakedQuad(final float x, final float y, final float z, final BakedQuad quad, final QuadInstance instance) {
        Vector3fc normal = quad.direction().getUnitVec3f();
        int lightEmission = quad.materialInfo().lightEmission();

        for (int vertex = 0; vertex < 4; vertex++) {
            Vector3fc pos = quad.position(vertex);
            long packedUv = quad.packedUV(vertex);
            int vertexColor = instance.getColor(vertex);
            int light = instance.getLightCoordsWithEmission(vertex, lightEmission);
            float u = UVPair.unpackU(packedUv);
            float v = UVPair.unpackV(packedUv);
            this.addVertex(pos.x() + x, pos.y() + y, pos.z() + z, vertexColor, u, v, instance.overlayCoords(), light, normal.x(), normal.y(), normal.z());
        }
    }

    default void putBakedQuad(final PoseStack.Pose pose, final BakedQuad quad, final QuadInstance instance) {
        Vector3fc normalVec = quad.direction().getUnitVec3f();
        Matrix4f matrix = pose.pose();
        Vector3f normal = pose.transformNormal(normalVec, new Vector3f());
        int lightEmission = quad.materialInfo().lightEmission();

        for (int vertex = 0; vertex < 4; vertex++) {
            Vector3fc position = quad.position(vertex);
            long packedUv = quad.packedUV(vertex);
            int vertexColor = instance.getColor(vertex);
            int light = instance.getLightCoordsWithEmission(vertex, lightEmission);
            Vector3f pos = matrix.transformPosition(position, new Vector3f());
            float u = UVPair.unpackU(packedUv);
            float v = UVPair.unpackV(packedUv);
            this.addVertex(pos.x(), pos.y(), pos.z(), vertexColor, u, v, instance.overlayCoords(), light, normal.x(), normal.y(), normal.z());
        }
    }

    default VertexConsumer addVertex(final Vector3fc position) {
        return this.addVertex(position.x(), position.y(), position.z());
    }

    default VertexConsumer addVertex(final PoseStack.Pose pose, final Vector3fc position) {
        return this.addVertex(pose, position.x(), position.y(), position.z());
    }

    default VertexConsumer addVertex(final PoseStack.Pose pose, final float x, final float y, final float z) {
        return this.addVertex(pose.pose(), x, y, z);
    }

    default VertexConsumer addVertex(final Matrix4fc pose, final float x, final float y, final float z) {
        Vector3f pos = pose.transformPosition(x, y, z, new Vector3f());
        return this.addVertex(pos.x(), pos.y(), pos.z());
    }

    default VertexConsumer addVertexWith2DPose(final Matrix3x2fc pose, final float x, final float y) {
        Vector2f pos = pose.transformPosition(x, y, new Vector2f());
        return this.addVertex(pos.x(), pos.y(), 0.0F);
    }

    default VertexConsumer setNormal(final PoseStack.Pose pose, final float x, final float y, final float z) {
        Vector3f normal = pose.transformNormal(x, y, z, new Vector3f());
        return this.setNormal(normal.x(), normal.y(), normal.z());
    }

    default VertexConsumer setNormal(final PoseStack.Pose pose, final Vector3fc normal) {
        return this.setNormal(pose, normal.x(), normal.y(), normal.z());
    }
}