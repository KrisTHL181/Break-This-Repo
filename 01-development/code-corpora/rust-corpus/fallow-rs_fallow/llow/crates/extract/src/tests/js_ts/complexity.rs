use crate::tests::parse_ts_with_complexity as parse_source;

#[test]
fn complexity_basic_if_else_for_while_switch() {
    let info = parse_source(
        r"function basic(x: number) {
            if (x > 10) {
                return 'big';
            } else {
                for (let i = 0; i < x; i++) {}
                while (x > 0) { x--; }
                switch (x) {
                    case 0: break;
                    case 1: break;
                    default: break;
                }
            }
        }",
    );
    let f = info.complexity.iter().find(|c| c.name == "basic").unwrap();
    assert_eq!(f.cyclomatic, 6);
}

#[test]
fn complexity_nested_if_in_for_loop() {
    let info = parse_source(
        r"function nested(items: number[]) {
            for (const item of items) {
                if (item > 0) {
                    return item;
                }
            }
        }",
    );
    let f = info.complexity.iter().find(|c| c.name == "nested").unwrap();
    assert_eq!(f.cyclomatic, 3);
    assert_eq!(f.cognitive, 3);
}

#[test]
fn complexity_deeply_nested_three_levels() {
    let info = parse_source(
        r"function deep(a: boolean, b: boolean, c: boolean) {
            if (a) {
                for (let i = 0; i < 10; i++) {
                    while (b) {
                        if (c) {
                            break;
                        }
                    }
                }
            }
        }",
    );
    let f = info.complexity.iter().find(|c| c.name == "deep").unwrap();
    assert_eq!(f.cyclomatic, 5);
    assert_eq!(f.cognitive, 10);
}

#[test]
fn complexity_boolean_same_operator_sequence() {
    let info = parse_source(
        "function sameBool(a: boolean, b: boolean, c: boolean) { return a && b && c; }",
    );
    let f = info
        .complexity
        .iter()
        .find(|c| c.name == "sameBool")
        .unwrap();
    assert_eq!(f.cyclomatic, 3);
    assert_eq!(f.cognitive, 1);
}

#[test]
fn complexity_boolean_mixed_operator_sequence() {
    let info = parse_source(
        "function mixedBool(a: boolean, b: boolean, c: boolean) { return a && b || c; }",
    );
    let f = info
        .complexity
        .iter()
        .find(|c| c.name == "mixedBool")
        .unwrap();
    assert_eq!(f.cyclomatic, 3);
    assert_eq!(f.cognitive, 2);
}

#[test]
fn complexity_boolean_three_operator_changes() {
    let info = parse_source(
        "function threeBool(a: boolean, b: boolean, c: boolean, d: boolean) { return a && b || c && d; }",
    );
    let f = info
        .complexity
        .iter()
        .find(|c| c.name == "threeBool")
        .unwrap();
    assert_eq!(f.cyclomatic, 4);
    assert_eq!(f.cognitive, 3);
}

#[test]
fn complexity_ternary_operator() {
    let info = parse_source("function tern(x: number) { return x > 0 ? 'pos' : 'non-pos'; }");
    let f = info.complexity.iter().find(|c| c.name == "tern").unwrap();
    assert_eq!(f.cyclomatic, 2);
    assert_eq!(f.cognitive, 1);
}

#[test]
fn complexity_nested_ternary() {
    let info = parse_source(
        "function nestedTern(x: number) { return x > 0 ? 'pos' : x < 0 ? 'neg' : 'zero'; }",
    );
    let f = info
        .complexity
        .iter()
        .find(|c| c.name == "nestedTern")
        .unwrap();
    assert_eq!(f.cyclomatic, 3);
    assert_eq!(f.cognitive, 3);
}

