package net.minecraft.client.particle;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleLimit;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class Particle {
    private static final AABB INITIAL_AABB = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    private static final double MAXIMUM_COLLISION_VELOCITY_SQUARED = Mth.square(100.0);
    protected final ClientLevel level;
    protected double xo;
    protected double yo;
    protected double zo;
    protected double x;
    protected double y;
    protected double z;
    protected double xd;
    protected double yd;
    protected double zd;
    private AABB bb = INITIAL_AABB;
    protected boolean onGround;
    protected boolean hasPhysics = true;
    private boolean stoppedByCollision;
    protected boolean removed;
    protected float bbWidth = 0.6F;
    protected float bbHeight = 1.8F;
    protected final RandomSource random = RandomSource.create();
    protected int age;
    protected int lifetime;
    protected float gravity;
    protected float friction = 0.98F;
    protected boolean speedUpWhenYMotionIsBlocked = false;

    protected Particle(final ClientLevel level, final double x, final double y, final double z) {
        this.level = level;
        this.setSize(0.2F, 0.2F);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.lifetime = (int)(4.0F / (this.random.nextFloat() * 0.9F + 0.1F));
    }

    public Particle(final ClientLevel level, final double x, final double y, final double z, final double xa, final double ya, final double za) {
        this(level, x, y, z);
        this.xd = xa + (this.random.nextFloat() * 2.0F - 1.0F) * 0.4F;
        this.yd = ya + (this.random.nextFloat() * 2.0F - 1.0F) * 0.4F;
        this.zd = za + (this.random.nextFloat() * 2.0F - 1.0F) * 0.4F;
        double speed = (this.random.nextFloat() + this.random.nextFloat() + 1.0F) * 0.15F;
        double dd = Math.sqrt(this.xd * this.xd + this.yd * this.yd + this.zd * this.zd);
        this.xd = this.xd / dd * speed * 0.4F;
        this.yd = this.yd / dd * speed * 0.4F + 0.1F;
        this.zd = this.zd / dd * speed * 0.4F;
    }

    public Particle setPower(final float power) {
        this.xd *= power;
        this.yd = (this.yd - 0.1F) * power + 0.1F;
        this.zd *= power;
        return this;
    }

    public void setParticleSpeed(final double xd, final double yd, final double zd) {
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
    }

    public Particle scale(final float scale) {
        this.setSize(0.2F * scale, 0.2F * scale);
        return this;
    }

    public void setLifetime(final int lifetime) {
        this.lifetime = lifetime;
    }

    public int getLifetime() {
        return this.lifetime;
    }

    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.yd = this.yd - 0.04 * this.gravity;
            this.move(this.xd, this.yd, this.zd);
            if (this.speedUpWhenYMotionIsBlocked && this.y == this.yo) {
                this.xd *= 1.1;
                this.zd *= 1.1;
            }

            this.xd = this.xd * this.friction;
            this.yd = this.yd * this.friction;
            this.zd = this.zd * this.friction;
            if (this.onGround) {
                this.xd *= 0.7F;
                this.zd *= 0.7F;
            }
        }
    }

    public abstract ParticleRenderType getGroup();

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ", Pos (" + this.x + "," + this.y + "," + this.z + "), Age " + this.age;
    }

    public void remove() {
        this.removed = true;
    }

    protected void setSize(final float w, final float h) {
        if (w != this.bbWidth || h != this.bbHeight) {
            this.bbWidth = w;
            this.bbHeight = h;
            AABB aabb = this.getBoundingBox();
            double newMinX = (aabb.minX + aabb.maxX - w) / 2.0;
            double newMinZ = (aabb.minZ + aabb.maxZ - w) / 2.0;
            this.setBoundingBox(new AABB(newMinX, aabb.minY, newMinZ, newMinX + this.bbWidth, aabb.minY + this.bbHeight, newMinZ + this.bbWidth));
        }
    }

    public void setPos(final double x, final double y, final double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        float w = this.bbWidth / 2.0F;
        float h = this.bbHeight;
        this.setBoundingBox(new AABB(x - w, y, z - w, x + w, y + h, z + w));
    }

    public void move(double xa, double ya, double za) {
        if (!this.stoppedByCollision) {
            double originalXa = xa;
            double originalYa = ya;
            double originalZa = za;
            if (this.hasPhysics && (xa != 0.0 || ya != 0.0 || za != 0.0) && xa * xa + ya * ya + za * za < MAXIMUM_COLLISION_VELOCITY_SQUARED) {
                Vec3 movement = Entity.collideBoundingBox(
                    CollisionContext.positionContext(this.y), new Vec3(xa, ya, za), this.getBoundingBox(), this.level, List.of()
                );
                xa = movement.x;
                ya = movement.y;
                za = movement.z;
            }

            if (xa != 0.0 || ya != 0.0 || za != 0.0) {
                this.setBoundingBox(this.getBoundingBox().move(xa, ya, za));
                this.setLocationFromBoundingbox();
            }

            if (Math.abs(originalYa) >= 1.0E-5F && Math.abs(ya) < 1.0E-5F) {
                this.stoppedByCollision = true;
            }

            this.onGround = originalYa != ya && originalYa < 0.0;
            if (originalXa != xa) {
                this.xd = 0.0;
            }

            if (originalZa != za) {
                this.zd = 0.0;
            }
        }
    }

    protected void setLocationFromBoundingbox() {
        AABB aabb = this.getBoundingBox();
        this.x = (aabb.minX + aabb.maxX) / 2.0;
        this.y = aabb.minY;
        this.z = (aabb.minZ + aabb.maxZ) / 2.0;
    }

    protected int getLightCoords(final float a) {
        BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
        return this.level.hasChunkAt(pos) ? LightCoordsUtil.getLightCoords(this.level, pos) : 15728640;
    }

    public boolean isAlive() {
        return !this.removed;
    }

    public AABB getBoundingBox() {
        return this.bb;
    }

    public void setBoundingBox(final AABB bb) {
        this.bb = bb;
    }

    public Optional<ParticleLimit> getParticleLimit() {
        return Optional.empty();
    }

    @OnlyIn(Dist.CLIENT)
    public record LifetimeAlpha(float startAlpha, float endAlpha, float startAtNormalizedAge, float endAtNormalizedAge) {
        public static final Particle.LifetimeAlpha ALWAYS_OPAQUE = new Particle.LifetimeAlpha(1.0F, 1.0F, 0.0F, 1.0F);

        public boolean isOpaque() {
            return this.startAlpha >= 1.0F && this.endAlpha >= 1.0F;
        }

        public float currentAlphaForAge(final int age, final int lifetime, final float partialTickTime) {
            if (Mth.equal(this.startAlpha, this.endAlpha)) {
                return this.startAlpha;
            }

            float timeNormalized = Mth.inverseLerp((age + partialTickTime) / lifetime, this.startAtNormalizedAge, this.endAtNormalizedAge);
            return Mth.clampedLerp(timeNormalized, this.startAlpha, this.endAlpha);
        }
    }
}