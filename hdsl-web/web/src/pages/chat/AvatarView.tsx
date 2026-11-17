import { api } from "../../api";

export function avatarIsImage(value?: string | null): boolean {
  return typeof value === "string" && value.startsWith("img:");
}

export function avatarImageId(value?: string | null): string | null {
  return avatarIsImage(value) ? (value as string).slice(4) : null;
}

/** 头像：emoji 文本原样显示，`img:<id>` 显示成服务端重编码过的 PNG。 */
export function AvatarView({
  value, fallback = "💬", className,
}: {
  value?: string | null;
  fallback?: string;
  className?: string;
}) {
  const id = avatarImageId(value);
  if (id) {
    return (
      <img
        className={className ? `chat-avatar-img ${className}` : "chat-avatar-img"}
        src={api.chat.avatarUrl(id)}
        alt=""
        loading="lazy"
        draggable={false}
      />
    );
  }
  return <>{value || fallback}</>;
}