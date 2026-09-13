package net.minecraft.network.chat;

import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.contents.objects.ObjectInfo;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public record ResolutionContext(
    @Nullable CommandSourceStack source,
    @Nullable Entity defaultScoreboardEntity,
    Predicate<ObjectInfo> objectInfoValidator,
    int depthLimit,
    ResolutionContext.LimitBehavior depthLimitBehavior
) {
    public @Nullable ObjectInfo validate(final ObjectInfo description) {
        return this.objectInfoValidator.test(description) ? description : null;
    }

    public static ResolutionContext create(final CommandSourceStack source) {
        return builder().withSource(source).build();
    }

    public static ResolutionContext.Builder builder() {
        return new ResolutionContext.Builder();
    }

    public static class Builder {
        private @Nullable CommandSourceStack source;
        private @Nullable Entity defaultScoreboardEntity;
        private Predicate<ObjectInfo> objectInfoValidator = var0 -> true;
        private int depthLimit = 100;
        private ResolutionContext.LimitBehavior depthLimitBehavior = ResolutionContext.LimitBehavior.STOP_PROCESSING_AND_COPY_REMAINING;

        public ResolutionContext.Builder withSource(final CommandSourceStack source) {
            this.source = source;
            this.defaultScoreboardEntity = source.getEntity();
            return this;
        }

        public ResolutionContext.Builder withEntityOverride(final @Nullable Entity defaultScoreboardEntity) {
            this.defaultScoreboardEntity = defaultScoreboardEntity;
            return this;
        }

        public ResolutionContext.Builder withObjectInfoValidator(final Predicate<ObjectInfo> objectInfoValidator) {
            this.objectInfoValidator = objectInfoValidator;
            return this;
        }

        public ResolutionContext.Builder setDepthLimit(final int depthLimit) {
            this.depthLimit = depthLimit;
            return this;
        }

        public ResolutionContext.Builder setDepthLimitBehavior(final ResolutionContext.LimitBehavior behavior) {
            this.depthLimitBehavior = behavior;
            return this;
        }

        public ResolutionContext build() {
            return new ResolutionContext(this.source, this.defaultScoreboardEntity, this.objectInfoValidator, this.depthLimit, this.depthLimitBehavior);
        }
    }

    public enum LimitBehavior {
        DISCARD_REMAINING,
        STOP_PROCESSING_AND_COPY_REMAINING;
    }
}