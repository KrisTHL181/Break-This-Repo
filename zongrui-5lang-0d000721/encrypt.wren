// Wren — 5-layer encrypt of 0d000721
// L1 reverse → L2 XOR 0x5A → L3 hex → L4 base64 → L5 reverse

class Util {
  static reverse(s) {
    var r = ""
    var i = s.count - 1
    while (i >= 0) {
      r = r + s[i]
      i = i - 1
    }
    return r
  }

  static b64encode(bytes) {
    var table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    var out = ""
    var i = 0
    while (i < bytes.count) {
      var b0 = bytes[i]
      var has1 = i + 1 < bytes.count
      var has2 = i + 2 < bytes.count
      var b1 = has1 ? bytes[i + 1] : 0
      var b2 = has2 ? bytes[i + 2] : 0
      var n = (b0 << 16) + (b1 << 8) + b2
      out = out + table[(n >> 18) & 63]
      out = out + table[(n >> 12) & 63]
      out = out + (has1 ? table[(n >> 6) & 63] : "=")
      out = out + (has2 ? table[n & 63] : "=")
      i = i + 3
    }
    return out
  }
}

var plain = "0d000721"
var l1 = Util.reverse(plain)
var xored = []
for (b in l1.bytes) {
  xored.add(b ^ 0x5A)
}
var hexDigits = "0123456789abcdef"
var l3 = ""
for (b in xored) {
  l3 = l3 + hexDigits[(b >> 4) & 15] + hexDigits[b & 15]
}
var l4 = Util.b64encode(l3.bytes)
var l5 = Util.reverse(l4)
System.print(l5)
