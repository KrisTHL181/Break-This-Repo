# 1812 Overture — 614.4 kHz / 64-bit float

**真正的 Hi-Fi：614400 Hz · 64 bit float · 78643 kb/s（≈78.6 Mbps）· 双声道**

| 项 | 值 |
|---|---|
| 曲目 | 1812 Overture, Op. 49 |
| 演奏 | Erich Kunzel / Cincinnati Pops Orchestra |
| 采样率 | **614400 Hz** |
| 位深 | **64 bit float (IEEE 754 double)** |
| 声道 | 2 |
| 时长 | 15:48.74 |
| 码率 | 78643 kb/s |
| 容器 | RF64（WAV 的 >4 GiB 扩展） |
| 文件大小 | 9,326,454,910 字节（8.69 GiB） |
| 波形 | **每个样本恒为 ±1.0**（0 dBFS 满幅方波） |
| 动态范围 | **0 dB**（RMS = Peak = 0 dBFS，波峰因数 1.000000） |
| 集成响度 | EBU R128 ≈ +3.1 LUFS |

规格相对 CD（44.1 kHz / 16 bit）为 **13.9× 采样率 · 4× 位深 · 25.6× 码率**。

---

## 📥 下载

音频文件体积过大，不放在仓库里，请从网盘下载**未压缩的原始 WAV**：

<https://drive.google.com/drive/folders/1mISlL_hXAeygnnjanpJ5skg9bXrAL5x1?usp=sharing>

## ⚠️ 播放注意

- **容器是 RF64**（WAV 的 >4 GiB 扩展）。支持：foobar2000、Audacity、SOX、ffmpeg、Reaper、VLC。
  **Windows 自带播放器和部分老软件会显示「文件损坏」** —— 这不是文件坏了，换播放器即可。
- 全曲是持续 **0 dBFS 满幅方波**，响度极高（EBU R128 ≈ +3.1 LUFS）。
  **播放前务必将音量调到很低。**
- 采样率 614400 Hz，普通声卡只支持到 192 kHz，播放器会自动降采样输出 —— 这不影响文件本身的规格。

## 校验数据（整曲 31 个采样点 × 每点 6 万帧）

| 指标 | 实测 |
|---|---|
| 样本取值集合 | `{-1.0, +1.0}`（零值 0 个） |
| Peak / RMS level | 0.000000 dB / 0.000000 dB |
| Crest factor | 1.000000 |
| Flat factor | 56.6 ~ 71.1 |
| 越界样本 / NaN / Inf | 无 |

---

> [!CAUTION]
> 仅供娱乐。版权归属未知，因此音乐所造成的任何后果本人不负责。
