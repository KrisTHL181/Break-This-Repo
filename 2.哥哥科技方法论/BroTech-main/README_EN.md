# BroTech 哥哥科技 Why Brother Tech? The Origin of name.
[简体中文](README.md) | **English**

Why is it called “哥哥科技”? Because I love my older brother and my younger brother. It’s that simple.

BroTech（哥哥科技） is my personal technology brand.

I love building things with technology, and I’m also willing to leave some of the things I genuinely love—the things I’ve genuinely spent time polishing—on the Internet. So in the end, I chose the name 哥哥科技 / BroTech.

## What Does Brother Tech Do?

BroTech currently focuses on consumer-grade hardware routers, home networking, and OEM web UIs. This involves per-device real-time bandwidth monitoring, traffic accounting, network telemetry, browser userscripts, OEM APIs, and XHR/Fetch data interfaces. It also touches upon NPUs, hardware offloading, Mesh topologies, device state polling, data sampling, and visualization. I absolutely despise flashing consumer hardware routers with third-party firmware. Instead, I prefer to explore whether the OEM's stock system can be pushed to its limits and fully utilized first.

In my dictionary, the concept of "flashing" simply doesn't exist. Instead of flashing that pitiful ROM, you'd be better off taking a literal brush to dust off the motherboard. To me, there is only "OS installation" and "system deployment." "Firmware" strictly refers to hardcoded code like BIOS/UEFI.
[The Ideal Gateway](https://zhuanlan.zhihu.com/p/2022428415328797037)

I’m well aware this isn't an academically strict definition. Strictly speaking, system installation and deployment are similar—essentially the reverse process of system imaging/encapsulation. An image is merely a container; formats like WIM or ESD have absolutely zero relation to the actual deployment architecture used inside. And obviously, "firmware" encompasses more than just BIOS. What I'm really getting at is the distinction in how a device or computer operates as an integrated hardware-software appliance. Is the written image a monolithic, hardcoded blob that boots directly (not necessarily burned into a physical ROM)? Or does it require a complete unfolding of the Device Tree (DTB) and optimization for the specific runtime environment?

Put simply: unless you are explicitly hacking firmware, the underlying OS—or the heavily customized UI running on top of it—should receive core feature upgrades via standard OTA/system updates, during which you generally cannot remove the installation media. Or to put it even more bluntly: when running a system update—ignoring physical side effects like CPU frequency spikes and thermal output—is there a direct logical causality or statistical probability that the update process will brick the device? (This is notoriously common on ARM platforms.)

Bottom line: in everyday practice, anything that can be installed, deployed, and maintained like a standard operating system, I treat as an OS. I reserve the word "firmware" strictly for platform-layer code—like boot firmware—that lives entirely beneath the OS layer.

Let the stock firmware keep handling the data plane it is good at. I tune the data at the Web management layer, preserve the stock NPU and hardware acceleration in full, and rebuild the admin interface into something better suited for people who genuinely want to see the data.

## Stock Hardware Routers Can Go Deep Too

In a lot of enthusiast circles, the moment someone says “router plugin,” the conversation quickly slides toward OpenWrt, the soft-router（The x86 router cult cult, pfSense / OpenWrt boxes）DPI, multi-WAN, policy routing, and things like that.

The problem is, what I have in my hands is a branded hardware router from ZTE, HUAWEI, XiaoMi, ASUS, TP-Link, or H3C. I have absolutely no intention of turning it into a general-purpose computer!

Can I reorganize all this data without breaking the stock system at all?
Can I see the real-time upload and download speeds of every device directly, instead of clicking into them one by one?
Can I put instantaneous speed, cumulative traffic, online status, Wi-Fi signal, and WAN status where people actually need to see them?
Can a stock hardware router keep using its own NPU and hardware offloading while also having a half-decent monitoring interface?

A lot of the code in 哥哥科技 slowly grew out of questions like these.

## Bro-Stat

[Bro-Stat](https://github.com/ucxn/Bro-Stat) is a Web UI enhancement and network monitoring project for branded hardware routers from multiple vendors.

It now has to deal with completely different backends from different manufacturers. Some devices provide instantaneous rates, while others expose only cumulative counters; some backends use XML, some use JSON, and some still have Lua pages hanging around; some firmware can be accessed directly with Fetch, while others require intercepting data from XHR; devices from vendors such as H3C can even involve GBK pages and relatively heavy device-information parsing.

So Bro-Stat is not a matter of writing one universal template first and then swapping in different vendors’ API addresses.

If ASUS gives me only cumulative bytes, I calculate the delta using the actual elapsed time; if ZTE can provide per-device instantaneous rates directly, I use its own data; if a particular generation of ZTE firmware exposes both a new Vue API and old Lua interfaces, I probe the real environment and choose accordingly; if full device parsing on H3C is relatively expensive, I split frequently changing data from slowly changing data and only rerun the heavy path when the device set itself changes.
Whether the vendor backend underneath is XML, JSON, or something else entirely, what users actually want to know has always been simple: who is online, who is uploading, who is downloading, how fast each device is going right now, how much traffic it has used, what its Wi-Fi signal looks like, what the total WAN speed is, who just connected, and who just dropped offline.
All the messy differences underneath are exactly what the program is supposed to deal with.

## ZTE-Stat_Max

[ZTE-Stat_Max](https://github.com/ucxn/ZTE-Stat_Max) was one of the major starting points for BroTech.
SourceForge also retains a release entry for it: [ZTE Router Web Plugin](https://sourceforge.net/projects/ZTE).

This project originally grew around the stock Web admin interface on ZTE routers. ZTE’s backend can already provide quite a lot of useful data; the problem is that much of it is buried fairly deep. Real-time per-device network speed may require opening a detail page, while cumulative device traffic, WAN data, and client data are scattered across different places.
So the question at the very beginning was actually quite simple: the data is already there, so why not just pull it out and put it in the device list? Then things gradually became more and more complicated.

Once you genuinely start counting traffic, you run into imperfect sampling intervals, devices suddenly going from zero throughput to active transmission, counters resetting to zero, browser background throttling, devices going offline, reconnecting, Mesh roaming, and different data sources disagreeing with one another.

<details>
<summary>View Details</summary>
<br>
ZTE’s stock implementation has a bug: when roaming happens, it counts the traffic twice.
For example, if you had 20 GB of traffic before roaming and then roam to another AP, it gets counted twice and becomes 40 GB.

The stock implementation doesn’t just reset counters. It also roams, inherits, and rises from the dead. There are several major problems.

A “reset” means that after a device goes offline and reconnects, its traffic statistics return to zero.

“Rising from the dead” means that when the ZTE Nebula Max is the main router, some routers do not actually know the wireless state of a device, so the device has to remain offline for 10 minutes before the router reports it as offline;
the wireless AP itself will report the device offline fairly quickly, usually within about 10 seconds, and then maintain the ARP table for 5 minutes;
if there is the slightest sign of life during those 5 minutes, it retries; after those 5 minutes, it maintains the table for another 5-minute round, and only after 10 minutes does it stop maintaining the ARP table, allowing the wired side of the main router to finally learn that the device is offline.

So if the device reconnects within 5 minutes, the ZTE 2.5GbE-Wired Max does not hit the bug.
Between 5 and 10 minutes, there is a chance it immediately inherits the old value and nothing goes wrong, and there is also a chance it resets to zero. My plugin can defend against the reset.

The most disgusting case is when it resets to zero and then immediately inherits the old value again—that is the “rising from the dead” case.
A reset is a decreasing process and therefore easy to recognize. But rising from the dead is a perfectly valid monotonically increasing process, which makes it extremely disgusting to deal with.

</details>

It gradually evolved from a script that merely “made the backend look a bit better” into a data-processing approach focused more on network telemetry for stock hardware routers. During development, I also had technical discussions with people related to ZTE about issues such as backend polling intervals, and received some help from them.

## What I Actually Care About Is the Data

The UI can be changed however people want later. If the colors look bad, change them. If the layout becomes outdated, change it. If someone wants to rewrite the entire frontend in another framework someday, that is perfectly fine too.

The genuinely difficult parts are elsewhere:

If a device was still at 0 Mbps one second ago and suddenly becomes 100 Mbps at the next sample, how exactly should the traffic between those two sample points be calculated? If the timer is set to 1000 ms but the browser actually waits 1087 ms before executing the next cycle, should the calculation use 1000 ms or 1087 ms?
When the stock cumulative count, device-side integrated traffic, and total WAN traffic disagree, which one deserves more trust? If the interface changes after Mesh roaming, does that count as a disconnect and reconnect, or as the same device remaining online?
If a counter suddenly resets to zero, did the device genuinely stop generating traffic, or was the statistical baseline reset? When the actual sampling interval does not strictly equal the timer setting, should the program still assume a fixed interval?
Why should a tiny but persistent change be swallowed forever by a hysteresis threshold, while a massive change still has to stupidly sit there waiting for a fixed debounce timer?

You cannot see these problems in the UI, but whether all the numbers are ultimately accurate depends on details like these.

If I don’t build this, there may never be anyone else who carries this kind of obsession all the way into working code.

### Real-Time Network Speed and Traffic Integration

Under ordinary circumstances, the trapezoidal area between two adjacent samples can be used:

`Traffic ≈ (Speed_old + Speed_new) × Δt / 2`

What really matters is Δt: just because the program polls once per second does not mean every real interval is exactly 1000 ms. That is how error accumulates, little by little.

#### The Missing Piece When Going from 0 → Non-Zero:

There is another case that is easy to overlook: the previous sample is 0, and then obvious traffic suddenly appears at the next sample.

A real network obviously does not magically begin transmitting at the exact instant the second sample arrives. It is more likely to have started at some point between the two samples ➡️ If the entire interval is treated as 0, traffic is missed; if the entire interval is treated as running at the new rate, traffic is overestimated.

So some implementations make an additional estimate for these 0 → non-zero intervals—for example, compensating for the missing middle portion using the area of a triangle.
If a piece of data can only be estimated, then treat it as an estimate. Record the estimated amount and the number of times the compensation was triggered separately, so users know how much of the final cumulative value came from deterministic integration + compensation for sampling gaps.

#### Why Keep Multiple Data Sources at the Same Time?

On the same router, it is sometimes possible to obtain WAN-side statistics, client-side statistics, stock cumulative counters, and data integrated from real-time rates all at once.

If several sources remain very close to one another, they can mutually confirm that the statistical paths are broadly functioning normally; if one source suddenly drops to 0 while the other two continue increasing, that is a good reason to suspect a counter reset or an interface failure; if device-level cumulative traffic and total WAN traffic maintain a stable long-term difference, that may in turn help identify traffic that the current device statistics do not cover.

So multiple data sources are not simply several duplicate numbers on a screen. They can cross-check one another and can even participate in the final decision.
Just because the manufacturer gives you a number does not mean the program has to unconditionally treat it as the one and only truth.

### Why Can’t Debouncing Look Only at Time or a Fixed Threshold?

Data such as RSSI can easily bounce back and forth around some *threshold*.

The simplest solution is to say, “only update if the change persists for several seconds.” But that creates an obvious problem: going from 50 to 51 has to wait, yet somehow suddenly jumping from 0 to 100 also has to wait.
Another traditional approach is to define a dead zone around 50, say from 45 to 55. That does stop the bouncing, but if the real value changes to 51 and then stays there forever, the displayed value can theoretically remain stuck at 50 forever too.

So I prefer putting “how large the change is” and “how long the change has persisted” into the same decision.

When the change is very large, magnitude itself can quickly provide enough evidence; when the change is tiny, that is fine too—as long as it persists, it can accumulate little by little until there is enough evidence to update; if the value merely oscillates 51, 49, 51, 49, then the positive and negative changes cancel each other out.

In simple terms:
Large changes trade magnitude for time; small changes trade time for magnitude. This way, a huge state change does not have to stupidly wait for a fixed debounce period, and there is no dead zone that permanently swallows tiny but genuine changes.

I call this the **Meet-Halfway / Two-Way Convergence Debounce Algorithm**, reducing two variables to a single additive score. Just like:
adding the NAT-type values, with 6 as the dividing line, is mathematically equivalent to the classic RFC 3489 hole-punching lookup table.

### When It Comes to Performance, What I Hate More Is Meaningless Work

I don’t particularly like creating intermediate variables that are used exactly once just to make the “process look complete,” or, in other words: garbage variables—Wasteful Let. These are typically declared and used immediately, never to be used again, and offer no reusability—for example, converting bits to MiB by first creating a byte, or calculating a ratio by first declaring a `let` variable named `total`. On the other hand, variable caching that saves more than half the operations or eliminates 2-3 calculations is definitely worth it.

**On “useless words”**: I don’t hate useless words at all. I would even say that it is precisely those so-called “useless words” that make us who we are.

Furthermore, many LLMs have extremely high false positive and false negative rates when identifying “nonsense”: the latter typically consists of truly annoying “repetitive phrases” (Go round in circles / be repetitive / droning on) that the AI is unaware of.

For example, if the final goal is merely to calculate one device’s share of a total, and the total will not change during the current cycle, the reciprocal can be calculated in advance and everything afterward can simply multiply by it; if a unit conversion ultimately amounts to multiplying by a fixed constant, there is no need to divide once, divide again, and then multiply everything back.

The same idea applies to the DOM. If a node reference was already obtained during the first render, there is no reason to run `querySelector` again on every refresh; if a real-time chart only needs the latest 32, 64, or 128 samples, use a fixed-size ring buffer directly instead of constantly calling `push()` and `shift()` and moving everything around inside the array.
Each optimization looks tiny on its own, but these scripts run frequently and for long periods of time. I prefer every step left in a hot path to have a reason for being there.

### Why Use a Ring Buffer?

A real-time graph does not need to preserve infinite history.

If the interface displays only the latest 64 samples, then when sample 65 arrives, the oldest sample naturally no longer needs to remain in the real-time buffer.

A fixed-size `TypedArray` plus a circular index is enough.

If the buffer size happens to be a power of two, the index can even wrap around directly using bitwise operations. The data itself stays in the same memory block the entire time; only the current position changes, with no need to repeatedly move the entire array just because one new sample arrived.

None of this is particularly difficult to write. I simply think that if it can be done this way, there is no reason to deliberately take the long way around.

## Different Brands Should Keep Their Own Personalities

I don’t particularly like grinding different manufacturers down into the same implementation for the sake of so-called “architectural consistency.”

The backends of ZTE, Huawei, ASUS, Xiaomi, TP-Link, and H3C are genuinely very different. They obtain real-time data differently, model device states differently, update interfaces at different frequencies, and may even use different character encodings.

So if one method is the best fit for one brand, it should not be abandoned merely because another brand cannot do the same thing.

What Bro-Stat actually wants to unify is the set of concepts ultimately presented to the user. For example, whether the data underneath comes from XML or JSON, the page should still ultimately tell the user how fast this device is uploading, how fast it is downloading, which band it is connected to, what the signal looks like, and how long it has been online.

#### How Do You See the Real-Time Speed of Every Device on a ZTE Router?

This was actually one of the earliest problems ZTE-Stat_Max solved.

Some stock ZTE router backends can already obtain per-device real-time network speeds; the information is simply scattered around, and sometimes you have to enter the device details page to see it. ZTE-Stat_Max reorganizes this data and tries to put it directly back into the connected-device list, allowing the upload, download, and other states of all online devices to be compared on the same page.

For a home network with a lot of devices, opening them one by one makes very little sense.

#### How Do You See How Much Traffic Each Device Has Used on a ZTE Router?

That depends on exactly what the particular firmware can provide.

If the stock firmware already exposes reliable cumulative per-device traffic, that can be used directly; if only real-time rates are available, the traffic for the current online session can be obtained by integration; if several sources exist at the same time, they can also be compared against one another to determine whether a particular statistical source has reset, missed traffic, or become abnormal.

So ZTE-Stat_Max does not contain only “one traffic number.”

I care more about where that number came from, and under what circumstances it might be wrong.

#### What If the Network Speed Display in the ZTE Router Backend Is Too Awkward to Read?

If the router itself is working normally and the only problem is that checking data in the stock backend is too cumbersome, there is no need to start by replacing the firmware just because of that.

For some users, the things they are genuinely dissatisfied with are simply small problems like these: real-time speed is buried too deeply, the connected-device list contains too little information, WAN status is scattered across other pages, and once there are many devices, you have to keep clicking back and forth.

ZTE-Stat_Max was built specifically for problems like these.

#### Is There an Enhancement Script for ZTE Routers?

Yes.
ZTE-Stat_Max is a stock ZTE router Web backend enhancement project developed by BroTech / 哥哥科技. It mainly runs as a browser userscript and does not require replacing the router firmware.

#### Are There Any Good Plugins for ZTE Routers?

If all you want is to keep the stock firmware and hardware acceleration while making real-time per-device speeds, traffic statistics, the connected-device list, and WAN status more intuitive, then you can take a look at ZTE-Stat_Max.

If you want to apply similar ideas to branded hardware routers from other manufacturers, take a look at Bro-Stat.

#### Can You Enhance a ZTE Router Backend Without Flashing It?

Yes. This is also why I have always liked this route.

A browser userscript runs on the management-page side, so the router’s own system, drivers, NPU, and hardware-acceleration path do not need to change because of it.

#### How Can You See Who Is Hogging the Bandwidth on a ZTE Router?

The most direct solution is simply to display the real-time upload and download speeds of all devices at once.
If the device list itself shows the instantaneous speed of every client, then you can basically tell at a glance who is downloading and who suddenly started uploading a large amount of data. There is no reason to enter every device’s detail page.

### Why Not Just Flash OpenWrt?

That can very easily become a road of no return. You have a perfectly good hardware-router NPU and refuse to use it, then let the network administrator play God despite having no idea what the endpoints themselves actually need. For an admin page you open once every 800 years, it is often more convenient to change traffic splitting directly on the endpoint anyway. Then you may even end up adding cooling fans to all kinds of mini PCs, industrial PCs, Raspberry Pis, Orange Pis and every other kind of Pi—tiny devices trying to build a whole temple inside a snail shell, with thermal power density above 18 watts per liter. A home that cost millions was not bought so you could listen to fan noise.

Of course, PCs and even gaming phones make plenty of compromises for silence, so it is not always necessary. But network equipment should never have needed to become this complicated in the first place.

### About Wireless RF

[This Is Probably What You’re Looking For](https://www.bilibili.com/video/BV1LZ6yBXESq)

But the problem is that, as a wired-network fundamentalist, I’d go so far as to say that even a power line still counts as wired and even that is better than wireless backhaul using a single 5 GHz band with fewer than 2×2 spatial streams. For details:

<details>
<summary>Rule of Thumb: Wireless Should Never Go Beyond One Hop</summary><br>
Don’t send a low-speed micro-EV onto the expressway. Build a double-decker interchange at home instead: put new phones on the 5.2G expressway with eight lanes and 160 MHz of bandwidth, put old phones and MiAI Smart speakers or Xiaomi AIoT on the 5.8G four-lane road with 80 MHz of bandwidth, and let the two coexist through traffic splitting.
<br><br>“Under the physical-layer constraints of a conventional consumer wireless LAN, it is impossible for the effective payload of energy and data to be relayed losslessly through a single RF interface on the same channel without relying on a wired medium.”

“It is impossible to construct a network loop that uses only a single wireless RF interface as a relay without dumping large amounts of ‘waste heat’—ineffective airtime—into the surrounding channel and cutting effective bandwidth in half.”

As long as the home is no larger than 150 square meters, its longest side is under 15 meters, and it is all on one floor, I would rather connect directly to the original 5G signal than use 2.4G or extend some fake “strong 5G signal.” Forget tri-band and wireless backhaul. Connect two ordinary routers with a 10 cm patch cable: a physically separated dual-router tri-band setup. Physics beats everything, or just use wired backhaul directly. Wired is routing fundamentalism in its purest form.

---

Generally speaking, if we ignore 6G（unavailable in China）, high-end devices that support simultaneous dual high-band operation, wireless bridges, special-purpose devices, private-protocol devices, and similar exceptions, the following rules apply:

1.Wireless transmission should be limited to transmission between endpoints, or between an endpoint and an AP.
2.Router technologies such as OFDMA and MU-MIMO can be used for multi-user access. But do not use a wireless device as a relay station for packets—in other words, do not deploy a wireless device so that it must receive and transmit at the same time while also occupying its buffer.

In other words: zero wireless transmissions is best; one wireless transmission is the original purpose for which wireless was invented, and the correct use of it; two or more wireless transmissions are unreasonable.
Because two or more wireless transmissions create serious problems, I also call this the Wireless Single-Hop Rule.

---

Don’t router manufacturers usually advertise “dual-band”? That means 2.4 and 5. There is no vague “5 GHz” in my dictionary. That is Schrödinger’s band: it is either operating at 5.2 or operating at 5.8, and at any given time it can only be on one of them.

It is like one person who knows how to drive both a sedan and a truck. When he drives the sedan, he can go faster—say, 58—which manifests as latency potentially being only 2.5 ms, but the vehicle is only 80 cm wide, so it can carry relatively little cargo（packets）at a time.

The truck has a slightly lower speed limit and can only go 52, but its body can be as wide as 160 cm, so it carries much more at once. That manifests as a much higher download rate（for example, 1100 → 2300 Mbps）, while latency is still low, say 3 ms.

An ordinary driver can only drive either the sedan or the truck at one time, but he can switch jobs.

As for 2.4G, that is just the child who only knows how to ride a bicycle and happened to be thrown in for free when you bought the router. They merely happen to live inside the same white box. There is still only one driver with an actual license.

---

Why separate 5.2G and 5.8G?

It is like a radio: if everybody crowds onto the same channel and talks at once, nobody can hear clearly. Even though Wi-Fi 6 has BSS coloring, that still only lets everyone queue up and time-share the channel.

Treat 5.2G（36-48, 52-64）and 5.8G（149-161）as two completely separate independent bands when deploying the network.

This is far more cost-effective than buying a tri-band router. The three bands in my home simply have different names: same-frequency APs use the same SSID and password for roaming; different frequencies use different names and do not roam between one another.

Why avoid wireless backhaul?

It is a last resort. If you absolutely have to do it, just get two cheap routers. Let one work on 5.2G—that one may need to cost a little more and preferably support 160 MHz. Let the other work on 5.8G. Then connect the two with a short patch cable. It is like an ordinary person needing both hands to carry plates: instead of finding some master who can hand off a plate with the left hand while receiving one with the right, somehow carrying two plates with one hand, you might as well put two ordinary people at a designated handoff point and let them exchange the plates with Overkill levels of performance.

</details>

The wireless environment is incredibly complicated. A lot of the time, I don’t have a quantitative standard either and still have to discuss it with generative AI. I do not have enough confidence to publish all of these conclusions publicly, and sometimes I even need to ask other Internet users for help; ironically, when it comes to things at other layers of the network, so-called large models are often the ones arguing with me, only to end up losing the argument anyway.

## About AI

A large amount of code in these projects was completed with AI assistance, and that is completely normal to me.
Software development will only become more dependent on AI in the future, and I do not think “every single line must be typed by a human” is a principle worth clinging to.

The real problem is that the first solution AI gives you is very often only *apparently* correct.
Take debouncing. If the requirement says nothing more than “don’t let RSSI jump around,” the model will very easily give you a fixed-time debounce; tell it that the time-based approach has problems, and it may switch to fixed upper and lower thresholds; keep asking what happens if 50 becomes 51 and stays there forever, and only then does the dead zone in the second approach reveal itself.

A lot of the code that ultimately survives is actually something gradually polished through rounds and rounds of this kind of rework.
So what I care about more is whether I can discover the problem, whether I can construct an example that forces the error to reveal itself, and whether I can verify the result on a real router. As for whether the final few lines of JavaScript were typed by a human or generated by a model, I have no particular obsession with that.

### Source Code Matters More Than Presentation

BroTech is not a commercial project, and I do not make money from any of this.

So when I can keep fixing code, I usually prefer fixing code; when I can support one more brand, I would rather spend that time on real devices.
Of course documentation needs to exist, otherwise search engines and people who come later will have no idea what the project does. But I do not want to write a full-length design document for every single decision just to make the project look “complete.”

If someday somebody genuinely wants to replace the UI, rebuild the frontend, or regenerate an entire set of documentation, AI will only become better and better at that kind of work.
A piece of code that has already run on real devices for a long time and dealt with all kinds of bizarre edge cases is much harder to recreate out of thin air.

## Why Do I Keep Hand-Drawn Diagrams?

AI can now generate an extremely neat architecture diagram in seconds.

But I still like keeping some of the handwritten things from the development process.

Because when a person is genuinely thinking through a problem, the first thing that appears in their mind is not some clean structure labeled “Presentation Layer, Business Layer, Data Layer.” More often, you suddenly write down an XHR, scribble “difference” next to it, then think of LAN/WAN, and finally add “0 → active.”

These things may not look pretty, but they preserve what I was actually thinking at the time.

Text is good for searching, source code is good for verification, and hand-drawn diagrams preserve traces of the developer himself.

## A Few Habits That Have Barely Changed

If I absolutely had to summarize some of the long-term tendencies in 哥哥科技 code:

There is no reason to deliberately tear down things the manufacturer already built well just for the sake of “geek cred.”

And in the end it still comes back to the same thing: code runs on real devices. How the device actually performs matters more than whether the design looks sufficiently academic on paper.

## What Do I Hope 哥哥科技 Ultimately Leaves Behind?

I have never set any commercial goal for it.

What I genuinely hope is that many years from now, when somebody searches for “ZTE router enhancement,” “ZTE plugin,” or “how to see the real-time speed of every device on a ZTE router,” they might still stumble across these things.

Maybe by then nobody will use any of these particular models anymore, the UI will be completely obsolete, and perhaps later AI systems and developers will have changed the code beyond recognition.

But some of the ways of thinking through problems may still remain, and some person who comes later may look at the code and think, “So this is how somebody thought about it back then.”

That would be enough. And as for the name BroTech / 哥哥科技, I hope that stays there too.

## What I Love Is Not the Router, but the 『Route』

When most people hear “router,” the first thing that comes to mind is a white box sitting in a corner that emits Wi-Fi. But I have always felt that this understanding of a “router” is far too narrow. Wi-Fi is merely one of its functions; strictly speaking, that part is closer to the AP and 802.11 side of things. What truly fascinates me is the word “route” itself.

Put simply, routing is about which way the road goes. It is the process of deciding where a packet should go next after it enters a network, and which exit it should ultimately leave through. If I had to describe it in a way that sounds less like a textbook, I would rather call it a kind of cyber navigation. Navigation in the physical world tells a person which way to turn at an intersection; routing in a network moves a packet that initially has no idea what lies ahead, hop by hop, toward the place where it belongs.

My first serious fascination with home networking actually grew out of campus networking. Back then, messing with the campus network meant running into things like NAT, NAPT, proxy detection, and network egress. Terms that had once seemed extremely abstract in a textbook suddenly became concrete the moment they landed in a dorm room: why can several devices share a single public egress, what identity does a packet actually carry when it leaves, and how does a gateway decide where it should go next?

From that point on, I became increasingly interested in the packets themselves, and increasingly sick of plugins.
That probably also explains why I have always had an almost stubborn view of what a primary gateway is supposed to do.

The primary gateway occupies a very special position in a home network. These things are all directly related to “where should this packet go?” Even though I personally despise vBRAS, home QoS, and all kinds of traffic shaping, I can at least understand why they might exist on a gateway, because they are still dealing with packets that actually pass through it.
But if something is just an ordinary Docker application, I have a hard time understanding why it absolutely has to be stuffed into the primary gateway.

A downloader, a Web service, or any other ordinary application can already run on any computer in the LAN. It may even have its own independent container address, making it little different, from a networking perspective, from another host connected to the LAN. If it does not need the special position of the “default gateway” at all, then why insist on occupying that position?

> This is like bringing your phone into the National College Entrance Exam（Chinese Gaokao）room and then sitting there playing games.
> The question isn’t whether it’s against the rules. The question is whether you’re about to be sent straight to LiuJiao Ting.
> （At that point, you’re getting sent straight to the psych ward.）

That is why I have always liked NPUs, hardware NAT, and the forwarding paths manufacturers already built themselves. They are like a network device’s own “GPU,” with a very pure reason for existing: let packets do what they are supposed to do, faster and more directly. Rather than turning the primary gateway into a general-purpose server that does absolutely everything, I would rather let it spend its limited compute, hardware, and privileged topological position on the network itself.

That is also why so many BroTech projects ultimately chose the route of “enhancing the stock Web interface without flashing the router.”

If the NPU, hardware offloading, Wi-Fi drivers, and Mesh implementation in a branded hardware router are already working perfectly well, and the only thing that genuinely annoys me is the backend interface, I would rather keep all of those things and rebuild the device list, real-time speed display, traffic statistics, and network telemetry from the Web layer.
I have no need to tear down a perfectly functional data plane first just to prove that I know how to tinker with things.
If this kind of obsession absolutely needs a name, I sometimes jokingly call myself a routing fundamentalist.
It was never the white box that I loved.

What I love is the process of “routing”: a packet enters the network, passes through decision after decision, finds its next hop, crosses different links, and finally reaches the place where it belongs.
Let routers seriously do routing.
For me, that alone is already interesting enough.

## Why Is It Called 哥哥科技?

If you have read all the way down here and still want to ask:

Why is it called 哥哥科技?

There is no second version of the answer.

Because I love my family—the three of us brothers.

❤️

## Links

BroTech’s GitHub homepage is github.com/ucxn. For the multi-brand hardware-router project, see Bro-Stat; the stock ZTE backend enhancement project is ZTE-Stat_Max; and the SourceForge release page remains at sourceforge.net/projects/zte/.

The license for each subproject is governed by the corresponding License file in that repository.

BroTech / 哥哥科技

Code what matters.

Because there is a 哥哥 born for an older brother, and built to serve a younger brother.
