#![cfg(feature = "render")]

use obscura_cdp::dispatch::{dispatch, CdpContext};
use obscura_cdp::types::CdpRequest;
use serde_json::{json, Value};
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::TcpListener;

async fn serve_fixture() -> String {
    let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
    let addr = listener.local_addr().unwrap();
    tokio::spawn(async move {
        let (mut socket, _) = listener.accept().await.unwrap();
        let mut buf = [0u8; 2048];
        let _ = socket.read(&mut buf).await.unwrap();
        let body = r#"<!doctype html><html><head><style>
            html, body { margin: 0; }
            #page { width: 1800px; height: 2400px; }
            #box { position: absolute; left: 20px; top: 20px; width: 180px;
                   height: 120px; overflow: auto; border: 10px solid black; }
            #inner { width: 700px; height: 800px; }
        </style></head><body>
          <div id="page"></div>
          <div id="box"><div id="inner"></div></div>
          <input id="check" type="checkbox">
          <form id="radio-form">
            <input id="radio-a" type="radio" name="choice" checked>
            <input id="radio-b" type="radio" name="choice">
          </form>
        </body></html>"#;
        let response = format!(
            "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nContent-Length: {}\r\nConnection: close\r\n\r\n{body}",
            body.len()
        );
        let _ = socket.write_all(response.as_bytes()).await;
    });
    format!("http://{addr}/")
}

async fn cdp(
    ctx: &mut CdpContext,
    id: u64,
    method: &str,
    params: Value,
    session_id: &str,
) -> Value {
    let response = dispatch(
        &CdpRequest {
            id,
            method: method.to_string(),
            params,
            session_id: Some(session_id.to_string()),
        },
        ctx,
    )
    .await;
    assert!(response.error.is_none(), "CDP {method} failed: {:?}", response.error);
    response.result.unwrap_or_else(|| json!({}))
}

async fn evaluate(ctx: &mut CdpContext, id: u64, expression: &str, session_id: &str) -> Value {
    cdp(
        ctx,
        id,
        "Runtime.evaluate",
        json!({"expression": expression, "returnByValue": true, "awaitPromise": true}),
        session_id,
    )
    .await
}

async fn setup() -> (CdpContext, String) {
    std::env::set_var("OBSCURA_ALLOW_PRIVATE_NETWORK", "1");
    let url = serve_fixture().await;
    let mut ctx = CdpContext::new();
    let page_id = ctx.create_page();
    let session_id = "input-mouse-session";
    ctx.sessions.insert(session_id.to_string(), page_id);
    cdp(
        &mut ctx,
        1,
        "Page.navigate",
        json!({"url": url, "waitUntil": "load"}),
        session_id,
    )
    .await;
    (ctx, session_id.to_string())
}

async fn wheel(ctx: &mut CdpContext, id: u64, sid: &str, x: f64, y: f64, dx: f64, dy: f64) {
    cdp(
        ctx,
        id,
        "Input.dispatchMouseEvent",
        json!({"type": "mouseWheel", "x": x, "y": y, "deltaX": dx, "deltaY": dy}),
        sid,
    )
    .await;
}

async fn scroll_state(ctx: &mut CdpContext, id: u64, sid: &str) -> Value {
    let result = evaluate(
        ctx,
        id,
        r#"JSON.stringify({
            rootX: scrollX, rootY: scrollY,
            boxX: document.getElementById('box').scrollLeft,
            boxY: document.getElementById('box').scrollTop,
            rootScrollWidth: document.scrollingElement.scrollWidth,
            rootClientWidth: document.scrollingElement.clientWidth,
            pageRect: document.getElementById('page').getBoundingClientRect().toJSON(),
            maxBoxX: document.getElementById('box').scrollWidth - document.getElementById('box').clientWidth,
            maxBoxY: document.getElementById('box').scrollHeight - document.getElementById('box').clientHeight
        })"#,
        sid,
    )
    .await;
    serde_json::from_str(result["result"]["value"].as_str().unwrap()).unwrap()
}

#[tokio::test(flavor = "current_thread")]
async fn wheel_over_page_scrolls_the_root_on_both_axes() {
    let (mut ctx, sid) = setup().await;
    wheel(&mut ctx, 2, &sid, 600.0, 300.0, 45.0, 160.0).await;
    let state = scroll_state(&mut ctx, 3, &sid).await;
    assert_eq!(state["rootX"], 45.0, "unexpected root geometry: {state}");
    assert_eq!(state["rootY"], 160.0);
    assert_eq!(state["boxX"], 0.0);
    assert_eq!(state["boxY"], 0.0);
}

