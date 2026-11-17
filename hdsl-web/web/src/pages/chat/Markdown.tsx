import { memo, useState, type MouseEvent, type ReactNode } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import rehypeSanitize from "rehype-sanitize";
import rehypeHighlight from "rehype-highlight";
import rehypeKatex from "rehype-katex";
import remarkMath from "remark-math";
import { ContentCopyIcon, DownloadIcon } from "../../components/icons";
import type { ChatSearchHit } from "../../types";
import { hastText, languageOf, prepareText, sanitizeSchema } from "./markdown-schema";
import { toast } from "../../store";

function stripAnsi(text: string): string {
  return text.replace(/\u001b\[[0-9;]*m/g, "");
}

/** 代码块：语言标签 + 复制 + 下载 + 换行。高亮由 rehype-highlight 注入 class。 */
function CodeBlock({ node, children }: { node?: any; children?: ReactNode }) {
  const [copied, setCopied] = useState(false);
  const [wrap, setWrap] = useState(false);
  const [expanded, setExpanded] = useState(false);
  const raw = hastText(node).replace(/\n$/, "");
  const language = languageOf(node);
  const long = raw.split("\n").length > 32;

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(stripAnsi(raw));
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1200);
    } catch {
      // Clipboard permission refused; the user can still select the code.
    }
  };

  const download = () => {
    const blob = new Blob([stripAnsi(raw)], { type: "text/plain;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `snippet${language ? "." + language : ".txt"}`;
    link.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="chat-code">
      <div className="chat-code-head">
        <span className="chat-code-lang">{language || "text"}</span>
        <span className="spacer" />
        <button className="chat-code-btn" onClick={() => setWrap((value) => !value)}>
          {wrap ? "不换行" : "换行"}
        </button>
        <button className="chat-code-btn" onClick={download} title="下载代码">
          <DownloadIcon size={14} />
        </button>
        <button className="chat-code-btn" onClick={() => void copy()} title="复制代码">
          <ContentCopyIcon size={14} />
          {copied ? "已复制" : "复制"}
        </button>
      </div>
      <pre className={`chat-code-body${wrap ? " wrap" : ""}${long && !expanded ? " collapsed" : ""}`}>
        {children}
      </pre>
      {long && (
        <button className="chat-code-more" onClick={() => setExpanded((value) => !value)}>
          {expanded ? "收起" : `展开全部（${raw.split("\n").length} 行）`}
        </button>
      )}
    </div>
  );
}

/** GFM + sanitize + highlight。不启用 raw HTML。 */
function MarkdownImpl({ content, streaming, citations, sources }: {
  content: string;
  streaming?: boolean;
  /** 引用短 id → 原页面 URL；模型写 [citation,domain](id) 时用它换成真链接。 */
  citations?: Record<string, string>;
  /** DeepSeek 官方引用：正文里的 [citation:N] 按来源顺序换成链接。 */
  sources?: ChatSearchHit[];
}) {
  const text = prepareText(content ?? "", { streaming, citations, sources });
  // Clicking a formula copies its TeX source: KaTeX renders MathML with an
  // `application/x-tex` annotation, which selection/copy alone does not expose.
  const onMathClick = (event: MouseEvent<HTMLDivElement>) => {
    const formula = (event.target as HTMLElement).closest(".katex");
    if (!formula) return;
    const annotation = formula.querySelector('annotation[encoding="application/x-tex"]');
    const tex = annotation?.textContent?.trim();
    if (!tex) return;
    void navigator.clipboard.writeText(tex).then(
      () => toast("success", "已复制公式 TeX"),
      () => undefined,
    );
  };
  return (
    <div className="chat-markdown" onClick={onMathClick}>
      <ReactMarkdown
        remarkPlugins={[remarkGfm, remarkMath]}
        rehypePlugins={[[rehypeSanitize, sanitizeSchema] as any, [rehypeKatex, { throwOnError: false }] as any, rehypeHighlight]}
        components={{
          pre: ({ node, children }) => <CodeBlock node={node}>{children}</CodeBlock>,
          a: ({ href, children }) => (
            <a href={href} target="_blank" rel="noopener noreferrer">
              {children}
            </a>
          ),
        }}
      >
        {text}
      </ReactMarkdown>
    </div>
  );
}

export const Markdown = memo(MarkdownImpl);