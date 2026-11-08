package com.elfmcys.yesstevemodel.geckolib3.geo.render.built;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.resource.models.GeometryDescription;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import rip.ysm.gpu.GpuRenderPath;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Bedrock的.geo模型文件
 */
public class GeoModel {

    @NotNull
    public final List<GeoBone> bones;

    @NotNull
    public final IntList leftHandIds;

    @NotNull
    public final IntList rightHandIds;

    @NotNull
    public final IntList elytraIds;

    @NotNull
    public final IntList tacPistolIds;

    @NotNull
    public final IntList tacRifleIds;

    @NotNull
    public final IntList leftWaistIds;

    @NotNull
    public final IntList rightWaistIds;

    @NotNull
    public final IntList leftShoulderIds;

    @NotNull
    public final IntList rightShoulderIds;

    @NotNull
    public final IntList bladeIds;

    @NotNull
    public final IntList sheathIds;

    @NotNull
    public final IntList headIds;

    @NotNull
    public final IntList backpackIds;

    public final boolean hasCustomLeftHand;

    public final boolean hasCustomRightHand;

    public final boolean hasCustomLimbs;

    @NotNull
    private final GeometryDescription properties;

    public final float[] boneTransformData;

    private boolean[] translucentTexture;

    @NotNull
    public final List<IntList> extraLeftHandGroups = new ObjectArrayList<>();

    @NotNull
    public final List<IntList> extraRightHandGroups = new ObjectArrayList<>();

    @NotNull
    public final List<IntList> passengerGroups = new ObjectArrayList<>();

    public List<BakedBone> bakedBones;

    public static class BakedBone {
        public String name;
        public boolean glow;
        public int parentIdx = -1;
        public float pivotX, pivotY, pivotZ;
        public float rotX, rotY, rotZ;
        public List<BakedCube> cubes = new ObjectArrayList<>();
        public int partMask;
    }

    public static class BakedCube {
        public boolean cullable = false;
        //        public float pivotX, pivotY, pivotZ;
//        public float rotX, rotY, rotZ;
        public List<BakedQuad> quads = new ObjectArrayList<>();
    }

    public static class BakedQuad {
        public final float[] positions = new float[12];
        public final float[] uvs = new float[8];
        public final float[] normal = new float[3];
        public boolean isTranslucent;
    }

//    static {
//        System.load("test.dll");
//    }

    public long nativeModelHandle = 0;

    public long gpuMeshHandle = 0;

    public static void initSIMD() {
        try {
            String bufferName = null;
            String verticesName = null;
            String nextElementByteName = null;
            String ensureCapacityName = null;
            String modeName = null;


            String classPath = "/com/elfmcys/yesstevemodel/mixin/client/BufferBuilderMixin.class";
            InputStream is = GeoModel.class.getResourceAsStream(classPath);

            if (is == null) {
                YesSteveModel.LOGGER.error("[YSM] Could not find Mixin class resource!");
                return;
            }

            ClassReader classReader = new ClassReader(is); //客户端环境没法加载mixin类，只能这样了
            ClassNode classNode = new ClassNode();
            classReader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            is.close();


            String targetAnnotationDesc = "Lrip/ysm/annotations/BufferBuilderMapping;";

            BiFunction<List<AnnotationNode>, String, String> getAnnotationValue = (annotations, targetDesc) -> {
                if (annotations == null) return null;
                for (AnnotationNode ann : annotations) {
                    if (targetDesc.equals(ann.desc) && ann.values != null) {
                        for (int i = 0; i < ann.values.size(); i += 2) {
                            if ("value".equals(ann.values.get(i))) {
                                return (String) ann.values.get(i + 1);
                            }
                        }
                    }
                }
                return null;
            };


            BiFunction<List<AnnotationNode>, List<AnnotationNode>, String> extractMappingId = (visibleAnns, invisibleAnns) -> {
                String id = getAnnotationValue.apply(visibleAnns, targetAnnotationDesc);
                return id != null ? id : getAnnotationValue.apply(invisibleAnns, targetAnnotationDesc);
            };

            for (FieldNode field : classNode.fields) {
                String id = extractMappingId.apply(field.visibleAnnotations, field.invisibleAnnotations);
                if (id != null) {
                    switch (id) {
                        case "buffer_builder_buffer": bufferName = field.name; break;
                        case "buffer_builder_vertices": verticesName = field.name; break;
                        case "buffer_builder_nextElementByte": nextElementByteName = field.name; break;
                        case "buffer_builder_mode": modeName = field.name; break;
                    }
                }
            }

            for (MethodNode method : classNode.methods) {
                String id = extractMappingId.apply(method.visibleAnnotations, method.invisibleAnnotations);
                if ("buffer_builder_ensureCapacity".equals(id)) {
                    ensureCapacityName = method.name;
                }
            }

            YesSteveModel.LOGGER.info("[YSM] Dynamic Mapping Loaded: buffer={}, vertices={}, nextElementByte={}, mode={}, ensureCapacity={}",
                    bufferName, verticesName, nextElementByteName, modeName, ensureCapacityName);

            nInitSIMD(
                    BufferBuilder.class,
                    bufferName,
                    verticesName,
                    nextElementByteName,
                    ensureCapacityName,
                    modeName,
                    VertexFormat.Mode.class
            );
        } catch (Throwable ex) {
            YesSteveModel.LOGGER.error("[YSM] Failed to initialize SIMD mappings, fast vertex building will not work.", ex);
        }
    }

