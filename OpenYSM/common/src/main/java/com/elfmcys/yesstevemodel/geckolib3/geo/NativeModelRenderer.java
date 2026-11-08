

package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.NativeLibLoader;
import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.elfmcys.yesstevemodel.util.log.ChatLogger;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import rip.ysm.compat.oculus.OculusCompat;
import rip.ysm.compat.optifine.OptiFineDetector;
import rip.ysm.gpu.GpuCapability;
import rip.ysm.gpu.GpuRenderPath;
import rip.ysm.gpu.IrisRenderPath;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class NativeModelRenderer {
    private static final Matrix4f projectionModelViewMatrix = new Matrix4f();

    public static void renderMesh(VertexConsumer buffer, PoseStack.Pose pose, GeoModel model, float[] boneParams, float[] stateBuffer, int textureIndex, int renderPartMask, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        renderMesh(buffer, pose, model, boneParams, stateBuffer, textureIndex, renderPartMask, packedLight, packedOverlay, red, green, blue, alpha, null);
    }

    public static void renderMesh(VertexConsumer buffer, PoseStack.Pose pose, GeoModel model, float[] boneParams, float[] stateBuffer, int textureIndex, int renderPartMask, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, net.minecraft.resources.ResourceLocation textureLocation) {
        OculusCompat.updatePBRState();
        RenderSystem.getProjectionMatrix().mul(RenderSystem.getModelViewMatrix(), projectionModelViewMatrix);
        boolean isPreview = ModelPreviewRenderer.isPreview() || ModelPreviewRenderer.isExtraPlayer();

        if (textureLocation != null && NativeLibLoader.isLoaded() && !GeneralConfig.USE_COMPATIBILITY_RENDERER.get() && GeneralConfig.USE_GPU_RENDERER.get()) {

            if(!GpuCapability.isAvailable())
            {
                ChatLogger.INSTANCE.logFormatted("Disabled GPU renderer for: " + GpuCapability.getReason());
                GeneralConfig.USE_GPU_RENDERER.set(false);
                return;
            }

            if (OculusCompat.isShaderPackInUse() && !isPreview) {
                if (IrisRenderPath.tryRender(model, pose, boneParams, renderPartMask, packedLight, packedOverlay, red, green, blue, alpha, textureLocation)) {
                    return;
                }
            } else {
                if (GpuRenderPath.tryRender(model, pose, boneParams, stateBuffer, textureIndex, renderPartMask, packedLight, packedOverlay, red, green, blue, alpha, textureLocation)) {
                    return;
                }
            }
        }

        if (NativeLibLoader.isLoaded() && !GeneralConfig.USE_COMPATIBILITY_RENDERER.get()) { // WIP: SIMD MODEL RENDER
            nativeRenderModel(
                    buffer,
                    pose,
                    projectionModelViewMatrix,
                    OptiFineDetector.isOptifinePresent(),
                    model,
                    boneParams,
                    stateBuffer,
                    textureIndex,
                    renderPartMask,
                    packedLight,
                    packedOverlay,
                    red, green, blue, alpha,
                    isPreview
            );
        } else {
            renderModel(
                    buffer,
                    pose,
                    projectionModelViewMatrix,
                    OptiFineDetector.isOptifinePresent(),
                    model,
                    boneParams,
                    stateBuffer,
                    textureIndex,
                    renderPartMask,
                    packedLight,
                    packedOverlay,
                    red, green, blue, alpha,
                    isPreview
            );
        }
    }

    public static void renderModel(
            VertexConsumer vertexConsumer,
            PoseStack.Pose pose,
            Matrix4f projectionModelViewMatrix,
            boolean isCompatMode,
            GeoModel mesh,
            float[] boneParams,
            float[] stateBuffer,
            int textureIndex, int renderPartMask,
            int packedLight, int packedOverlay,
            float r, float g, float b, float a,
            boolean isPreview) {

        if (mesh.bakedBones == null || mesh.bakedBones.isEmpty()) return;

        // TODO: 修復GC壓力
        Matrix4f rootPoseMat = pose.pose();
        Matrix3f rootNormalMC = pose.normal();
        Matrix4f projMat = RenderSystem.getProjectionMatrix();

        Matrix4f identityMat = new Matrix4f();
        Matrix4f globalBoneMat = new Matrix4f();
        Matrix4f projBoneMat = new Matrix4f();
        Matrix3f localNormalMat = new Matrix3f();
        Matrix3f globalNormalMat = new Matrix3f();

        Vector4f p1 = new Vector4f();
        Vector4f p2 = new Vector4f();
        Vector4f p3 = new Vector4f();
        Vector4f tempPos = new Vector4f();
        Vector3f tempNorm = new Vector3f();
        Matrix4f[] boneLocalTransforms = new Matrix4f[mesh.bakedBones.size()];
        boolean[] boneVisible = new boolean[mesh.bakedBones.size()];

        for (int i = 0; i < mesh.bakedBones.size(); i++) {
            calculateBoneMatrix(i, mesh.bakedBones, boneParams, boneLocalTransforms, boneVisible, identityMat, stateBuffer);
        }

        for (int i = 0; i < mesh.bakedBones.size(); i++) {
            if (!boneVisible[i]) {
                continue;
            }

            GeoModel.BakedBone bone = mesh.bakedBones.get(i);
            if (renderPartMask != 0 && bone.partMask != renderPartMask && bone.partMask != 3) {
                continue;
            }

            Matrix4f localBoneMat = boneLocalTransforms[i];
            globalBoneMat.set(rootPoseMat).mul(localBoneMat);
            projBoneMat.set(projMat).mul(globalBoneMat);

            // 法線全域矩陣
            localBoneMat.normal(localNormalMat);
            globalNormalMat.set(rootNormalMC).mul(localNormalMat);

            int currentPackedLight = bone.glow ? LightTexture.pack(15, 15) : packedLight;

            for (GeoModel.BakedCube cube : bone.cubes) {
                for (GeoModel.BakedQuad quad : cube.quads) {
                    if (cube.cullable) {
                        p1.set(quad.positions[0], quad.positions[1], quad.positions[2], 1.0f).mul(projBoneMat);
                        p2.set(quad.positions[3], quad.positions[4], quad.positions[5], 1.0f).mul(projBoneMat);
                        p3.set(quad.positions[6], quad.positions[7], quad.positions[8], 1.0f).mul(projBoneMat);
                        float det = p1.x() * (p2.y() * p3.w() - p3.y() * p2.w()) - p2.x() * (p1.y() * p3.w() - p3.y() * p1.w()) + p3.x() * (p1.y() * p2.w() - p2.y() * p1.w());
                        if (det <= 0.0f) {
                            continue;
                        }
                    }
                    tempNorm.set(quad.normal[0], quad.normal[1], quad.normal[2]).mul(globalNormalMat).normalize();
                    for (int v = 0; v < 4; v++) {
                        int positionOffset = v * 3;
                        int uvOffset = v * 2;
                        tempPos.set(quad.positions[positionOffset], quad.positions[positionOffset + 1], quad.positions[positionOffset + 2], 1.0f).mul(globalBoneMat);
                        vertexConsumer.vertex(tempPos.x(), tempPos.y(), tempPos.z(), r, g, b, a, quad.uvs[uvOffset], quad.uvs[uvOffset + 1], packedOverlay, currentPackedLight, tempNorm.x(), tempNorm.y(), tempNorm.z());
                    }
                }
            }
        }
    }

    private static Matrix4f calculateBoneMatrix(int idx, java.util.List<GeoModel.BakedBone> bones, float[] boneParams, Matrix4f[] cache, boolean[] visibleCache, Matrix4f rootPose, float[] stateBuffer) {
        if (cache[idx] != null) return cache[idx];

        GeoModel.BakedBone bone = bones.get(idx);
        Matrix4f parentMatrix = rootPose;
        boolean isVisible = true;

        if (bone.parentIdx != -1) {
            parentMatrix = calculateBoneMatrix(bone.parentIdx, bones, boneParams, cache, visibleCache, rootPose, stateBuffer);
            // 如果父骨骼不可見，子骨骼必然跟著不可見
            if (!visibleCache[bone.parentIdx]) {
                isVisible = false;
            }
        }

        Matrix4f localMat = new Matrix4f(parentMatrix);

        int pOffset = idx * 12;
        float animRx = boneParams[pOffset];
        float animRy = boneParams[pOffset + 1];
        float animRz = boneParams[pOffset + 2];
        float animTx = boneParams[pOffset + 3];
        float animTy = boneParams[pOffset + 4];
        float animTz = boneParams[pOffset + 5];
        float animSx = boneParams[pOffset + 6];
        float animSy = boneParams[pOffset + 7];
        float animSz = boneParams[pOffset + 8];

        float unk1 = boneParams[pOffset + 9];
        float unk2 = boneParams[pOffset + 10];
        float unk3 = boneParams[pOffset + 11];

        if (unk1 != 0.0F && unk2 != 0.0F && unk3 != 0.0F) {
            //"".hashCode();
        }

        if (animSx == 0.0f && animSy == 0.0f && animSz == 0.0f) {
            isVisible = false;
        }/* else if (unk1 == 1 || unk2 == 1) isVisible = false;*/

        localMat.translate(
                (bone.pivotX - animTx) * 0.0625f,
                (bone.pivotY + animTy) * 0.0625f,
                (bone.pivotZ + animTz) * 0.0625f
        );
        localMat.rotateZ(animRz);
        localMat.rotateY(animRy);
        localMat.rotateX(animRx);

//        if (bone.name.equals("gun")) {
//            //"".hashCode();
//        }

        if (animSx != 1.0f || animSy != 1.0f || animSz != 1.0f) {
            localMat.scale(animSx, animSy, animSz);
        }

        if (unk3 == 1.0F && stateBuffer != null && isVisible) {
            int offset = idx * 4;
            // bone pivot abs
            if (offset + 2 < stateBuffer.length) {
                stateBuffer[offset + 0] =-localMat.m30() * 16;
                stateBuffer[offset + 1] = localMat.m31() * 16;
                stateBuffer[offset + 2] = localMat.m32() * 16;
            }
        }

        localMat.translate(-bone.pivotX / 16f, -bone.pivotY / 16f, -bone.pivotZ / 16f);

        cache[idx] = localMat;
        visibleCache[idx] = isVisible; // 保存當前骨骼的可見性
        return localMat;
    }

    private static final float[] matrixTransferArray = new float[48];
    @SuppressWarnings("unused") // TODO: native中直接往VertexConsumer中的buffer写入顶点
    public static void submitVertices(Object v, int vertexCount, ByteBuffer fBuf, ByteBuffer iBuf) {
        FloatBuffer f = fBuf.order(ByteOrder.nativeOrder()).asFloatBuffer();
        IntBuffer in = iBuf.order(ByteOrder.nativeOrder()).asIntBuffer();
        VertexConsumer vc = (VertexConsumer) v;
        int fIdx = 0, iIdx = 0;
        for (int n = 0; n < vertexCount; n++) {
            vc.vertex(
                    // position
                    f.get(fIdx),     f.get(fIdx + 1), f.get(fIdx + 2),
                    // rgba
                    f.get(fIdx + 3), f.get(fIdx + 4), f.get(fIdx + 5), f.get(fIdx + 6),
                    // uv
                    f.get(fIdx + 7), f.get(fIdx + 8),
                    // overlay light
                    in.get(iIdx),    in.get(iIdx + 1),
                    // normal
                    f.get(fIdx + 9), f.get(fIdx + 10), f.get(fIdx + 11)
            );
            fIdx += 12;
            iIdx += 2;
        }
    }


    public static void nativeRenderModel( // TODO:
            VertexConsumer vertexConsumer, PoseStack.Pose pose, Matrix4f projectionModelViewMatrix,
            boolean isCompatMode, GeoModel mesh, float[] boneVertex, float[] stateBuffer,
            int textureIndex, int renderPartMask, int packedLight, int packedOverlay,
            float r, float g, float b, float a, boolean isPreview) {

        if (mesh.nativeModelHandle == 0) return;

        Matrix4f projMat = RenderSystem.getProjectionMatrix();

        pose.pose().get(matrixTransferArray, 0);
        pose.normal().get(matrixTransferArray, 16);
        projMat.get(matrixTransferArray, 32);

        GeoModel.nComputeModelVertices(
                mesh.nativeModelHandle,
                vertexConsumer,
                matrixTransferArray,
                boneVertex,
                stateBuffer,
                renderPartMask,
                packedLight, packedOverlay,
                r, g, b, a
        );
    }
}