#[test]
fn complexity_try_catch() {
    let info = parse_source(
        r"function tryCatch() {
            try {
                riskyOp();
            } catch (e) {
                handleError(e);
            }
        }",
    );
    let f = info
        .complexity
        .iter()
        .find(|c| c.name == "tryCatch")
        .unwrap();
    assert_eq!(f.cyclomatic, 2);
    assert_eq!(f.cognitive, 1);
}

#[test]
fn complexity_try_catch_with_nested_if() {
    let info = parse_source(
        r"function tryCatchNested(x: boolean) {
            try {
                if (x) { riskyOp(); }
            } catch (e) {
                if (e instanceof Error) { log(e); }
            }
        }",
    );
    let f = info
        .complexity
        .iter()
        .find(|c| c.name == "tryCatchNested")
        .unwrap();
    assert_eq!(f.cyclomatic, 4);
    assert_eq!(f.cognitive, 4);
}

#[test]
fn complexity_nested_functions_independent() {
    let info = parse_source(
        r"function outer(x: boolean) {
            if (x) {}
            function inner(y: boolean) {
                if (y) {
                    if (y) {}
                }
            }
        }",
    );
    let outer = info.complexity.iter().find(|c| c.name == "outer").unwrap();
    let inner = info.complexity.iter().find(|c| c.name == "inner").unwrap();
    assert_eq!(outer.cyclomatic, 2);
    assert_eq!(outer.cognitive, 1);
    assert_eq!(inner.cyclomatic, 3);
    assert_eq!(inner.cognitive, 3);
}

#[test]
fn complexity_arrow_function_in_callback() {
    let info = parse_source(
        r"function process(items: number[]) {
            items.map((item) => {
                if (item > 0) {
                    return item * 2;
                }
                return 0;
            });
        }",
    );
    let outer = info
        .complexity
        .iter()
        .find(|c| c.name == "process")
        .unwrap();
    let arrow = info
        .complexity
        .iter()
        .find(|c| c.name == "<arrow>")
        .unwrap();
    assert_eq!(outer.cyclomatic, 1);
    assert_eq!(outer.cognitive, 0);
    assert_eq!(arrow.cyclomatic, 2);
    assert_eq!(arrow.cognitive, 1);
}

#[test]
fn complexity_named_arrow_in_variable() {
    let info = parse_source(
        r"function process(items: number[]) {
            const filter = (item: number) => item > 0;
            return items.filter(filter);
        }",
    );
    let arrow = info.complexity.iter().find(|c| c.name == "filter").unwrap();
    assert_eq!(arrow.cyclomatic, 1);
    assert_eq!(arrow.cognitive, 0);
}

#[test]
fn complexity_class_methods_independent() {
    let info = parse_source(
        r"class Parser {
            parse(input: string) {
                if (input.length === 0) { return null; }
                for (let i = 0; i < input.length; i++) {
                    if (input[i] === '{') { return this.parseObject(input); }
                }
                return input;
            }
            validate(input: string) {
                return input ? true : false;
            }
        }",
    );
    let parse = info.complexity.iter().find(|c| c.name == "parse").unwrap();
    let validate = info
        .complexity
        .iter()
        .find(|c| c.name == "validate")
        .unwrap();
    assert_eq!(parse.cyclomatic, 4);
    assert_eq!(parse.cognitive, 4);
    assert_eq!(validate.cyclomatic, 2);
    assert_eq!(validate.cognitive, 1);
}

#[test]
fn complexity_class_property_arrow() {
    let info = parse_source(
        r"class Handler {
            handle = (x: number) => {
                if (x > 0) { return x; }
                return 0;
            };
        }",
    );
    let handle = info.complexity.iter().find(|c| c.name == "handle").unwrap();
    assert_eq!(handle.cyclomatic, 2);
    assert_eq!(handle.cognitive, 1);
}

#[test]
fn complexity_nullish_coalescing() {
    let info = parse_source("function nc(a?: string) { return a ?? 'default'; }");
    let f = info.complexity.iter().find(|c| c.name == "nc").unwrap();
    assert_eq!(f.cyclomatic, 2);
    assert_eq!(f.cognitive, 1);
}

