import { defaultSchema } from "rehype-sanitize";
import type { ChatSearchHit } from "../../types";

/// The sanitize schema and the citation text transforms behind the chat's
/// markdown, shared by the SPA renderer (`Markdown.tsx`) and the server-side
/// renderer (`scripts/render-markdown.mjs`) so the two surfaces cannot drift:
/// the legacy no-JS surface serves exactly the markup the panel renders here.
///
/// GFM + sanitize + highlight, raw HTML never enabled.
export const sanitizeSchema = {
  ...defaultSchema,
  attributes: {
    ...defaultSchema.attributes,
    span: [...(defaultSchema.attributes?.span ?? []), "className"],
    code: [...(defaultSchema.attributes?.code ?? []), "className"],
  },
  protocols: {
    ...defaultSchema.protocols,
    href: ["http", "https", "mailto"],
  },
};

/// Turns stored message text into the markdown the renderers consume.
///
/// - streaming: an unclosed fence is closed temporarily so a half-written code
///   block does not swallow the rest of the message as one paragraph;
/// - sources: DeepSeek's `[citation:N]` becomes a link by source order;
/// - citations: `[citation,domain](id)` becomes a link by the hit's short id.
///   rehype-sanitize only allows http/https/mailto, so the short id has to be
///   resolved to the real URL before rendering, not after.
export function prepareText(
  content: string,
  options?: {
    streaming?: boolean;
    citations?: Record<string, string>;
    sources?: ChatSearchHit[];
  },
): string {
  let text = content ?? "";
  if (options?.streaming) {
    const fences = (text.match(/^```/gm) ?? []).length;
    if (fences % 2 === 1) text += "\n```";
  }
  const sources = options?.sources;
  if (sources && sources.length > 0) {
    text = text.replace(/\[citation:(\d+)\]/g, (_, raw) => {
      const index = Number(raw) - 1;
      const source = sources[index];
      if (!source) return `[${raw}]`;
      return `[${raw}](${source.url})`;
    });
  }
  const citations = options?.citations;
  if (citations && Object.keys(citations).length > 0) {
    // The model does not always copy the short id verbatim: it also writes the
    // index (1, 2, …), pastes the URL straight into the parentheses, or glues
    // two ids together ("8e788c→9e788c"). All three are covered here.
    text = text.replace(/\[citation,([^\]]+)\]\(([^)\s]+)\)/g, (_, domain, target) => {
      if (/^https?:\/\//i.test(target)) {
        return `[${domain}](${target})`;
      }
      const direct = citations[target];
      if (direct) {
        return `[${domain}](${direct})`;
      }
      const hex = target.match(/[0-9a-f]{6}/i);
      const url = hex ? citations[hex[0].toLowerCase()] : undefined;
      return url ? `[${domain}](${url})` : `[${domain}]`;
    });
  }
  return text;
}

/// Plain text of a hast node; after rehype-highlight a code block's children
/// are span arrays, so `children` alone is not enough.
export function hastText(node: any): string {
  if (!node) return "";
  if (typeof node.value === "string") return node.value;
  if (Array.isArray(node.children)) return node.children.map(hastText).join("");
  return "";
}

/// The fenced block's language tag, from the class rehype-highlight injects.
export function languageOf(node: any): string {
  const codeNode = node?.children?.find((child: any) => child.tagName === "code") ?? node;
  const classes = Array.isArray(codeNode?.properties?.className)
    ? codeNode.properties.className.join(" ")
    : typeof codeNode?.properties?.className === "string"
      ? codeNode.properties.className
      : "";
  return /language-([\w+-]+)/.exec(classes)?.[1] ?? "";
}
