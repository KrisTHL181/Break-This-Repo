# Nim — 5-layer encrypt of 0d000721
# L1 reverse → L2 XOR 0x5A → L3 hex → L4 base64 → L5 reverse

import std/[base64, strutils, sequtils]

const plain = "0d000721"
let l1 = plain.reversed
var xored: seq[byte]
for ch in l1:
  xored.add(byte(ch.ord xor 0x5A))
let l3 = xored.mapIt(it.toHex(2).toLowerAscii).join()
let l4 = encode(l3)
var l5 = newString(l4.len)
for i, ch in l4:
  l5[l4.high - i] = ch
echo l5
