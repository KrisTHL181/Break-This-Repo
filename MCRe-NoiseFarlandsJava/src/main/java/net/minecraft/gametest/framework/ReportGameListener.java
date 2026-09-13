package net.minecraft.gametest.framework;

import com.google.common.base.MoreObjects;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class ReportGameListener implements GameTestListener {
    private int attempts = 0;
    private int successes = 0;

    @Override
    public void testStructureLoaded(final GameTestInfo testInfo) {
        this.attempts++;
    }

    private void handleRetry(final GameTestInfo testInfo, final GameTestRunner runner, final boolean passed) {
        RetryOptions retryOptions = testInfo.retryOptions();
        String reportAs = String.format(Locale.ROOT, "[Run: %4d, Ok: %4d, Fail: %4d", this.attempts, this.successes, this.attempts - this.successes);
        if (!retryOptions.unlimitedTries()) {
            reportAs = reportAs + String.format(Locale.ROOT, ", Left: %4d", retryOptions.numberOfTries() - this.attempts);
        }

        reportAs = reportAs + "]";
        String namePart = testInfo.id() + " " + (passed ? "passed" : "failed") + "! " + testInfo.getRunTime() + "ms";
        String text = String.format(Locale.ROOT, "%-53s%s", reportAs, namePart);
        if (passed) {
            reportPassed(testInfo, text);
        } else {
            say(testInfo.getLevel(), ChatFormatting.RED, text);
        }

        if (retryOptions.hasTriesLeft(this.attempts, this.successes)) {
            runner.rerunTest(testInfo);
        }
    }

    @Override
    public void testPassed(final GameTestInfo testInfo, final GameTestRunner runner) {
        this.successes++;
        if (testInfo.retryOptions().hasRetries()) {
            this.handleRetry(testInfo, runner, true);
        } else if (!testInfo.isFlaky()) {
            reportPassed(testInfo, testInfo.id() + " passed! (" + testInfo.getRunTime() + "ms / " + testInfo.getTick() + "gameticks)");
        } else {
            if (this.successes >= testInfo.requiredSuccesses()) {
                reportPassed(testInfo, testInfo + " passed " + this.successes + " times of " + this.attempts + " attempts.");
            } else {
                say(
                    testInfo.getLevel(),
                    ChatFormatting.GREEN,
                    "Flaky test " + testInfo + " succeeded, attempt: " + this.attempts + " successes: " + this.successes
                );
                runner.rerunTest(testInfo);
            }
        }
    }

    @Override
    public void testFailed(final GameTestInfo testInfo, final GameTestRunner runner) {
        if (!testInfo.isFlaky()) {
            reportFailure(testInfo, testInfo.getError());
            if (testInfo.retryOptions().hasRetries()) {
                this.handleRetry(testInfo, runner, false);
            }
        } else {
            GameTestInstance testFunction = testInfo.getTest();
            String text = "Flaky test " + testInfo + " failed, attempt: " + this.attempts + "/" + testFunction.maxAttempts();
            if (testFunction.requiredSuccesses() > 1) {
                text = text + ", successes: " + this.successes + " (" + testFunction.requiredSuccesses() + " required)";
            }

            say(testInfo.getLevel(), ChatFormatting.YELLOW, text);
            if (testInfo.maxAttempts() - this.attempts + this.successes >= testInfo.requiredSuccesses()) {
                runner.rerunTest(testInfo);
            } else {
                reportFailure(testInfo, new ExhaustedAttemptsException(this.attempts, this.successes, testInfo));
            }
        }
    }

    @Override
    public void testAddedForRerun(final GameTestInfo original, final GameTestInfo copy, final GameTestRunner runner) {
        copy.addListener(this);
    }

    public static void reportPassed(final GameTestInfo testInfo, final String text) {
        getTestInstanceBlockEntity(testInfo).ifPresent(blockEntity -> blockEntity.setSuccess());
        visualizePassedTest(testInfo, text);
    }

    private static void visualizePassedTest(final GameTestInfo testInfo, final String text) {
        say(testInfo.getLevel(), ChatFormatting.GREEN, text);
        GlobalTestReporter.onTestSuccess(testInfo);
    }

    protected static void reportFailure(final GameTestInfo testInfo, final Throwable error) {
        Component description;
        if (error instanceof GameTestAssertException testException) {
            description = testException.getDescription();
        } else {
            description = Component.literal(Util.describeError(error));
        }

        getTestInstanceBlockEntity(testInfo).ifPresent(blockEntity -> blockEntity.setErrorMessage(description));
        visualizeFailedTest(testInfo, error);
    }

    protected static void visualizeFailedTest(final GameTestInfo testInfo, final Throwable error) {
        String errorMessage = error.getMessage() + (error.getCause() == null ? "" : " cause: " + Util.describeError(error.getCause()));
        String failureMessage = (testInfo.isRequired() ? "" : "(optional) ") + testInfo.id() + " failed! " + errorMessage;
        say(testInfo.getLevel(), testInfo.isRequired() ? ChatFormatting.RED : ChatFormatting.YELLOW, failureMessage);
        Throwable rootCause = MoreObjects.firstNonNull(ExceptionUtils.getRootCause(error), error);
        if (rootCause instanceof GameTestAssertPosException assertError) {
            testInfo.getTestInstanceBlockEntity().markError(assertError.getAbsolutePos(), assertError.getMessageToShowAtBlock());
        }

        GlobalTestReporter.onTestFailed(testInfo);
    }

    private static Optional<TestInstanceBlockEntity> getTestInstanceBlockEntity(final GameTestInfo testInfo) {
        ServerLevel level = testInfo.getLevel();
        Optional<BlockPos> testPos = Optional.ofNullable(testInfo.getTestBlockPos());
        return testPos.flatMap(pos -> level.getBlockEntity(pos, BlockEntityTypes.TEST_INSTANCE_BLOCK));
    }

    protected static void say(final ServerLevel level, final ChatFormatting format, final String text) {
        level.getPlayers(player -> true).forEach(player -> player.sendSystemMessage(Component.literal(text).withStyle(format)));
    }
}