    private static native void nInitSIMD(
            Class<?> bufferBuilderClass,
            String bufferName,
            String verticesName,
            String nextElementByteName,
            String ensureCapacityName,
            String modeName,
            Class<?> vertexFormatClass
    );

    public static native long nInitModelCache(ByteBuffer buffer);

    public static native void nDestroyModelCache(long handle);

    public static native void nComputeModelVertices(
            long handle,
            Object vertexConsumer,
            float[] matrixArray,
            float[] animArray,
            float[] stateArray,
            int renderPartMask,
            int packedLight,
            int packedOverlay,
            float r, float g, float b, float a
    );

    public static native long nBuildGpuMesh(ByteBuffer buffer, int[] outMeta);

    public static native ByteBuffer nGetGpuMeshVertexBuffer(long pointer);

    public static native ByteBuffer nGetGpuMeshIndexBuffer(long pointer);

    public static native void nReleaseGpuMeshScratch(long pointer);

    public static native void nFreeGpuMesh(long pointer);

    public static native void nComputeBoneMatrices(long pointer, float[] rootPose, float[] rootNormal, float[] anim, int packedLight, ByteBuffer outBoneBuffer);

    public static native void nComputeBoneMatricesLocal(long handle, float[] animArray, int packedLight, ByteBuffer outBoneBuffer);

    public void buildNativeCache() {
        if (bakedBones == null || bakedBones.isEmpty()) return;

        int totalBones = bakedBones.size();
        int totalCubes = 0;
        int totalQuads = 0;

        for (BakedBone bone : bakedBones) {
            totalCubes += bone.cubes.size();
            for (BakedCube cube : bone.cubes) {
                totalQuads += cube.quads.size();
            }
        }

        int initBufferSize = 4 + (totalBones * 25) + (totalCubes * 5) + (totalQuads * 93);
        ByteBuffer buffer = ByteBuffer.allocateDirect(initBufferSize).order(ByteOrder.nativeOrder());

        buffer.putInt(bakedBones.size());
        for (BakedBone bone : bakedBones) {
            buffer.putInt(bone.parentIdx);
            buffer.putInt(bone.partMask);
            buffer.put((byte) (bone.glow ? 1 : 0));
            buffer.putFloat(bone.pivotX);
            buffer.putFloat(bone.pivotY);
            buffer.putFloat(bone.pivotZ);

            buffer.putInt(bone.cubes.size());
            for (BakedCube cube : bone.cubes) {
                buffer.put((byte) (cube.cullable ? 1 : 0));
                buffer.putInt(cube.quads.size());
                for (BakedQuad quad : cube.quads) {
                    buffer.put((byte) (quad.isTranslucent ? 1 : 0)); //是否含半透明
                    for (float position : quad.positions) {
                        buffer.putFloat(position);
                    }
                    for (float uv : quad.uvs) {
                        buffer.putFloat(uv);
                    }
                    // 3 floats *4=12
                    buffer.putFloat(quad.normal[0]);
                    buffer.putFloat(quad.normal[1]);
                    buffer.putFloat(quad.normal[2]);
                }
            }
        }

        buffer.position(0);
        this.nativeModelHandle = nInitModelCache(buffer);
    }

