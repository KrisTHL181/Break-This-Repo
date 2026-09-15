use std::path::Path;

use fallow_extract::parse_from_content;
use fallow_types::discover::FileId;

#[test]
fn skips_unrelated_calls_with_framework_evidence() {
    let module = parse_from_content(
        FileId(0),
        Path::new("src/app.ts"),
        r#"
        import "elysia";
        import "fastify";
        import "@trpc/server";
        import "hono";
        import "@nestjs/common";
        import "express-validator";
        console.log("ready");
        metrics.record("request");
        new OtherPipe();
        "#,
    );

    assert!(!module.security_control_sites.iter().any(|control| {
        matches!(
            control.callee_path.as_str(),
            "elysia.route.validation"
                | "fastify.route.schema"
                | "trpc.procedure.input"
                | "hono.validator"
                | "nestjs.validation-pipe"
                | "express-validator.middleware"
        )
    }));
}
