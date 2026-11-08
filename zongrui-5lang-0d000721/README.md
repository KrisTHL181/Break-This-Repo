# five obscure langs × five-layer encrypt `0d000721`

Same pipeline in five languages that were **not** in this repo's GitHub language breakdown at PR time:

| # | Language | File |
|--:|----------|------|
| 1 | **Nim** | `encrypt.nim` |
| 2 | **BQN** | `encrypt.bqn` |
| 3 | **Wren** | `encrypt.wren` |
| 4 | **Factor** | `encrypt.factor` |
| 5 | **Arturo** | `encrypt.art` |

## Pipeline

| Layer | Operation |
|------:|-----------|
| 0 | plaintext `0d000721` |
| 1 | reverse string |
| 2 | XOR every byte with `0x5A` |
| 3 | lowercase hex-encode |
| 4 | Base64-encode the hex text |
| 5 | reverse Base64 string |

Expected ciphertext:

```
==QY2U2MhZTY2EmNkZDO2ImN
```
