export function unixTimestampNow(): number {
  return Math.floor(new Date().getTime() / 1000);
}

export function promptMultiLine(promptStr?: string): string {
  if (promptStr) console.log(promptStr);
  let s = "";
  let buf = "";
  while (true) {
    buf = prompt("")?.trim() ?? "";
    if (buf.length === 0) {
      s += "\n";
      break;
    }
    s += buf + "\n";
  }
  return s;
}
