import { useEffect, useState } from "react";

/** 浏览器网络断开提示条。检测的是浏览器自身的网络状态（网线断、Wi-Fi 掉），
 *  不等于面板服务器可达性——后者由各请求的报错与 WS 重连状态承担。 */
export function OfflineBanner() {
  const [offline, setOffline] = useState(!navigator.onLine);

  useEffect(() => {
    const goOffline = () => setOffline(true);
    const goOnline = () => setOffline(false);
    window.addEventListener("offline", goOffline);
    window.addEventListener("online", goOnline);
    return () => {
      window.removeEventListener("offline", goOffline);
      window.removeEventListener("online", goOnline);
    };
  }, []);

  if (!offline) {
    return null;
  }
  return <div className="offline-banner">网络连接已断开，正在等待恢复…</div>;
}
