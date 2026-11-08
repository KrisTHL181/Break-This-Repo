package com.hodastar.photosreview.utils;

import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class ImageUtils {
    private static int convertToWebpTaskCount = 0;
    private static int convertToThumbnailTaskCount = 0;

    public static void createThumbnail(String sourceStr, String targetStr, String filename, int width, int height) throws Exception {
        if (convertToThumbnailTaskCount >= 3) {
            // 限制同时进行的转换任务数量，避免过多占用资源
            throw new RuntimeException("当前转换任务过多，请稍后再试");
        }
        convertToThumbnailTaskCount += 1;

        // 读取
        File sourceD = new File(sourceStr);
        File targetD = new File(targetStr);
        if (!sourceD.exists()) {
            throw new IOException("源文件不存在: " + sourceD.getAbsolutePath());
        }
        if (!targetD.exists()) {
            targetD.mkdirs();
        }
        File source = new File(sourceD, filename);
        File target = new File(targetD, filename);
        BufferedImage srcImg = ImageIO.read(source);

        // 按比例缩放
        int srcWidth = srcImg.getWidth();
        int srcHeight = srcImg.getHeight();

        double scale = Math.min(
                (double) width / srcWidth,
                (double) height / srcHeight
        );

        int newW = (int) (srcWidth * scale);
        int newH = (int) (srcHeight * scale);

        Image scaledImg = srcImg.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);

        BufferedImage output = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = output.createGraphics();

        // 抗锯齿
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(scaledImg, 0, 0, null);
        g.dispose();

        // 写入文件
        try {
            ImageIO.write(output, "jpg", target);
        } finally {
            convertToThumbnailTaskCount -= 1;
        }
    }

    public static void convertToWebp(String sourceStr,
                                     String targetStr,
                                     String filename) throws Exception {
        if (convertToWebpTaskCount >= 3) {
            // 限制同时进行的转换任务数量，避免过多占用资源
            throw new Exception("当前转换任务过多，请稍后再试");
        }
        convertToWebpTaskCount += 1;

        // 原文件
        File sourceFile = new File(sourceStr, filename);

        if (!sourceFile.exists()) {
            convertToWebpTaskCount -= 1;
            throw new Exception("源文件不存在: " + sourceFile.getAbsolutePath());
        }

        // 创建目标目录
        File targetDir = new File(targetStr);

        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        // 输出 webp 文件
        File targetFile = new File(targetDir, filename + ".webp");

        // 读取原图
        BufferedImage image = ImageIO.read(sourceFile);

        if (image == null) {
            convertToWebpTaskCount -= 1;
            throw new Exception("无法读取图片文件");
        }

        ImageWriter writer = null;

        try {
            // 获取 WebP Writer
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("webp");

            if (!writers.hasNext()) {
                convertToWebpTaskCount -= 1;
                throw new Exception("未找到 WebP Writer，请检查 imageio-webp 依赖");
            }

            writer = writers.next();

            // 压缩参数
            ImageWriteParam param = writer.getDefaultWriteParam();

            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

                String[] types = param.getCompressionTypes();
                if (types != null && types.length > 0) {
                    param.setCompressionType(types[0]);
                }

                param.setCompressionQuality(0.5f);
            }

            ImageOutputStream ios = ImageIO.createImageOutputStream(targetFile);
            writer.setOutput(ios);
            writer.write(
                    null,
                    new IIOImage(image, null, null),
                    param
            );
        } catch (Exception e) {
            throw new Exception("WebP 转换失败: " + e.getMessage(), e);
        } catch (Error e) {
            throw new Exception("WebP 转换失败: " + e.getMessage(), e);
        } finally {
            assert writer != null;
            writer.dispose();
            convertToWebpTaskCount -= 1;
        }
    }
}
