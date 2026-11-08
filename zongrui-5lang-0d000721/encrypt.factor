! Factor — 5-layer encrypt of 0d000721
! L1 reverse → L2 XOR 0x5A → L3 hex → L4 base64 → L5 reverse
USING: io sequences strings kernel math math.bitwise
base64 hex ascii ;
IN: zongrui.encrypt

: xor5a ( seq -- seq ) [ 0x5A bitxor ] map ;
: encrypt-0d000721 ( -- str )
  "0d000721"
  reverse
  >byte-array xor5a
  bytes>hex
  >byte-array >base64
  reverse ;

: main ( -- ) encrypt-0d000721 print ;

MAIN: main
