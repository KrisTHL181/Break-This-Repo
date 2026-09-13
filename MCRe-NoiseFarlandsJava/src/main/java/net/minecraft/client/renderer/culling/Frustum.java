package net.minecraft.client.renderer.culling;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class Frustum {
    public static final int OFFSET_STEP = 4;
    private final FrustumIntersection intersection = new FrustumIntersection();
    private final Matrix4f matrix = new Matrix4f();
    private Vector4f viewVector;
    private double camX;
    private double camY;
    private double camZ;

    public Frustum(final Matrix4fc modelView, final Matrix4f projection) {
        this.calculateFrustum(modelView, projection);
    }

    public Frustum(final Frustum frustum) {
        this.set(frustum);
    }

    public void set(final Frustum frustum) {
        this.intersection.set(frustum.matrix);
        this.matrix.set(frustum.matrix);
        this.camX = frustum.camX;
        this.camY = frustum.camY;
        this.camZ = frustum.camZ;
        this.viewVector = frustum.viewVector;
    }

    public Frustum offset(final float offset) {
        this.camX = this.camX + this.viewVector.x * offset;
        this.camY = this.camY + this.viewVector.y * offset;
        this.camZ = this.camZ + this.viewVector.z * offset;
        return this;
    }

    public Frustum offsetToFullyIncludeCameraCube(final int cubeSize) {
        // For extremely large camera coordinates, the original float‑based offset logic can lose precision.
        // If any coordinate exceeds a safe threshold, skip the offset adjustment.
        double maxAbs = Math.max(Math.abs(this.camX), Math.max(Math.abs(this.camY), Math.abs(this.camZ)));
        if (maxAbs > 1.0E9) {
            return this;
        }

        double camX1 = Math.floor(this.camX / cubeSize) * cubeSize;
        double camY1 = Math.floor(this.camY / cubeSize) * cubeSize;
        double camZ1 = Math.floor(this.camZ / cubeSize) * cubeSize;
        double camX2 = Math.ceil(this.camX / cubeSize) * cubeSize;
        double camY2 = Math.ceil(this.camY / cubeSize) * cubeSize;

        // Use the double‑precision cubeInFrustum to avoid float overflow.
        for (double camZ2 = Math.ceil(this.camZ / cubeSize) * cubeSize;
             this.cubeInFrustum(camX1, camY1, camZ1, camX2, camY2, camZ2) != -2;
             this.camZ = this.camZ - this.viewVector.z() * 4.0) {
            this.camX = this.camX - this.viewVector.x() * 4.0;
            this.camY = this.camY - this.viewVector.y() * 4.0;
        }

        return this;
    }

    public void prepare(final double camX, final double camY, final double camZ) {
        this.camX = camX;
        this.camY = camY;
        this.camZ = camZ;
    }

    private void calculateFrustum(final Matrix4fc modelView, final Matrix4f projection) {
        projection.mul(modelView, this.matrix);
        this.intersection.set(this.matrix);
        this.viewVector = this.matrix.transformTranspose(new Vector4f(0.0F, 0.0F, 1.0F, 0.0F));
    }

    public boolean isVisible(final AABB bb) {
        int intersectionResult = this.cubeInFrustum(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
        return intersectionResult == -2 || intersectionResult == -1;
    }

    public int cubeInFrustum(final AABB bb) {
        return this.cubeInFrustum(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
    }

    public int cubeInFrustum(final BoundingBox bb) {
        return this.cubeInFrustum(bb.minX(), bb.minY(), bb.minZ(), bb.maxX() + 1, bb.maxY() + 1, bb.maxZ() + 1);
    }

    /**
     * Computes the intersection result between the frustum and an axis‑aligned bounding box.
     *
     * The original implementation cast the coordinate differences to {@code float} before
     * delegating to {@link FrustumIntersection#intersectAab}. This works well for typical
     * Minecraft world coordinates but loses precision once the absolute coordinate magnitude
     * exceeds about 1 × 10⁹ (≈ 2³⁰). At such scales the {@code float} mantissa can only represent
     * steps of ~256 blocks, causing small bounding‑boxes (e.g. a single chunk or section) to be
     * rounded to a degenerate shape and mistakenly reported as outside the view frustum.
     *
     * To retain correct culling for the "NoiseFarlands" deep‑world modifications where player
     * positions can surpass {@code Integer.MAX_VALUE}, we first compute the deltas as {@code double}
     * values. If any delta exceeds a safe threshold (1 × 10⁹), the float conversion would be
     * lossy, so we bypass the {@code FrustumIntersection} test and conservatively treat the
     * bounding box as visible (return {@code -1}). This ensures terrain beyond the overflow
     * point continues to render while preserving the original fast‑path for normal ranges.
     */
    private int cubeInFrustum(final double minX, final double minY, final double minZ, final double maxX, final double maxY, final double maxZ) {
        // Compute differences using double precision to avoid overflow/precision loss.
        double dx1 = minX - this.camX;
        double dy1 = minY - this.camY;
        double dz1 = minZ - this.camZ;
        double dx2 = maxX - this.camX;
        double dy2 = maxY - this.camY;
        double dz2 = maxZ - this.camZ;

        // Determine the largest absolute delta. If it exceeds 1e9 (well beyond typical render
        // distances) we deem the float conversion unsafe and assume the box is visible.
        double maxAbs = Math.max(Math.abs(dx1), Math.abs(dx2));
        maxAbs = Math.max(maxAbs, Math.abs(dy1));
        maxAbs = Math.max(maxAbs, Math.abs(dy2));
        maxAbs = Math.max(maxAbs, Math.abs(dz1));
        maxAbs = Math.max(maxAbs, Math.abs(dz2));
        if (maxAbs > 1.0E9) {
            // Treat as intersecting to avoid false‑negative culling at extreme coordinates.
            return -1;
        }

        // Safe to down‑cast to float for the JOML intersection test.
        float x1 = (float) dx1;
        float y1 = (float) dy1;
        float z1 = (float) dz1;
        float x2 = (float) dx2;
        float y2 = (float) dy2;
        float z2 = (float) dz2;
        return this.intersection.intersectAab(x1, y1, z1, x2, y2, z2);
    }

    public boolean pointInFrustum(final double x, final double y, final double z) {
        return this.intersection.testPoint((float)(x - this.camX), (float)(y - this.camY), (float)(z - this.camZ));
    }

    public Vector4f[] getFrustumPoints() {
        Vector4f[] frustumPoints = new Vector4f[]{
            new Vector4f(-1.0F, -1.0F, -1.0F, 1.0F),
            new Vector4f(1.0F, -1.0F, -1.0F, 1.0F),
            new Vector4f(1.0F, 1.0F, -1.0F, 1.0F),
            new Vector4f(-1.0F, 1.0F, -1.0F, 1.0F),
            new Vector4f(-1.0F, -1.0F, 1.0F, 1.0F),
            new Vector4f(1.0F, -1.0F, 1.0F, 1.0F),
            new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
            new Vector4f(-1.0F, 1.0F, 1.0F, 1.0F)
        };
        Matrix4f clipToWorldMatrix = this.matrix.invert(new Matrix4f());

        for (int i = 0; i < 8; i++) {
            clipToWorldMatrix.transform(frustumPoints[i]);
            frustumPoints[i].div(frustumPoints[i].w());
        }

        return frustumPoints;
    }

    public double getCamX() {
        return this.camX;
    }

    public double getCamY() {
        return this.camY;
    }

    public double getCamZ() {
        return this.camZ;
    }
}