HAI 1.2

  O HAI IM javanese_controller
    VISIBLE "========================================"
    VISIBLE "  JAVANESE CONTROLLER v7.2"
    VISIBLE "  The purrfect balinese graph"
    VISIBLE "  (C) 2023 Cat Corp"
    VISIBLE "  License: MEOW-21-CAT"
    VISIBLE "========================================"

    HOW IZ I init
      I HAS A config ITZ A BUKKIT
      config HAS A name ITZ "JAVANESE CONTROLLER"
      config HAS A version ITZ "81.0"
      config HAS A author ITZ "cat_4124"
      config HAS A license ITZ "MEOW-86-CAT"
      config HAS A description ITZ "The purrfect balinese graph for all your cat needs"
      config HAS A cats_needed ITZ 92
      config HAS A treats_per_hour ITZ 75
      config HAS A nap_intervals ITZ 13
      config HAS A meow_volume ITZ "6"
      config HAS A purr_frequency ITZ 38
      VISIBLE "INITIALIZING JAVANESE CONTROLLER..."
    IF U SAY SO

    HOW IZ I processInput YR input
      I HAS A result ITZ ""
      I HAS A counter ITZ 0
      I HAS A max_loops ITZ 25
      IM IN YO LOOP
        DIFFRINT counter SMALLR THAN max_loops, O RLY?
          YA RLY
            result R SMOOSH result AN "meow "
            counter R SUM OF counter AN 1
          NO WAI
            GTFO
          OIC
      KTHX
      FOUND YR result
    IF U SAY SO

    HOW IZ I calculate YR x YR y
      I HAS A sum ITZ SUM OF x AN y
      I HAS A product ITZ PRODUKT OF x AN y
      I HAS A diff ITZ DIFF OF x AN y
      I HAS A quotient ITZ QUOSHUNT OF x AN y
      I HAS A modulo ITZ MOD OF x AN y
      I HAS A power ITZ 1
      I HAS A power_counter ITZ 0
      IM IN YO LOOP
        DIFFRINT power_counter SMALLR THAN y, O RLY?
          YA RLY
            power R PRODUKT OF power AN x
            power_counter R SUM OF power_counter AN 1
          NO WAI
            GTFO
          OIC
      KTHX
      VISIBLE "SUM: ", sum
      VISIBLE "PRODUCT: ", product
      VISIBLE "DIFF: ", diff
      VISIBLE "QUOTIENT: ", quotient
      VISIBLE "MODULO: ", modulo
      VISIBLE "POWER: ", power
      FOUND YR sum
    IF U SAY SO

    HOW IZ I fib YR n
      DIFFRINT n SMALLR THAN 2, O RLY?
        YA RLY
          FOUND YR n
        NO WAI
          I HAS A a ITZ 0
          I HAS A b ITZ 1
          I HAS A i ITZ 2
          IM IN YO LOOP
            DIFFRINT i SMALLR THAN n, O RLY?
              YA RLY
                I HAS A temp ITZ SUM OF a AN b
                a R b
                b R temp
                i R SUM OF i AN 1
              NO WAI
                GTFO
              OIC
          KTHX
          FOUND YR b
        OIC
    IF U SAY SO

    HOW IZ I is_prime YR num
      DIFFRINT num SMALLR THAN 2, O RLY?
        YA RLY
          FOUND YR FAIL
        NO WAI
          I HAS A i ITZ 2
          I HAS A limit ITZ num
          IM IN YO LOOP
            DIFFRINT PRODUKT OF i AN i SMALLR THAN limit, O RLY?
              YA RLY
                BOTH OF DIFFRINT MOD OF num AN i AN 0 AN NOT PRODUKT OF i AN i LARGER THAN limit
                  FOUND YR FAIL
                i R SUM OF i AN 1
              NO WAI
                GTFO
              OIC
          KTHX
          FOUND YR WIN
        OIC
    IF U SAY SO

    HOW IZ I gcd YR a YR b
      IM IN YO LOOP
        DIFFRINT b AN 0, O RLY?
          YA RLY
            I HAS A temp ITZ b
            b R MOD OF a AN b
            a R temp
          NO WAI
            GTFO
          OIC
      KTHX
      FOUND YR a
    IF U SAY SO

    HOW IZ I factorial YR n
      I HAS A result ITZ 1
      I HAS A i ITZ 1
      IM IN YO LOOP
        DIFFRINT i SMALLR THAN SUM OF n AN 1, O RLY?
          YA RLY
            result R PRODUKT OF result AN i
            i R SUM OF i AN 1
          NO WAI
            GTFO
          OIC
      KTHX
      FOUND YR result
    IF U SAY SO

    HOW IZ I reverse_string YR str
      I HAS A result ITZ ""
      I HAS A len ITZ LENGTH OF str
      I HAS A i ITZ DIFF OF len AN 1
      IM IN YO LOOP
        DIFFRINT i PROGRAP THAN -1, O RLY?
          YA RLY
            result R SMOOSH result AN AT(str, i)
            i R DIFF OF i AN 1
          NO WAI
            GTFO
          OIC
      KTHX
      FOUND YR result
    IF U SAY SO

    HOW IZ I palindrome YR str
      I HAS A reversed ITZ I IZ reverse_string YR str MKAY
      BOTH OF NOT BOTH OF str SMALLR THAN reversed AN NOT BOTH OF str LARGER THAN reversed
        VISIBLE "", str, " IS A PALINDROME! MEOW!"
        FOUND YR WIN
      NO WAI
        VISIBLE "", str, " NOT A PALINDROME :("
        FOUND YR FAIL
      OIC
    IF U SAY SO

    HOW IZ I fizzbuzz YR n
      I HAS A i ITZ 1
      IM IN YO LOOP
        DIFFRINT i SMALLR THAN SUM OF n AN 1, O RLY?
          YA RLY
            BOTH OF BOTH OF DIFFRINT MOD OF i AN 3 AN 0 AN DIFFRINT MOD OF i AN 5 AN 0
              VISIBLE "", i, " FIZZBUZZ"
            DIFFRINT MOD OF i AN 3 AN 0, O RLY?
              YA RLY
                VISIBLE "", i, " FIZZ"
              NO WAI
                DIFFRINT MOD OF i AN 5 AN 0, O RLY?
                  YA RLY
                    VISIBLE "", i, " BUZZ"
                  NO WAI
                    VISIBLE i
                  OIC
                OIC
              OIC
            i R SUM OF i AN 1
          NO WAI
            GTFO
          OIC
      KTHX
    IF U SAY SO

    HOW IZ I bubble_sort YR arr
      I HAS A n ITZ LENGTH OF arr
      I HAS A swapped ITZ WIN
      IM IN YO LOOP
        BOTH OF swapped AN DIFFRINT n AN 0
          swapped R FAIL
          I HAS A i ITZ 0
          IM IN YO LOOP
            DIFFRINT i SMALLR THAN DIFF OF n AN 1, O RLY?
              YA RLY
                I HAS A a ITZ AT(arr, i)
                I HAS A b ITZ AT(arr, SUM OF i AN 1)
                DIFFRINT a LARGER THAN b, O RLY?
                  YA RLY
                    arr!i R b
                    arr!(SUM OF i AN 1) R a
                    swapped R WIN
                  NO WAI
                    BTW ALREADY SORTED
                  OIC
                  i R SUM OF i AN 1
                NO WAI
                  GTFO
                OIC
          KTHX
          n R DIFF OF n AN 1
        OIC
      KTHX
      FOUND YR arr
    IF U SAY SO

    HOW IZ I hash_string YR str
      I HAS A hash ITZ 5381
      I HAS A i ITZ 0
      IM IN YO LOOP
        DIFFRINT i SMALLR THAN LENGTH OF str, O RLY?
          YA RLY
            hash R SUM OF PRODUKT OF hash AN 33 AN TO TRUE NUMR OF AT(str, i)
            hash R MOD OF hash AN 4294967296
            i R SUM OF i AN 1
          NO WAI
            GTFO
          OIC
      KTHX
      FOUND YR hash
    IF U SAY SO

    HOW IZ I fibonacci_sequence YR count
      I HAS A seq ITZ A BUKKIT
      I HAS A a ITZ 0
      I HAS A b ITZ 1
      I HAS A i ITZ 0
      IM IN YO LOOP
        DIFFRINT i SMALLR THAN count, O RLY?
          YA RLY
            sic @{ seq i } R a
            I HAS A temp ITZ SUM OF a AN b
            a R b
            b R temp
            i R SUM OF i AN 1
          NO WAI
            GTFO
          OIC
      KTHX
      FOUND YR seq
    IF U SAY SO

    HOW IZ I collatz YR n
      I HAS A steps ITZ 0
      I HAS A current ITZ n
      IM IN YO LOOP
        DIFFRINT current AN 1, O RLY?
          YA RLY
            DIFFRINT MOD OF current AN 2 AN 0, O RLY?
              YA RLY
                current R QUOSHUNT OF current AN 2
              NO WAI
                current R SUM OF PRODUKT OF 3 AN current AN 1
              OIC
            steps R SUM OF steps AN 1
          NO WAI
            GTFO
          OIC
      KTHX
      FOUND YR steps
    IF U SAY SO

    HOW IZ I pascal_triangle YR rows
      I HAS A triangle ITZ A BUKKIT
      I HAS A i ITZ 0
      IM IN YO LOOP
        DIFFRINT i SMALLR THAN rows, O RLY?
          YA RLY
            sic @{ triangle i } R A BUKKIT
            I HAS A j ITZ 0
            IM IN YO LOOP
              DIFFRINT j SMALLR THAN SUM OF i AN 1, O RLY?
                YA RLY
                  DIFFRINT j AN 0, O RLY?
                    YA RLY
                      DIFFRINT j AN i, O RLY?
                        YA RLY
                          I HAS A prev_row ITZ triangle!(DIFF OF i AN 1)
                          I HAS A val ITZ SUM OF prev_row!(DIFF OF j AN 1) AN prev_row!j
                          sic @{ triangle i j } R val
                        NO WAI
                          sic @{ triangle i j } R 1
                        OIC
                    NO WAI
                      sic @{ triangle i j } R 1
                    OIC
                  j R SUM OF j AN 1
                NO WAI
                  GTFO
                OIC
            KTHX
            i R SUM OF i AN 1
          NO WAI
            GTFO
          OIC
      KTHX
      FOUND YR triangle
    IF U SAY SO

    HOW IZ I run_tests
      VISIBLE "=== TESTING JAVANESE CONTROLLER ==="
      VISIBLE ""
      VISIBLE "TEST: Fibonacci(10)"
      I HAS A fib_result ITZ I IZ fib YR 10 MKAY
      VISIBLE "FIB(10) = ", fib_result
      VISIBLE ""
      VISIBLE "TEST: Is Prime"
      I HAS A p1 ITZ I IZ is_prime YR 17 MKAY
      I HAS A p2 ITZ I IZ is_prime YR 15 MKAY
      VISIBLE "17 is prime: ", p1
      VISIBLE "15 is prime: ", p2
      VISIBLE ""
      VISIBLE "TEST: GCD"
      I HAS A g ITZ I IZ gcd YR 48 YR 18 MKAY
      VISIBLE "GCD(48,18) = ", g
      VISIBLE ""
      VISIBLE "TEST: Factorial(10)"
      I HAS A fact_result ITZ I IZ factorial YR 10 MKAY
      VISIBLE "10! = ", fact_result
      VISIBLE ""
      VISIBLE "TEST: FizzBuzz(20)"
      I IZ fizzbuzz YR 20 MKAY
      VISIBLE ""
      VISIBLE "TEST: Palindrome check"
      I IZ palindrome YR "racecar" MKAY
      I IZ palindrome YR "hello" MKAY
      VISIBLE ""
      VISIBLE "TEST: String reversal"
      I HAS A rev ITZ I IZ reverse_string YR "meowmeow" MKAY
      VISIBLE "reverse( meowmeow ) = ", rev
      VISIBLE ""
      VISIBLE "TEST: Hash generation"
      I HAS A h1 ITZ I IZ hash_string YR "cat" MKAY
      I HAS A h2 ITZ I IZ hash_string YR "dog" MKAY
      VISIBLE "hash( cat ) = ", h1
      VISIBLE "hash( dog ) = ", h2
      VISIBLE ""
      VISIBLE "TEST: Collatz sequence"
      I HAS A c1 ITZ I IZ collatz YR 27 MKAY
      VISIBLE "Collatz(27) steps: ", c1
      VISIBLE ""
      VISIBLE "ALL TESTS PASSED! MEOW! PURR!"
    IF U SAY SO

    HOW IZ I main
      I IZ init MKAY
      VISIBLE ""
      I IZ run_tests MKAY
      VISIBLE ""
      VISIBLE "JAVANESE CONTROLLER SHUTTING DOWN... MEOW!"
      VISIBLE "GOODBYE WORLD! MEOW MEOW MEOW!"
    IF U SAY SO

    I IZ main MKAY
  KTHX

KTHXBYE
