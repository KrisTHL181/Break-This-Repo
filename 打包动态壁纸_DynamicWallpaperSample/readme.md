# 动态壁纸包（样例项目）
*Dynamic Wallpaper Sample by copperate*
### 项目简介
用Kotlin写的一个动态壁纸样例，可用于制作自己的**独立打包**的视频动态壁纸。

### 如何处理
1. 准备好一个想用作动态壁纸的mp4格式的视频
2. 下载本项目
3. 将准备好的视频替换 app\src\main\ **assets** 下的 **livewallapaper.mp4** 文件
4. 更改 app\src\main\ **res\values** 下 的 **strings.xml** 文件
  >\<!-- 填写动态壁纸的名称 -->
  >\<string name="app_name">一个超棒的动态壁纸</string>
  >
  >\<!-- 填写动态壁纸的描述 -->
  >\<string name="livewallpaper_description">一个极其美妙的动态壁纸的描述</string>
  >

5. （可选）替换该动态壁纸的预览图，图片位于 app\src\main\ **res\drawable** 下的wallpaper_preview.png 文件

6. 使用你喜欢的打包工具进行打包。如果你对打包工具一无所知，请下载Android Studio，打开项目 - 选择此文件夹，等待导入完成后，使用USB数据线连接你的手机，之后点击右上角的绿色的播放按钮
7. 完成 