#[tokio::test(flavor = "current_thread")]
async fn wheel_over_nested_overflow_scrolls_the_nested_container() {
    let (mut ctx, sid) = setup().await;
    wheel(&mut ctx, 2, &sid, 50.0, 50.0, 70.0, 110.0).await;
    let state = scroll_state(&mut ctx, 3, &sid).await;
    assert_eq!(state["boxX"], 70.0);
    assert_eq!(state["boxY"], 110.0);
    assert_eq!(state["rootX"], 0.0, "nested wheel must not leak to the viewport");
    assert_eq!(state["rootY"], 0.0, "nested wheel must not leak to the viewport");
}

#[tokio::test(flavor = "current_thread")]
async fn wheel_offsets_clamp_to_nested_scroll_extents() {
    let (mut ctx, sid) = setup().await;
    wheel(&mut ctx, 2, &sid, 50.0, 50.0, 100_000.0, 100_000.0).await;
    let state = scroll_state(&mut ctx, 3, &sid).await;
    assert_eq!(state["boxX"], state["maxBoxX"]);
    assert_eq!(state["boxY"], state["maxBoxY"]);

    wheel(&mut ctx, 4, &sid, 50.0, 50.0, -100_000.0, -100_000.0).await;
    let state = scroll_state(&mut ctx, 5, &sid).await;
    assert_eq!(state["boxX"], 0.0);
    assert_eq!(state["boxY"], 0.0);
}

#[tokio::test(flavor = "current_thread")]
async fn wheel_chains_to_root_when_nested_scroller_is_saturated() {
    let (mut ctx, sid) = setup().await;
    evaluate(
        &mut ctx,
        2,
        "(() => { const box = document.getElementById('box'); box.scrollTop = box.scrollHeight; })()",
        &sid,
    )
    .await;
    let saturated = scroll_state(&mut ctx, 3, &sid).await;
    assert_eq!(saturated["boxY"], saturated["maxBoxY"]);

    wheel(&mut ctx, 4, &sid, 50.0, 50.0, 0.0, 90.0).await;
    let state = scroll_state(&mut ctx, 5, &sid).await;
    assert_eq!(state["boxY"], state["maxBoxY"], "inner remains clamped");
    assert_eq!(state["rootY"], 90.0, "remaining wheel gesture chains to the viewport");
}

#[tokio::test(flavor = "current_thread")]
async fn canceling_wheel_prevents_its_scroll_default() {
    let (mut ctx, sid) = setup().await;
    evaluate(
        &mut ctx,
        2,
        r#"(() => {
            globalThis.wheelProbe = null;
            const page = document.getElementById('page');
            document.elementFromPoint = () => page;
            page.addEventListener('wheel', event => {
                wheelProbe = {
                    x: event.clientX, y: event.clientY,
                    dx: event.deltaX, dy: event.deltaY,
                    ctrl: event.ctrlKey, trusted: event.isTrusted
                };
                event.preventDefault();
            });
        })()"#,
        &sid,
    )
    .await;
    cdp(
        &mut ctx,
        3,
        "Input.dispatchMouseEvent",
        json!({
            "type": "mouseWheel", "x": 600.0, "y": 300.0,
            "deltaX": 25.0, "deltaY": 75.0, "modifiers": 2
        }),
        &sid,
    )
    .await;
    let state = scroll_state(&mut ctx, 4, &sid).await;
    assert_eq!(state["rootX"], 0.0);
    assert_eq!(state["rootY"], 0.0);
    let probe = evaluate(&mut ctx, 5, "JSON.stringify(wheelProbe)", &sid).await;
    let probe: Value = serde_json::from_str(probe["result"]["value"].as_str().unwrap()).unwrap();
    assert_eq!(probe["x"], 600.0);
    assert_eq!(probe["y"], 300.0);
    assert_eq!(probe["dx"], 25.0);
    assert_eq!(probe["dy"], 75.0);
    assert_eq!(probe["ctrl"], true);
    assert_eq!(probe["trusted"], true);
}

#[tokio::test(flavor = "current_thread")]
async fn hit_testing_clips_scrolled_children_at_overflow_padding_edge() {
    let (mut ctx, sid) = setup().await;
    let result = evaluate(
        &mut ctx,
        2,
        r#"(() => {
            const box = document.getElementById('box');
            box.scrollLeft = 50;
            const inner = document.getElementById('inner').getBoundingClientRect();
            return JSON.stringify({
                hit: document.elementFromPoint(25, 50).id,
                innerLeft: inner.left, innerRight: inner.right,
                boxLeft: box.getBoundingClientRect().left
            });
        })()"#,
        &sid,
    )
    .await;
    let result: Value = serde_json::from_str(result["result"]["value"].as_str().unwrap()).unwrap();
    assert!(result["innerLeft"].as_f64().unwrap() <= 25.0);
    assert!(result["innerRight"].as_f64().unwrap() >= 25.0);
    assert_eq!(result["boxLeft"], 20.0);
    assert_eq!(result["hit"], "box", "content hidden behind the border cannot win hit testing");
}

