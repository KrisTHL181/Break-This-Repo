import { useEffect, useRef, useState } from "react";
import { Dialog } from "../../components/Dialog";
import { api } from "../../api";
import { toast } from "../../store";
import { errMsg } from "../../utils";

const PREVIEW = 240;
const OUTPUT = 256;
const MAX_ZOOM = 4;

interface Layout {
  scale: number;
  left: number;
  top: number;
}

function layoutOf(image: HTMLImageElement, zoom: number, offset: { x: number; y: number }): Layout {
  // 先按 cover 铺满预览框，再乘用户缩放；offset 是拖动量（预览像素）
  const cover = Math.max(PREVIEW / image.width, PREVIEW / image.height);
  const scale = cover * zoom;
  return {
    scale,
    left: (PREVIEW - image.width * scale) / 2 + offset.x,
    top: (PREVIEW - image.height * scale) / 2 + offset.y,
  };
}

function clampOffset(image: HTMLImageElement, zoom: number, offset: { x: number; y: number }) {
  const { scale } = layoutOf(image, zoom, { x: 0, y: 0 });
  const maxX = Math.max(0, (image.width * scale - PREVIEW) / 2);
  const maxY = Math.max(0, (image.height * scale - PREVIEW) / 2);
  return {
    x: Math.min(maxX, Math.max(-maxX, offset.x)),
    y: Math.min(maxY, Math.max(-maxY, offset.y)),
  };
}

/** 选图 → 拖动/缩放 → 圆形预览 → 导出 256×256 PNG 上传。 */
export function AvatarCropDialog({
  open, onClose, onUploaded,
}: {
  open: boolean;
  onClose: () => void;
  onUploaded: (id: string) => void;
}) {
  const fileRef = useRef<HTMLInputElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const [image, setImage] = useState<HTMLImageElement | null>(null);
  const [zoom, setZoom] = useState(1);
  const [offset, setOffset] = useState({ x: 0, y: 0 });
  const [busy, setBusy] = useState(false);
  const drag = useRef<{ x: number; y: number; ox: number; oy: number } | null>(null);

  useEffect(() => {
    if (!open) {
      setImage(null);
      setZoom(1);
      setOffset({ x: 0, y: 0 });
      setBusy(false);
    }
  }, [open]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas || !image) return;
    const context = canvas.getContext("2d");
    if (!context) return;
    const { scale, left, top } = layoutOf(image, zoom, offset);
    context.clearRect(0, 0, PREVIEW, PREVIEW);
    context.save();
    context.beginPath();
    context.arc(PREVIEW / 2, PREVIEW / 2, PREVIEW / 2 - 1, 0, Math.PI * 2);
    context.clip();
    context.drawImage(image, left, top, image.width * scale, image.height * scale);
    context.restore();
    context.beginPath();
    context.arc(PREVIEW / 2, PREVIEW / 2, PREVIEW / 2 - 1, 0, Math.PI * 2);
    context.strokeStyle = "rgba(0,0,0,0.28)";
    context.lineWidth = 2;
    context.stroke();
  }, [image, zoom, offset]);

  const pick = (file: File | null | undefined) => {
    if (!file) return;
    if (!/^image\/(png|jpeg|webp)$/.test(file.type)) {
      toast("error", "只支持 PNG / JPEG / WebP 静态图片");
      return;
    }
    if (file.size > 8 * 1024 * 1024) {
      toast("error", "图片不能超过 8 MB");
      return;
    }
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      setImage(img);
      setZoom(1);
      setOffset({ x: 0, y: 0 });
      URL.revokeObjectURL(url);
    };
    img.onerror = () => {
      toast("error", "这张图片打不开，换一张试试");
      URL.revokeObjectURL(url);
    };
    img.src = url;
  };

  const confirm = async () => {
    if (!image) return;
    setBusy(true);
    try {
      const canvas = document.createElement("canvas");
      canvas.width = OUTPUT;
      canvas.height = OUTPUT;
      const context = canvas.getContext("2d");
      if (!context) throw new Error("当前浏览器不支持 canvas");
      const { scale, left, top } = layoutOf(image, zoom, offset);
      const size = PREVIEW / scale;
      context.drawImage(image, -left / scale, -top / scale, size, size, 0, 0, OUTPUT, OUTPUT);
      const blob = await new Promise<Blob | null>((resolve) =>
        canvas.toBlob(resolve, "image/png"));
      if (!blob) throw new Error("导出 PNG 失败");
      const uploaded = await api.chat.uploadAvatar(new File([blob], "avatar.png", { type: "image/png" }));
      onUploaded(uploaded.id);
      onClose();
    } catch (error) {
      toast("error", `头像上传失败：${errMsg(error)}`);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog open={open} title="设置头像" onClose={busy ? undefined : onClose}>
      <div className="avatar-crop">
        <input
          ref={fileRef}
          type="file"
          hidden
          accept="image/png,image/jpeg,image/webp"
          onChange={(event) => {
            pick(event.target.files?.[0]);
            event.target.value = "";
          }}
        />
        {image ? (
          <>
            <canvas
              ref={canvasRef}
              className="avatar-crop-canvas"
              width={PREVIEW}
              height={PREVIEW}
              onPointerDown={(event) => {
                event.currentTarget.setPointerCapture(event.pointerId);
                drag.current = { x: event.clientX, y: event.clientY, ox: offset.x, oy: offset.y };
              }}
              onPointerMove={(event) => {
                const start = drag.current;
                if (!start) return;
                setOffset(clampOffset(image, zoom, {
                  x: start.ox + (event.clientX - start.x),
                  y: start.oy + (event.clientY - start.y),
                }));
              }}
              onPointerUp={() => { drag.current = null; }}
              onPointerCancel={() => { drag.current = null; }}
            />
            <label className="avatar-crop-zoom">
              <span>缩放</span>
              <input
                type="range"
                min={1}
                max={MAX_ZOOM}
                step={0.01}
                value={zoom}
                onChange={(event) => {
                  const next = Number(event.target.value);
                  setZoom(next);
                  setOffset((current) => clampOffset(image, next, current));
                }}
              />
            </label>
            <button className="btn btn-text dim" onClick={() => fileRef.current?.click()}>
              换一张
            </button>
          </>
        ) : (
          <button className="avatar-crop-empty" onClick={() => fileRef.current?.click()}>
            <span>选择图片</span>
            <span className="hint">PNG / JPEG / WebP，静态图片；可以拖动和缩放</span>
          </button>
        )}
      </div>
      <div className="hint">
        圆形区域就是头像的最终效果。服务端只会保存重新编码过的 512×512 PNG，
        原图的 EXIF、动画和附加数据都不会保留。
      </div>
      <div className="dialog-actions">
        <button className="btn btn-text dim" disabled={busy} onClick={onClose}>取消</button>
        <button className="btn btn-raised" disabled={busy || !image} onClick={() => void confirm()}>
          {busy ? "上传中…" : "使用这张"}
        </button>
      </div>
    </Dialog>
  );
}