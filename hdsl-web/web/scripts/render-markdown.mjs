// Server-side markdown renderer for the legacy no-JS surface.
//
// Bundled at build time (`npm run build:ssr`) into a single ESM file that the
// panel unpacks from its jar and runs with its own Node. It renders with the
// very same pipeline as the SPA (web/src/pages/chat/Markdown.tsx): react-markdown
// + remark-gfm + rehype-sanitize (shared schema) + rehype-highlight, so a
// transcript read in w3m or IE11 carries exactly the markup the panel shows in
// a modern browser. The one difference is deliberate: code blocks render their
// language tag but none of the interactive buttons (copy / download / wrap),
// which mean nothing without JavaScript.
//
// stdin:  the stored message text (UTF-8)
// stdout: the rendered HTML fragment, no document wrapper
import React from "react";
import { renderToStaticMarkup } from "react-dom/server";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import rehypeSanitize from "rehype-sanitize";
import rehypeHighlight from "rehype-highlight";
import rehypeKatex from "rehype-katex";
import remarkMath from "remark-math";
import { hastText, languageOf, prepareText, sanitizeSchema } from "../src/pages/chat/markdown-schema";

function CodeBlock({ node, children }) {
  const raw = hastText(node).replace(/\n$/, "");
  const language = languageOf(node);
  const long = raw.split("\n").length > 32;
  return React.createElement(
    "div",
    { className: "chat-code" },
    React.createElement(
      "div",
      { className: "chat-code-head" },
      React.createElement("span", { className: "chat-code-lang" }, language || "text"),
      long
        ? React.createElement(
            "span",
            { className: "chat-code-lines" },
            `${raw.split("\n").length} 行`,
          )
        : null,
    ),
    React.createElement("pre", { className: "chat-code-body" }, children),
  );
}

function readStdin() {
  return new Promise((resolve, reject) => {
    let text = "";
    process.stdin.setEncoding("utf8");
    process.stdin.on("data", (chunk) => {
      text += chunk;
    });
    process.stdin.on("end", () => resolve(text));
    process.stdin.on("error", reject);
  });
}

// The bundle is CommonJS (react-dom/server is CJS and requires node builtins),
// so top-level await is not available; chain instead.
readStdin()
  .then((raw) => {
    // The panel sends {content, sources?}; sources are what turns [citation:N]
    // markers into links, exactly as the SPA receives message.sources.
    let content = raw;
    let sources;
    try {
      const parsed = JSON.parse(raw);
      if (parsed && typeof parsed.content === "string") {
        content = parsed.content;
        if (Array.isArray(parsed.sources)) sources = parsed.sources;
      }
    } catch {
      // Not an envelope (a bare markdown body): render it as-is.
    }
    const html = renderToStaticMarkup(
      React.createElement(
        "div",
        { className: "chat-markdown legacy-md" },
        React.createElement(
          ReactMarkdown,
          {
            remarkPlugins: [remarkGfm, remarkMath],
            rehypePlugins: [[rehypeSanitize, sanitizeSchema], [rehypeKatex, { throwOnError: false }], rehypeHighlight],
            components: {
              pre: ({ node, children }) => React.createElement(CodeBlock, { node }, children),
              a: ({ href, children }) =>
                React.createElement(
                  "a",
                  { href, target: "_blank", rel: "noopener noreferrer" },
                  children,
                ),
            },
          },
          prepareText(content, { sources }),
        ),
      ),
    );
    process.stdout.write(html);
  })
  .catch((error) => {
    process.stderr.write(String(error && error.stack ? error.stack : error));
    process.exit(1);
  });