#[tokio::test(flavor = "current_thread")]
async fn press_release_orders_events_and_defers_click_activation() {
    let (mut ctx, sid) = setup().await;
    evaluate(
        &mut ctx,
        2,
        r#"(() => {
            const target = document.getElementById('check');
            document.elementFromPoint = () => target;
            globalThis.mouseLog = [];
            for (const type of ['mousedown', 'mouseup', 'click', 'input', 'change']) {
                target.addEventListener(type, event => mouseLog.push({
                    type, checked: target.checked, x: event.clientX,
                    ctrl: event.ctrlKey, shift: event.shiftKey, trusted: event.isTrusted
                }));
            }
        })()"#,
        &sid,
    )
    .await;

    cdp(
        &mut ctx,
        3,
        "Input.dispatchMouseEvent",
        json!({
            "type": "mousePressed", "x": 31.0, "y": 42.0,
            "button": "left", "clickCount": 1, "modifiers": 10
        }),
        &sid,
    )
    .await;
    let pressed = evaluate(
        &mut ctx,
        4,
        "JSON.stringify({log: mouseLog, checked: document.getElementById('check').checked})",
        &sid,
    )
    .await;
    let pressed: Value = serde_json::from_str(pressed["result"]["value"].as_str().unwrap()).unwrap();
    assert_eq!(pressed["checked"], false, "checkbox activation must wait for release");
    assert_eq!(pressed["log"][0]["type"], "mousedown");
    assert_eq!(pressed["log"].as_array().unwrap().len(), 1, "press must not synthesize click");

    cdp(
        &mut ctx,
        5,
        "Input.dispatchMouseEvent",
        json!({
            "type": "mouseReleased", "x": 31.0, "y": 42.0,
            "button": "left", "clickCount": 1, "modifiers": 10
        }),
        &sid,
    )
    .await;
    let released = evaluate(
        &mut ctx,
        6,
        "JSON.stringify({log: mouseLog, checked: document.getElementById('check').checked})",
        &sid,
    )
    .await;
    let released: Value = serde_json::from_str(released["result"]["value"].as_str().unwrap()).unwrap();
    let types: Vec<&str> = released["log"]
        .as_array()
        .unwrap()
        .iter()
        .map(|entry| entry["type"].as_str().unwrap())
        .collect();
    assert_eq!(types, ["mousedown", "mouseup", "click", "input", "change"]);
    assert_eq!(released["checked"], true);
    assert_eq!(released["log"][2]["checked"], true, "click sees checkbox pre-activation");
    assert_eq!(released["log"][2]["x"], 31.0);
    assert_eq!(released["log"][2]["ctrl"], true);
    assert_eq!(released["log"][2]["shift"], true);
    assert_eq!(released["log"][2]["trusted"], true);
}

#[tokio::test(flavor = "current_thread")]
async fn radio_release_selects_only_the_target_in_its_group() {
    let (mut ctx, sid) = setup().await;
    evaluate(
        &mut ctx,
        2,
        r#"(() => {
            const a = document.getElementById('radio-a');
            const b = document.getElementById('radio-b');
            document.elementFromPoint = () => b;
            globalThis.radioEvents = [];
            for (const radio of [a, b]) {
                for (const type of ['mousedown', 'mouseup', 'click', 'input', 'change']) {
                    radio.addEventListener(type, () => radioEvents.push(radio.id + ':' + type));
                }
            }
        })()"#,
        &sid,
    )
    .await;
    cdp(
        &mut ctx,
        3,
        "Input.dispatchMouseEvent",
        json!({"type": "mousePressed", "x": 10.0, "y": 10.0, "button": "left"}),
        &sid,
    )
    .await;
    cdp(
        &mut ctx,
        4,
        "Input.dispatchMouseEvent",
        json!({"type": "mouseReleased", "x": 10.0, "y": 10.0, "button": "left"}),
        &sid,
    )
    .await;
    let result = evaluate(
        &mut ctx,
        5,
        "JSON.stringify({a: document.getElementById('radio-a').checked, b: document.getElementById('radio-b').checked, events: radioEvents})",
        &sid,
    )
    .await;
    let result: Value = serde_json::from_str(result["result"]["value"].as_str().unwrap()).unwrap();
    assert_eq!(result["a"], false);
    assert_eq!(result["b"], true);
    assert_eq!(
        result["events"],
        json!(["radio-b:mousedown", "radio-b:mouseup", "radio-b:click", "radio-b:input", "radio-b:change"]),
        "the newly selected radio alone receives activation events"
    );
}