    public void freeNativeCache() {
        if (nativeModelHandle != 0) {
            nDestroyModelCache(nativeModelHandle);
            nativeModelHandle = 0;
        }
        if (gpuMeshHandle != 0) {
            GpuRenderPath.disposeMesh(this);
        }
    }

    public GeoModel(GeoBone[] geoBones, String[][] strArr, boolean[] zArr, @NotNull GeometryDescription properties, boolean[] zArr2) {
        this.bones = ObjectLists.unmodifiable(ObjectArrayList.wrap(geoBones));
        this.leftHandIds = resolveBoneIds(strArr[0]);
        this.rightHandIds = resolveBoneIds(strArr[1]);
        this.elytraIds = resolveBoneIds(strArr[2]);
        this.tacPistolIds = resolveBoneIds(strArr[3]);
        this.tacRifleIds = resolveBoneIds(strArr[4]);
        this.leftWaistIds = resolveBoneIds(strArr[5]);
        this.rightWaistIds = resolveBoneIds(strArr[6]);
        this.leftShoulderIds = resolveBoneIds(strArr[7]);
        this.rightShoulderIds = resolveBoneIds(strArr[8]);
        this.bladeIds = resolveBoneIds(strArr[9]);
        this.sheathIds = resolveBoneIds(strArr[10]);
        this.headIds = resolveBoneIds(strArr[11]);
        this.backpackIds = resolveBoneIds(strArr[12]);
        for (int i = 13; i <= 19; i++) {
            String[] strArr2 = strArr[i];
            if (strArr2.length > 0) {
                this.extraLeftHandGroups.add(resolveBoneIds(strArr2));
            }
        }
        for (int i = 20; i <= 26; i++) {
            String[] strArr3 = strArr[i];
            if (strArr3.length > 0) {
                this.extraRightHandGroups.add(resolveBoneIds(strArr3));
            }
        }
        for (int i = 27; i <= 34; i++) {
            String[] strArr4 = strArr[i];
            if (strArr4.length > 0) {
                this.passengerGroups.add(resolveBoneIds(strArr4));
            }
        }
        this.hasCustomLeftHand = zArr[0]; // has left hand?
        this.hasCustomRightHand = zArr[1]; // has right hand?
        this.hasCustomLimbs = zArr[2]; // has background
        this.translucentTexture = zArr2;
        this.properties = properties;
        this.boneTransformData = new AnimatedGeoModel(this).getMatrixData();
    }

    private static IntList resolveBoneIds(String[] strArr) {
        IntArrayList intArrayList = new IntArrayList(strArr.length);
        for (String str : strArr) {
            intArrayList.add(StringPool.computeIfAbsent(str));
        }
        return IntLists.unmodifiable(intArrayList);
    }

    @NotNull
    public List<GeoBone> topLevelBones() {
        return this.bones;
    }

    public float[] getBoneTransformData() {
        return this.boneTransformData;
    }

    @NotNull
    public GeometryDescription getProperties() {
        return this.properties;
    }

    public boolean isTranslucentTexture(int i) {
        if (i < 0 || i >= this.translucentTexture.length) {
            return false;
        }
        return this.translucentTexture[i];
    }

    public void setTranslucentTexture(int i, boolean translucent) {
        ensureTranslucentTextureCapacity(i);
        this.translucentTexture[i] = translucent;
    }

    private void ensureTranslucentTextureCapacity(int maxIndex) {
        if (maxIndex >= this.translucentTexture.length) {
            boolean[] expanded = new boolean[maxIndex + 1];
            System.arraycopy(this.translucentTexture, 0, expanded, 0, this.translucentTexture.length);
            this.translucentTexture = expanded;
        }
    }
}
