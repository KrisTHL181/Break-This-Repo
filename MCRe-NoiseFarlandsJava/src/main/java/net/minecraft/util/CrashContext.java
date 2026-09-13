package net.minecraft.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * 捕获方法入口时的参数快照，供 CrashReport 使用。
 * 使用方式：在方法入口处 CrashContext.pushFrame(className, methodName, arg1, arg2, ...);
 * 在方法出口处（finally 块） CrashContext.popFrame();
 */
public final class CrashContext {
    private static final ThreadLocal<Deque<FrameArgs>> CONTEXT = ThreadLocal.withInitial(ArrayDeque::new);

    public static final class FrameArgs {
        public final String className;
        public final String methodName;
        public final Object[] args; // 永不为 null，长度可能为 0

        public FrameArgs(String className, String methodName, Object[] args) {
            this.className = className;
            this.methodName = methodName;
            this.args = args == null ? new Object[0] : args.clone(); // 防御性复制
        }

        @Override
        public String toString() {
            return className + "." + methodName + "(" + java.util.Arrays.toString(args) + ")";
        }
    }

    /** 压入当前帧的参数快照 */
    public static void pushFrame(String className, String methodName, Object... args) {
        CONTEXT.get().addLast(new FrameArgs(className, methodName, args));
    }

    /** 弹出最近压入的帧（必须与 pushFrame 成对出现） */
    public static void popFrame() {
        Deque<FrameArgs> deque = CONTEXT.get();
        if (!deque.isEmpty()) {
            deque.removeLast();
        }
    }

    /** 获得当前上下文的深拷贝快照（用于 CrashReport） */
    public static Deque<FrameArgs> snapshot() {
        Deque<FrameArgs> original = CONTEXT.get();
        Deque<FrameArgs> copy = new ArrayDeque<>(original.size());
        for (FrameArgs fa : original) {
            copy.addLast(new FrameArgs(fa.className, fa.methodName, fa.args));
        }
        return copy;
    }

    /** 清理线程局部（防止内存泄漏，通常在线程复用时调用） */
    public static void clear() {
        CONTEXT.get().clear();
    }
}