#[test]
fn complexity_nullish_coalescing_chain() {
    let info =
        parse_source("function ncChain(a?: string, b?: string) { return a ?? b ?? 'default'; }");
    let f = info
        .complexity
        .iter()
        .find(|c| c.name == "ncChain")
        .unwrap();
    assert_eq!(f.cyclomatic, 3);
    assert_eq!(f.cognitive, 1);
}

#[test]
fn complexity_logical_and_assignment() {
    let info = parse_source("function la(obj: any) { obj.value &&= 'assigned'; }");
    let f = info.complexity.iter().find(|c| c.name == "la").unwrap();
    assert_eq!(f.cyclomatic, 2);
}

#[test]
fn complexity_logical_or_assignment() {
    let info = parse_source("function lo(obj: any) { obj.value ||= 'fallback'; }");
    let f = info.complexity.iter().find(|c| c.name == "lo").unwrap();
    assert_eq!(f.cyclomatic, 2);
}

#[test]
fn complexity_nullish_assignment() {
    let info = parse_source("function na(obj: any) { obj.value ??= 'default'; }");
    let f = info.complexity.iter().find(|c| c.name == "na").unwrap();
    assert_eq!(f.cyclomatic, 2);
}

#[test]
fn complexity_all_logical_assignments() {
    let info = parse_source("function allAssign(o: any) { o.a &&= 1; o.b ||= 2; o.c ??= 3; }");
    let f = info
        .complexity
        .iter()
        .find(|c| c.name == "allAssign")
        .unwrap();
    assert_eq!(f.cyclomatic, 4);
}

#[test]
fn complexity_optional_chaining_cyclomatic_only() {
    let info = parse_source("function oc(obj: any) { return obj?.a?.b; }");
    let f = info.complexity.iter().find(|c| c.name == "oc").unwrap();
    assert!(
        f.cyclomatic >= 2,
        "optional chaining should add to cyclomatic"
    );
    assert_eq!(f.cognitive, 0);
}

#[test]
fn complexity_do_while_loop() {
    let info = parse_source(
        r"function doWhile(x: number) {
            do {
                x--;
            } while (x > 0);
        }",
    );
    let f = info
        .complexity
        .iter()
        .find(|c| c.name == "doWhile")
        .unwrap();
    assert_eq!(f.cyclomatic, 2);
    assert_eq!(f.cognitive, 1);
}

#[test]
fn complexity_for_in_loop() {
    let info = parse_source(
        r"function forIn(obj: Record<string, number>) {
            for (const key in obj) {
                if (obj[key] > 0) {}
            }
        }",
    );
    let f = info.complexity.iter().find(|c| c.name == "forIn").unwrap();
    assert_eq!(f.cyclomatic, 3);
    assert_eq!(f.cognitive, 3);
}

#[test]
fn complexity_switch_cognitive_is_flat() {
    let info = parse_source(
        r"function sw(x: number) {
            switch (x) {
                case 1: return 'one';
                case 2: return 'two';
                case 3: return 'three';
                default: return 'other';
            }
        }",
    );
    let f = info.complexity.iter().find(|c| c.name == "sw").unwrap();
    assert_eq!(f.cyclomatic, 4);
    assert_eq!(f.cognitive, 1);
}

#[test]
fn complexity_else_if_chain_cognitive_flat() {
    let info = parse_source(
        r"function elseIfChain(x: number) {
            if (x === 1) {
                return 'one';
            } else if (x === 2) {
                return 'two';
            } else if (x === 3) {
                return 'three';
            } else {
                return 'other';
            }
        }",
    );
    let f = info
        .complexity
        .iter()
        .find(|c| c.name == "elseIfChain")
        .unwrap();
    assert_eq!(f.cyclomatic, 4);
    assert_eq!(f.cognitive, 4);
}

