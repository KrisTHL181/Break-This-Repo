# 🚀 Bro-Stat: Universal Router Monitoring Enhancement Suite

**English** | [简体中文](README.md)

![Preview](./华为预览图.png)
**Bro-Stat** is a browser-based extension designed to improve the native web management experience of consumer and prosumer routers across multiple brands. The current universal edition primarily supports and enhances routers from **TP-Link, Xiaomi (MiWiFi), ASUS/ROG，HUAWEI**, and other mainstream vendors.

Developed by **BroTech**, Bro-Stat provides a lightweight network telemetry and multi-end data forwarding solution for modern home and enthusiast networks. Output to CSV File Regularly is supported.

Integrated with smart home ecosystems and Home Assistant, it delivers advanced UI enhancements, telemetry synchronization, and router monitoring capabilities without requiring custom firmware. It serves as an ideal companion for hardware-accelerated routing platforms and supports the entire ZTE router ecosystem. Device lists become cleaner, dashboards become larger, and network insights become instantly accessible without endless page switching.

Native router dashboards usually provide only basic instantaneous bandwidth information. In many cases, all traffic statistics disappear after a reboot or device reconnection. Bro-Stat aims to provide a persistent, intuitive, and lightweight LAN traffic visualization layer while preserving the original router interface.

![logo](./logo.png)

**Bro-Stat Enhancement Suite**
Copyright © 2026 BroTech (哥哥科技) | All Rights Reserved

Bilibili：[哥哥科技：501430041](https://space.bilibili.com/501430041)

## 🔗 Specialized Editions for WhiteBox Routers Brand

**Mi-Stat_Max (Xiaomi Routers)**
https://github.com/ucxn/Mi-Stat_Max

**ZTE-Stat_Max (ZTE Routers)**
https://github.com/ucxn/ZTE-Stat_Max

**Home Assistant Integration (HACS)**
[Universal Edition](https://github.com/ucxn/ZTE-Stat_HA)

![signal](Wi-Fi图标映射设计稿.png)

## 💡 Core Features

### 1. Persistent Traffic Storage

Most router traffic counters have no memory.

Bro-Stat introduces a local snapshot persistence mechanism. Every time the dashboard is opened, the extension automatically loads the previous traffic snapshot and seamlessly continues tracking from the latest router statistics.

Even after router reboots or device reconnections, historical upload and download consumption remains visible, making it much easier to identify devices silently consuming bandwidth.

### 2. Miniature Time-Series Sparklines

Instead of relying on third-party chart libraries, Bro-Stat renders dynamic traffic waveforms directly using lightweight character-based sparklines.

**Peak Retention**

Inspired by Windows Task Manager, the Y-axis uses a sticky scaling algorithm. After a large traffic spike, the scale gradually falls back rather than instantly collapsing, significantly reducing visual jitter.

**Noise Suppression**

Background traffic is automatically filtered out. Tiny fluctuations disappear into silence, while meaningful throughput creates visible waveforms, allowing network activity patterns to be recognized at a glance.

### 3. Heartbeat Detection

When traffic drops to extremely low levels, such as MQTT heartbeat packets from smart home devices, native router interfaces typically display crude values like `0 KB/s` or `1 KB/s`.

Bro-Stat introduces a fractional display mechanism based on sixteenth increments, such as `[3/16] KiB/s`.

Even when a device is effectively idle, these subtle "digital heartbeats" allow users to determine whether the device remains online and active.

### 4. High-Precision Upload Tracking (PCDN Spotlight)

Because upstream bandwidth is often the most valuable resource on residential broadband connections, Bro-Stat treats upload and download traffic differently.

**Download Direction**

Focused on real-time competition. Instantly shows which devices are consuming downstream bandwidth.

**Upload Direction**

Uses an accounting-oriented visualization model. Independent orange progress bars and proportional radar indicators clearly display cumulative upload contributions for every device.

Combined with waveform patterns (continuous transmission versus burst traffic), suspicious PCDN activity within the LAN can be identified quickly and intuitively.

### 5. Performance Optimization

A monitoring dashboard should remain smooth even after running continuously for weeks.

Bro-Stat uses a pure mathematical trapezoidal integration engine to eliminate unnecessary polling overhead, while the rendering layer leverages native DOM optimizations and Flexbox-based layout stabilization to minimize browser reflow.

Even when hundreds of smart devices are refreshing simultaneously, memory usage and CPU consumption remain remarkably stable.

### 6. 🏠 Home Assistant Integration

When paired with the dedicated **BroTech Hub Integration**, device and traffic states can be pushed to Home Assistant through Webhooks and HACS-compatible integrations.

This eliminates the single-client limitation of traditional web dashboards and enables simultaneous monitoring from multiple devices and platforms.

Companion project:

**ZTE-Stat_HA**
https://github.com/ucxn/ZTE-Stat_HA

### 7. Customized, Precise Wi-Fi Signal SVG Icons


## ⚙️ Supported Layouts & Modes

Bro-Stat provides flexible configuration options for different screen sizes and visual preferences. Settings can be adjusted directly through the `CONFIG` section.

### Full-Width Floating Console

Removes the fixed-width limitations of many native router dashboards and expands monitoring panels across the entire screen.

Recommended for TP-Link and similar router platforms.

### Cockpit Layout (UI Layout 1)

Compact, information-dense, and optimized for monitoring a large number of devices simultaneously.

### Dashboard Report Layout (UI Layout 2)

A cleaner and more spacious presentation style, ideal for always-on secondary displays and dedicated monitoring screens.

## 📦 Installation

1. Install **Tampermonkey** or **ScriptCat** in your browser.
2. Click **Install Script** on this page.
3. Log in to your router's web management interface (for example, `tplogin.cn` or `192.168.31.1`).
4. A 🛸 floating button will automatically appear on the right side of the page. Click it to open the monitoring dashboard.

You may also pin the panel to the top of the page using the 📌 icon for permanent visibility.


## 📜 Legal Notice & Open Source Statement

> *"In a civilized society, a clean network free from surveillance and exploitation is a fundamental right for everyone."*

This software is released under the **GNU Affero General Public License v3.0 (AGPL-3.0)** and is provided **"AS IS"**, without any express or implied warranties regarding suitability, stability, accuracy, fitness for a particular purpose, or compliance with any commercial use case.

Out of respect for open-source contributors, any modification, redistribution, or derivative work based on this project must preserve the attribution and legal notice section displayed at the bottom of the interface.

Maintaining the visibility of these notices is a prerequisite for lawful use of the source code provided by this project.