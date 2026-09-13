package net.minecraft.client.renderer.item.properties.numeric;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class NeedleDirectionHelper {
    private final boolean wobble;

    protected NeedleDirectionHelper(final boolean wobble) {
        this.wobble = wobble;
    }

    public float get(final ItemStack itemStack, @Nullable ClientLevel clientLevel, final @Nullable ItemOwner owner, final int seed) {
        if (owner == null) {
            return 0.0F;
        }

        if (clientLevel == null && owner.level() instanceof ClientLevel level) {
            clientLevel = level;
        }

        return clientLevel == null ? 0.0F : this.calculate(itemStack, clientLevel, seed, owner);
    }

    protected abstract float calculate(final ItemStack itemStack, final ClientLevel level, final int seed, final ItemOwner owner);

    protected boolean wobble() {
        return this.wobble;
    }

    protected NeedleDirectionHelper.Wobbler newWobbler(final float factor) {
        return this.wobble ? standardWobbler(factor) : nonWobbler();
    }

    public static NeedleDirectionHelper.Wobbler standardWobbler(final float factor) {
        return new NeedleDirectionHelper.Wobbler() {
            private float rotation;
            private float deltaRotation;
            private long lastUpdateTick;

            @Override
            public float rotation() {
                return this.rotation;
            }

            @Override
            public boolean shouldUpdate(final long tick) {
                return this.lastUpdateTick != tick;
            }

            @Override
            public void update(final long tick, final float targetRotation) {
                this.lastUpdateTick = tick;
                float tempDeltaRotation = Mth.positiveModulo(targetRotation - this.rotation + 0.5F, 1.0F) - 0.5F;
                this.deltaRotation += tempDeltaRotation * 0.1F;
                this.deltaRotation = this.deltaRotation * factor;
                this.rotation = Mth.positiveModulo(this.rotation + this.deltaRotation, 1.0F);
            }
        };
    }

    public static NeedleDirectionHelper.Wobbler nonWobbler() {
        return new NeedleDirectionHelper.Wobbler() {
            private float targetValue;

            @Override
            public float rotation() {
                return this.targetValue;
            }

            @Override
            public boolean shouldUpdate(final long tick) {
                return true;
            }

            @Override
            public void update(final long tick, final float targetRotation) {
                this.targetValue = targetRotation;
            }
        };
    }

    @OnlyIn(Dist.CLIENT)
    public interface Wobbler {
        float rotation();

        boolean shouldUpdate(long tick);

        void update(long tick, float targetRotation);
    }
}