#[test]
fn complexity_break_with_label() {
    let info = parse_source(
        r"function labeled() {
            outer: for (let i = 0; i < 10; i++) {
                for (let j = 0; j < 10; j++) {
                    if (i + j > 5) {
                        break outer;
                    }
                }
            }
        }",
    );
    let f = info
        .complexity
        .iter()
        .find(|c| c.name == "labeled")
        .unwrap();
    assert_eq!(f.cyclomatic, 4);
    assert_eq!(f.cognitive, 7);
}

#[test]
fn complexity_continue_with_label() {
    let info = parse_source(
        r"function labeledContinue() {
            outer: for (let i = 0; i < 10; i++) {
                for (let j = 0; j < 10; j++) {
                    if (j === 3) {
                        continue outer;
                    }
                }
            }
        }",
    );
    let f = info
        .complexity
        .iter()
        .find(|c| c.name == "labeledContinue")
        .unwrap();
    assert_eq!(f.cognitive, 7);
}

#[test]
fn complexity_mixed_boolean_with_nullish() {
    let info = parse_source(
        "function mixedNullish(a: boolean, b?: string) { return a && b ?? 'default'; }",
    );
    let f = info
        .complexity
        .iter()
        .find(|c| c.name == "mixedNullish")
        .unwrap();
    assert_eq!(f.cyclomatic, 3);
    assert_eq!(f.cognitive, 2);
}

#[test]
fn complexity_boolean_in_if_condition() {
    let info = parse_source(
        r"function boolInIf(a: boolean, b: boolean) {
            if (a && b) {
                return true;
            }
            return false;
        }",
    );
    let f = info
        .complexity
        .iter()
        .find(|c| c.name == "boolInIf")
        .unwrap();
    assert_eq!(f.cyclomatic, 3);
    assert_eq!(f.cognitive, 2);
}

#[test]
fn complexity_multiple_independent_functions() {
    let info = parse_source(
        r"
        function a(x: boolean) { if (x) {} }
        function b(x: boolean, y: boolean) { if (x) { if (y) {} } }
        function c() {}
        ",
    );
    let fa = info.complexity.iter().find(|c| c.name == "a").unwrap();
    let fb = info.complexity.iter().find(|c| c.name == "b").unwrap();
    let fc = info.complexity.iter().find(|c| c.name == "c").unwrap();
    assert_eq!(fa.cyclomatic, 2);
    assert_eq!(fa.cognitive, 1);
    assert_eq!(fb.cyclomatic, 3);
    assert_eq!(fb.cognitive, 3);
    assert_eq!(fc.cyclomatic, 1);
    assert_eq!(fc.cognitive, 0);
}

#[test]
fn complexity_export_default_anonymous_function() {
    let info = parse_source("export default function() { if (true) { while (true) {} } }");
    let f = info
        .complexity
        .iter()
        .find(|c| c.name == "default")
        .unwrap();
    assert_eq!(f.cyclomatic, 3);
    assert_eq!(f.cognitive, 3);
}

#[test]
fn complexity_object_method_shorthand() {
    let info = parse_source(
        r"const obj = {
            process(x: number) {
                if (x > 0) { return x; }
                return 0;
            }
        };",
    );
    let f = info
        .complexity
        .iter()
        .find(|c| c.name == "process")
        .unwrap();
    assert_eq!(f.cyclomatic, 2);
    assert_eq!(f.cognitive, 1);
}

#[test]
fn complexity_catch_increases_nesting() {
    let info = parse_source(
        r"function tryCatchDeep() {
            try {
                riskyOp();
            } catch (e) {
                if (e instanceof Error) {
                    for (const c of e.message) {
                        log(c);
                    }
                }
            }
        }",
    );
    let f = info
        .complexity
        .iter()
        .find(|c| c.name == "tryCatchDeep")
        .unwrap();
    assert_eq!(f.cyclomatic, 4);
    assert_eq!(f.cognitive, 6);
}
