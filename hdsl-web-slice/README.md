# HDSL-web 源码切片

这是 [HDSL-web](https://github.com/nas200-timmy/HDSL-WEB) 的一小块只读源码切片，
用于 Break-This-Repo 的整活 PR。来源提交：`078fdd0948`。

## 包含

- `transcode/command/CommandBuilder.java`：ffmpeg argv 构建（编码器/质量/两遍/流控制）
- `transcode/preset/TranscodePreset.java`、`PresetSchema.java`：预设模型与参数面板 schema
- `web/src/pages/transcode/PreparePage.tsx`：准备文件/待转码列表前端
- `plain/PlainHtml.java`、`web-legacy/legacy.css`：无 JavaScript / IE11 表面
- `CommandBuilderTest.java`：命令构建的单元测试

## 不包含

构建产物、`node_modules`、数据目录、账号、证书、密钥、图片/视频素材。
本切片不是可独立构建的完整项目，依赖 HDSL-web 的其它模块。

## License

GPL-3.0，见 `LICENSE`。
