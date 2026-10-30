# -*- coding: utf-8 -*-
"""喵呜编码器：UTF-8 文本 -> 0/1 -> 「喵/呜」，并按泊松间隔插入噪声。

注意：这只是"编码/混淆"，不是加密。拿到码字（喵=0、呜=1）后任何人都能还原，
噪声只影响观感，不提供任何安全性。
"""

import random

import numpy as np

DEFAULT_NOISE = ["#", "$", "@", "&"]


def _check_codebook(zero_str: str, one_str: str, noise_list) -> None:
    """提前拦住会让解码产生歧义的参数组合。"""
    if not zero_str or not one_str:
        raise ValueError("zero_str 和 one_str 都不能为空")
    if zero_str == one_str:
        raise ValueError("zero_str 和 one_str 不能相同")
    if zero_str.startswith(one_str) or one_str.startswith(zero_str):
        raise ValueError("zero_str 和 one_str 不能互为前缀，否则无法唯一解码")

    # 只要噪声里出现任意一个编码字符，就可能被误读成 0/1（尤其是跨噪声边界的半个码字）
    code_chars = set(zero_str) | set(one_str)
    for noise in noise_list:
        if not noise:
            raise ValueError("噪声串不能为空字符串")
        overlap = code_chars & set(noise)
        if overlap:
            raise ValueError(
                f"噪声 {noise!r} 含有编码字符 {sorted(overlap)}，会导致误解码"
            )


def text2b(
    text: str,
    zero_str: str = "0",
    one_str: str = "1",
    noise_list=None,
    poisson_lambda: float = 5,
    seed=None,
) -> str:
    """把 text 编码成「zero_str/one_str + 噪声」的字符串。

    poisson_lambda 越大，平均每段有效码字越长、噪声越稀疏。
    seed 固定后结果可复现。
    """
    noise_list = list(DEFAULT_NOISE if noise_list is None else noise_list)
    _check_codebook(zero_str, one_str, noise_list)

    rng = random.Random(seed)
    nprng = np.random.default_rng(seed)

    bits = "".join(f"{byte:08b}" for byte in text.encode("utf-8"))
    tokens = [zero_str if bit == "0" else one_str for bit in bits]

    out = []
    pos = 0
    while pos < len(tokens):
        interval = max(1, int(nprng.poisson(poisson_lambda)))  # 防止间隔为 0
        end = min(pos + interval, len(tokens))
        out.append("".join(tokens[pos:end]))
        pos = end
        if noise_list and pos < len(tokens):
            out.append(rng.choice(noise_list))

    return "".join(out)


def b2text(custom_binary: str, zero_str: str = "0", one_str: str = "1", noise_list=None) -> str:
    """把「zero_str/one_str + 噪声」还原成原文。"""
    noise_list = list(DEFAULT_NOISE if noise_list is None else noise_list)
    _check_codebook(zero_str, one_str, noise_list)

    # 长码字优先匹配（正常情况下上面的校验已保证无前缀歧义）
    codebook = sorted(
        ((zero_str, "0"), (one_str, "1")), key=lambda pair: len(pair[0]), reverse=True
    )

    bits = []
    i = 0
    while i < len(custom_binary):
        for token, bit in codebook:
            if custom_binary.startswith(token, i):
                bits.append(bit)
                i += len(token)
                break
        else:
            i += 1  # 噪声，跳过

    if len(bits) % 8 != 0:
        raise ValueError(
            f"有效比特数 {len(bits)} 不是 8 的倍数，密文可能被截断，"
            "或 zero_str/one_str 设置与编码时不一致"
        )

    raw = bytes(
        int("".join(bits[i : i + 8]), 2) for i in range(0, len(bits), 8)
    )
    return raw.decode("utf-8")


if __name__ == "__main__":
    import sys

    # Windows 控制台默认 GBK，直接 print 会因 emoji 报 UnicodeEncodeError
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except AttributeError:
        pass

    noise_list = ["❤", "嗯❤啊~", "❤嗯啊~", "嗯啊❤~", "嗯❤~", "啊❤~", "❤嗯~"]

    original_text = "你好，喵呜世界！Hello 123"
    encrypted = text2b(original_text, "喵", "呜", noise_list, poisson_lambda=4, seed=42)
    decrypted = b2text(encrypted, "喵", "呜", noise_list)

    print(f"原始文本: {original_text}")
    print(f"加密结果: {encrypted}")
    print(f"解密结果: {decrypted}")
    print(f"是否还原成功: {decrypted == original_text}")
    assert decrypted == original_text

    # 控制台编码不可靠时，落一份 UTF-8 结果方便查看
    with open("meow_demo_out.txt", "w", encoding="utf-8") as fh:
        fh.write(f"原文: {original_text}\n密文: {encrypted}\n解密: {decrypted}\n")

