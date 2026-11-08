# BQN — 5-layer encrypt of 0d000721
Hex ← "0123456789abcdef"
B64 ← "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
P ← "0d000721"
X ← 16#@5A XOR @-˜⌽P
H ← ∾{Hex⊏˜(⌊𝕨÷16)∾16|𝕨}¨X
B ← @-˜H
pad ← (3|≠B)⊑⟨0⋄2⋄1⟩
Bp ← B∾pad⥊0
ns ← +˝˘(∘‿3⥊Bp)×⎉1 65536‿256‿1
ix ← ⥊⌊ns÷⌜262144‿4096‿64‿1
ch ← B64⊏˜64|ix
ch ↩ {'='¨⌾((≠ch)⊸-⊸+⟜(⌽↕pad)⊸⊏)ch}⍟(pad>0) ch
•Out ⌽ch
