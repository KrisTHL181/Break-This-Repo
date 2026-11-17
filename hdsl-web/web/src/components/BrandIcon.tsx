import { PublicIcon } from "./icons";
import type { BrandKey } from "./BrandsView";

/**
 * 品牌图标：用各家**上游仓库自带的**标志文件，来源、许可与商标声明见仓库根 `NOTICE`
 * 的 TRADEMARKS 一节：
 * - dsh 自己用 DeepSeek 的鲸鱼标（Simple Icons 的 CC0 文件，标志取自 deepseek.com）；
 * - Kimi Code 与 OpenCode 取自其 MIT 仓库的原文件；
 * - ZCode 取 Simple Icons 的 CC0 文件——ZCode 仓库自己的 `NOTICE` 对「图标」有笼统的
 *   保留条款，故改用许可明确的那份。
 *
 * - 单色标志走 CSS mask + `currentColor`：跟着主题明暗走，不必为深色主题备第二份文件。
 * - 彩色标志直接 `<img>`（OpenCode 的 mark 自带底色与描边，浅深主题都看得见）。
 * - **没有可用标志的品牌回退中性图标**：Claude Web 的社区仓库图标用的就是 Anthropic 的
 *   珊瑚色星芒标，而 Anthropic 的商标条款写明「须事先书面许可、不得改动」，故不上图，
 *   只保留文字标签。
 * - **Octop（腾讯云）同理**：上游仓库是 MIT，但 MIT 只覆盖代码，不含商标；章鱼标是腾讯的
 *   品牌标识，我们对它只做「指代这个产品」的描述性使用，不把它的标志当成本项目的图标。
 *   所以这里不给 octop 配 logo，走中性图标。要换成上游文件，得有明确的商标授权。
 *
 * 文件路径与 `assets-img/…` 同款（相对路径交给 `<base href>` 解析，子路径挂载同样成立）。
 */
type BrandLogoKey = BrandKey | "dsh";

const LOGOS: Partial<Record<BrandLogoKey, { file: string; monochrome: boolean }>> = {
  dsh: { file: "brand-dsh.svg", monochrome: true },
  zcode: { file: "brand-zcode.svg", monochrome: true },
  kimi: { file: "brand-kimi.svg", monochrome: true },
  opencode: { file: "brand-opencode.svg", monochrome: false },
};

export function BrandIcon({ brand, size = 20 }: { brand: BrandLogoKey; size?: number }) {
  const logo = LOGOS[brand];
  if (!logo) return <PublicIcon size={size} />;
  const url = `assets-img/${logo.file}`;
  if (!logo.monochrome) {
    return (
      <img
        src={url}
        alt=""
        width={size}
        height={size}
        style={{ display: "block", borderRadius: 3, flex: "none" }}
      />
    );
  }
  return (
    <span
      aria-hidden="true"
      style={{
        display: "block",
        flex: "none",
        width: size,
        height: size,
        background: "currentColor",
        maskImage: `url(${url})`,
        maskSize: "contain",
        maskRepeat: "no-repeat",
        maskPosition: "center",
        WebkitMaskImage: `url(${url})`,
        WebkitMaskSize: "contain",
        WebkitMaskRepeat: "no-repeat",
        WebkitMaskPosition: "center",
      }}
    />
  );
}