import { useRef, useState } from "react";

/** 文件拖拽/选择框（HMCL 样式：虚线描边、hover 高亮）。 */
export function FileDropInput({
  file,
  onFile,
  accept,
  hint,
  disabled,
}: {
  file: File | null;
  onFile: (f: File | null) => void;
  accept?: string;
  hint: string;
  disabled?: boolean;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);

  return (
    <>
      <input
        ref={inputRef}
        type="file"
        accept={accept}
        style={{ display: "none" }}
        onChange={(e) => {
          onFile(e.target.files?.[0] ?? null);
          e.target.value = "";
        }}
      />
      <button
        type="button"
        className={`filedrop${dragging ? " dragging" : ""}`}
        disabled={disabled}
        onClick={() => inputRef.current?.click()}
        onDragOver={(e) => {
          e.preventDefault();
          setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={(e) => {
          e.preventDefault();
          setDragging(false);
          onFile(e.dataTransfer.files?.[0] ?? null);
        }}
      >
        {file ? <span className="filedrop-name">{file.name}</span> : hint}
      </button>
    </>
  );
}
