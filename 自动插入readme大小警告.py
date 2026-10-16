#!/usr/bin/env python3
import re
from pathlib import Path

README = Path("README.md")
BACKUP = Path("README.md.bak")
set_backup = False

LIMIT = 500 * 1024
RESERVE = 1024

new_block = """## 注意
> [!NOTE]
> 你到达了主页渲染的尽头
>
> 如果你在 GitHub 的仓库主页看到了这段文字，说明你已经滚到了主页能显示的最远处。
> 主页的 README 渲染存在长度限制，**后面的内容主页不再显示**。
>
> 如果您在仓库主页看到了本文，不要惊慌，不要着急，请站稳扶好，安定坐下，本消息是为了告诉你，你需要换个地方才能阅读 `README` 接下面的文本。
> 
> 请移步 [README.markdown](./README.md) （文件页面）查看，这是由于仓库主页的 `README` 的显示存在比文件更短的长度限制，导致无法完全显示。
> (望后人，如若位置变更，请同步移动（或者使用我写的脚本 [自动插入readme大小警告](./自动插入readme大小警告.py) ），谢谢)
---

"""
if set backup:
    # 1. 读取并备份
    original_bytes = README.read_bytes()
    BACKUP.write_bytes(original_bytes)
    text = original_bytes.decode("utf-8")

# 2. 删除上一次插入的“主页渲染尽头”块
pattern_old = re.compile(
    r"## 注意\n> \[!NOTE\]\n> 你到达了主页渲染的尽头.*?---\n",
    re.DOTALL,
)
text, n = pattern_old.subn("", text)
print(f"删除了 {n} 个旧的主页渲染尽头块")

# 3. 计算插入位置并插入新块
data_cleaned = text.encode("utf-8")
new_bytes = new_block.encode("utf-8")

max_insert = LIMIT - RESERVE - len(new_bytes)
if max_insert < 0:
    raise SystemExit("新块太大，无法在限制内插入")

prefix = data_cleaned[:max_insert]
pos = prefix.rfind(b"\n\n")
if pos != -1:
    insert_at = pos + 2
else:
    pos = prefix.rfind(b"\n")
    insert_at = pos + 1 if pos != -1 else max_insert

data_with_new = data_cleaned[:insert_at] + new_bytes + data_cleaned[insert_at:]
text_final = data_with_new.decode("utf-8")

# 4. 在插入后的文本中，按新块的独特标记反查它的真实字符位置
anchor = "你到达了主页渲染的尽头"
anchor_pos = text_final.find(anchor)
if anchor_pos == -1:
    raise SystemExit("严重错误：插入后找不到新块标记")

block_start = text_final.rfind("## 注意", 0, anchor_pos)
if block_start == -1:
    block_start = anchor_pos

new_block_line = text_final[:block_start].count("\n") + 1
print(f"新块插入位置：第 {new_block_line} 行")

# 5. 把原有 "(现在在 N 行)" 里的 N 改成新块行号
marker = re.search(r"[（(]现在在 \d+ 行[）)]", text_final)
if marker:
    before = text_final[:marker.start()]
    note_start = before.rfind("## 注意")
    if note_start != -1:
        orig_line = text_final[:note_start].count("\n") + 1
        print(f"原有 '## 注意' 块位置：第 {orig_line} 行")
        text_final = (
            text_final[:marker.start()]
            + f"(现在在 {new_block_line} 行)"
            + text_final[marker.end():]
        )
        print(f"已将 '(现在在 N 行)' 更新为 {new_block_line}")
    else:
        print("警告：找到 '(现在在 N 行)'，但往前找不到 '## 注意'")
else:
    print("警告：未找到 '(现在在 N 行)' 字段，未更新行号")

# 6. 写回
README.write_bytes(text_final.encode("utf-8"))

print(f"原文件大小: {len(original_bytes)} bytes")
print(f"主页渲染上限: {LIMIT} bytes (预留 {RESERVE} bytes)")
print(f"新段结束位置: {insert_at + len(new_bytes)} bytes")
print(f"距上限剩余: {LIMIT - (insert_at + len(new_bytes))} bytes")
print("已写入 README.md，备份为 README.md.bak")