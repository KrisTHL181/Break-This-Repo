// Test stub for the experimental ZCode category: speaks the one stdout
// contract the launcher relies on — a `ZCode Web is running` line followed by
// `Local:   http://127.0.0.1:<port>/` — and then really serves HTTP on that
// port, so the `/i/<id>/` proxy has something to forward to.
//
// The port comes from the `--port` argument; 0 or absent asks the OS for a free
// one, and the readiness line reports what it got (exactly like upstream). The
// full argv and the ZCODE_DATA_BASE_DIR the launcher injected are written to the
// file FAKE_ZCODE_ARGS_FILE names, which is how the tests assert the
// --workspace/--port/--no-token pass-through.
import fs from "node:fs";
import http from "node:http";

const args = process.argv.slice(2);

const portIndex = args.indexOf("--port");
let wanted = portIndex >= 0 ? parseInt(args[portIndex + 1], 10) : 0;
if (!Number.isInteger(wanted) || wanted < 0) {
  wanted = 0;
}

const server = http.createServer((req, res) => {
  res.writeHead(200, { "Content-Type": "text/html; charset=utf-8" });
  res.end(
    "<!doctype html><html><head><title>fake zcode</title></head><body>" +
      "<h1>fake zcode</h1><p id=\"path\">" + req.url + "</p></body></html>",
  );
});

server.listen(wanted, "127.0.0.1", () => {
  const port = server.address().port;
  console.log("ZCode Web is running");
  console.log("Local:   http://127.0.0.1:" + port + "/");

  const argsFile = process.env.FAKE_ZCODE_ARGS_FILE;
  if (argsFile) {
    fs.writeFileSync(
      argsFile,
      JSON.stringify({
        argv: args,
        dataBaseDir: process.env.ZCODE_DATA_BASE_DIR || null,
        home: process.env.HOME || null,
      }) + "\n",
    );
  }

  let tick = 0;
  setInterval(() => {
    tick += 1;
    console.log("tick " + tick);
  }, 1000);
});

process.on("SIGTERM", () => process.exit(0));
process.on("SIGINT", () => process.exit(0));
