# PhotosReview

[中文](./README-zh.md) | [English](./README.md)

PhotosReview 是一个基于 **Spring Boot + SQLite** 的照片评审系统，提供：

- 用户登录与令牌校验。
- 后台项目管理（创建项目、分配任务、管理用户、上传与删除照片）。
- 前台项目浏览与评审模式（Project / History）。
- 中英双语切换与深浅色外观切换。

## 仓库结构

- `src/main/java/com/hodastar/photosreview/`：后端 Java 代码（Controller / Mapper / Config / Utils / Entity）。
- `src/main/resources/static/`：前端静态页面与脚本（`index.html`、`admin.html`、`proj.html` 等）。
- `src/main/resources/static/i18n/`：国际化文案（`zh.json`、`en.json`）。
- `data/`：运行时数据目录（如默认图标、项目资源）。

## 环境要求

- JDK 17+
- Maven 3.9+（或使用仓库自带 `mvnw`）

## 本地启动

```bash
# 1) 克隆并进入项目
git clone <your-repo-url>
cd PhotosReview

# 2) 启动
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

启动后默认访问：

- 前台：`http://localhost:8080/`
- 后台：`http://localhost:8080/admin.html`

## 打包

```bash
./mvnw clean package
java -jar target/*.jar
```

## 常见说明

- 前端语言切换依赖 `localStorage.lang`，文案来源于 `static/i18n/*.json`。
- 外观切换依赖 `localStorage.darkmode`。
- 首次运行时会根据后端初始化逻辑创建所需数据表与基础数据（若不存在）。

## 贡献

欢迎通过 Issue / Pull Request 提交问题与改进建议。
