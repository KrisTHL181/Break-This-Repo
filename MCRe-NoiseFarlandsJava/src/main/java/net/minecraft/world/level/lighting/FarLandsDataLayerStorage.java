package net.minecraft.world.level.lighting;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.DataLayer;

/**
 * 🔧 MCRe：单层光照存储（替代 vanilla 的 queued/updating/visible 三缓冲 + swap + COW）。
 *
 * <p>每个引擎一个 {@code ConcurrentHashMap<SectionPos, DataLayer>}：
 * <ul>
 *   <li>键 = {@link SectionPos} 对象（我们项目已对象化，对象即键、零碰撞——替代
 *       inf_farlands 的 HashUtil 哈希侧信道）；</li>
 *   <li>支持任意 Y section，不受 vanilla 高度数组钳制；</li>
 *   <li>ConcurrentHashMap 无锁读热路径（渲染线程每帧 getLightValue），
 *       传播写走服务端线程、initializeLight 走后台线程；</li>
 *   <li>弱一致性：与 remove 并发时 get 可能返回刚移除的引用（瞬态快照，下一帧修正，无害）。</li>
 * </ul>
 *
 * @author MCRe Ultimate Scaler
 */
public class FarLandsDataLayerStorage {

    private final ConcurrentHashMap<SectionPos, DataLayer> map = new ConcurrentHashMap<>();

    public DataLayer get(final SectionPos key) {
        return this.map.get(key);
    }

    public DataLayer getOrCreate(final SectionPos key) {
        return this.map.computeIfAbsent(key, k -> new DataLayer());
    }

    public void put(final SectionPos key, final DataLayer layer) {
        this.map.put(key, layer);
    }

    public DataLayer remove(final SectionPos key) {
        return this.map.remove(key);
    }

    public boolean containsKey(final SectionPos key) {
        return this.map.containsKey(key);
    }

    /**
     * 按谓词移除（服务端 chunk 卸载清理用）。ConcurrentHashMap keySet.removeIf
     * 线程安全；弱一致遍历可能漏掉并发加入的条目——卸载场景无新层加入该 chunk，
     * 残留瞬态由重载 propagateLightSources 覆盖。
     */
    public void removeIf(final Predicate<SectionPos> predicate) {
        this.map.keySet().removeIf(predicate);
    }
}
