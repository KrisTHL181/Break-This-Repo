![LunarCore](https://socialify.git.ci/Melledy/LunarCore/image?description=1&descriptionEditable=A%20game%20server%20reimplementation%20for%20version%202.1.0%20of%20a%20certain%20turn-based%20anime%20game%20for%20educational%20purposes.%20&font=Inter&forks=1&issues=1&language=1&name=1&owner=1&pulls=1&stargazers=1&theme=Light)
<div align="center"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/Melledy/LunarCore?logo=java&style=for-the-badge"> <img alt="GitHub" src="https://img.shields.io/github/license/Melledy/LunarCore?style=for-the-badge"> <img alt="GitHub last commit" src="https://img.shields.io/github/last-commit/Melledy/LunarCore?style=for-the-badge"> <img alt="GitHub Workflow Status" src="https://img.shields.io/github/actions/workflow/status/Melledy/LunarCore/build.yml?branch=development&logo=github&style=for-the-badge"></div>

<div align="center"><a href="https://discord.gg/cfPKJ6N5hw"><img alt="Discord - Grasscutter" src="https://img.shields.io/discord/1163718404067303444?label=Discord&logo=discord&style=for-the-badge"></a></div>

[EN](../README.md) | [简中](README_zh-CN.md) | [繁中](README_zh-TW.md) | [JP](README_ja-JP.md) | [RU](README_ru-RU.md) | [FR](README_fr-FR.md) | [KR](README_ko-KR.md) | [VI](README_vi-VI.md)


**Lưu ý:** Để biết thêm yêu cầu bổ sung, câu hỏi, hoặc thảo luận, vui lòng xem trên Discord của chúng tôi [Discord](https://discord.gg/cfPKJ6N5hw).

### Các tính năng có sẵn
- Các tính năng cơ bản: Đăng nhập, Đội Hình, Túi, Cảnh cơ bản/Quản lý thực thể
- Chiến đấu với quái hoạt động
- Quái vật trong cảnh/Đạo cụ/Spawn NPC
- Bí kĩ nhân vật
- Ghép/Vật phẩm tiêu hao 
- Shop của NPC 
- Hệ thống Gacha
- Hệ thống Mail
- Hệ thống bạn bè (Chi viện chưa hoạt động `Ít nhất với hiện tại không còn bị infinite load nếu bạn lỡ tay nhấn phải`)
- Sảnh đường lãng quên
- Kể chuyện hư cấu
- Vũ trụ mô phổng (Có thể hoạt động được, nhưng một số tính năng vẫn có thể đang thiếu)

# Chạy server và client

### Yêu cầu
* [Java 17 JDK](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)

### Khuyến khích sử dụng
* [MongoDB 4.0+](https://www.mongodb.com/try/download/community)

### Compiling Server
1. Mở Terminal và compilie server bằng lệnh `./gradlew jar`
2. Tạo folder `resources` trong folder server
3. Download `Config`, `TextMap`, và `ExcelBin` folder từ [https://github.com/Dimbreath/StarRailData](https://github.com/Dimbreath/StarRailData) và đặt vào trong thư mục `resources` của bạn.
4. Xóa thư mục theo đường dẫn `/resources/Config/LevelOutput`
5. Tải xuống folder `Config` từ [https://gitlab.com/Melledy/LunarCore-Configs](https://gitlab.com/Melledy/LunarCore-Configs) và đặt vào thư mục của bạn
6. Chạy server với lệnh `java -jar LunarCore.jar` từ Terminal hệ thống của bạn. Lunar Core đi kèm với một máy chủ MongoDB nội bộ cho cơ sở dữ liệu của nó, vì vậy không cần cài đặt MongoDB. Tuy nhiên, rất khuyến khích cài đặt MongoDB.
### Kết nối đến server (Phương án sử dụng Fiddler)
1. **Đăng nhập vào trò chơi tới máy chủ chính thức và tài khoản Hoyoverse một lần trước để tải xuống dữ liệu game.**
2. Cài đặt và chạy [Fiddler Classic](https://www.telerik.com/fiddler).
3. Cài đặt Fiddler để giải mã tín hiệu https. (Tools -> Options -> HTTPS -> Decrypt HTTPS traffic) Đảm bảo `ignore server certificate errors` đã được đánh dấu.
4. Copy và paste đoạn code vào Fiddlerscript tab của Fiddler Classic :

```
import System;
import System.Windows.Forms;
import Fiddler;
import System.Text.RegularExpressions;

class Handlers
{
    static function OnBeforeRequest(oS: Session) {
        if (oS.host.EndsWith(".starrails.com") || oS.host.EndsWith(".hoyoverse.com") || oS.host.EndsWith(".mihoyo.com") || oS.host.EndsWith(".bhsr.com")) {
            oS.host = "localhost"; // This can also be replaced with another IP address.
        }
    }
};
```

5. Nếu `autoCreateAccount` được set True ở config thì có thể bỏ qua bước này. Hoặc là, nhập `/account create [account name]` vào console của server để tạo tài khoản
6. Đăng nhập vào tài khoản của bạn, mật khẩu không được sử dụng ở server nên có thể nhập bất cứ thứ gì.

### Lệnh server
Lệnh server có thể chạy ở console của server hoặc sử dụng trong game. Sẽ có nhân vật tên "Server" trong mỗi danh sách bạn bè để bạn có thể sử dụng lệnh trong game.

```
/account {create | delete} [username] (reserved player uid). Tạo hoặc xóa tài khoản
/avatar {cur | all | lineup} lv(level) p(ascension) r(eidolon) s(skill levels). Điều chỉnh đặc tính nhân vật.
/clear {relics | lightcones | materials | items}. Xóa đồ khỏi túi.
/gender {male | female}. Điều chỉnh giới tính nhân vật. (Mặc định là Nam)
/give [item id] x[amount] lv[number]. Cho nhân vật chỉ định vật phẩm.
/giveall {materials | avatars | lightcones | relics}. Cho nhân vật chỉ định tất cả vật phẩm.
/heal. Hồi phục nhân vật.
/help. Hiện danh sách lệnh.
/kick @[player id]. Kick một ai đó khỏi server.
/mail [content]. Gửi đến nhân vật chỉ định mail hệ thống.
/permission {add | remove | clear} [permission]. Cho hoặc xóa quyền của một người.
/refill. Sạc lại điểm bí kĩ.
/reload. Load lại config server.
/setlevel [level] - Điều chỉnh cấp độ của nhà khai phá chỉ định.
/scene [scene id] [floor id]. Dịch chuyển một người đến mọt cảnh nhất định.
/spawn [monster/prop id] x[amount] s[stage id]. Spawn quái vật ở gần người chơi chỉ định.
/stop. Dừng server
/unstuck @[player id]. Unstucks người chơi offline khi cảnh của họ không được load.
/worldlevel [world level]. Điều chỉnh cấp cân bằng.
```
