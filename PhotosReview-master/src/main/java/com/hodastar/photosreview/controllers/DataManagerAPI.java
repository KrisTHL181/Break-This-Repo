package com.hodastar.photosreview.controllers;

import com.hodastar.photosreview.entities.EntityReviewPhotos;
import com.hodastar.photosreview.entities.EntityReviewProj;
import com.hodastar.photosreview.entities.EntityReviewRecheck;
import com.hodastar.photosreview.mappers.ReviewMapper;
import com.hodastar.photosreview.mappers.UserMapper;
import com.hodastar.photosreview.service.DataService;
import com.hodastar.photosreview.utils.FileUtil;
import com.hodastar.photosreview.utils.OutputPhotosPath;
import com.hodastar.photosreview.utils.Respond;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/data_manager")
public class DataManagerAPI {
    private static final long MAX_MAIN_JSON_BYTES = 25L * 1024 * 1024;
    private static final long MAX_UNCOMPRESSED_ZIP_BYTES = 1024L * 1024 * 1024;
    private static final int MAX_ZIP_ENTRIES = 100_000;
    private static final Set<String> REQUIRED_PROJECT_DIRECTORIES = Set.of(
            "icon", "img", "proxy", "thumbnail"
    );

    // 冷却时间，单位为毫秒。
    private static final long PRELIMINARY_RESULT_COOLDOWN_MS = 60_000L;

    @Autowired
    private DataService dataService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ReviewMapper reviewMapper;
    private long lastSaveTime = 0;
    // 冷却键格式为“管理员 UID:工程 ID”，值为最近一次成功开始统计的时间戳。
    private final Map<String, Long> saveAllDataCooldown = new ConcurrentHashMap<>();
    private ObjectMapper jsonMapper = new ObjectMapper();

    // 保存全部数据
    @GetMapping("/save_all_proj_data")
    public void saveAllProjData(
            @RequestParam("adminUid") int uid,
            @RequestParam("adminToken") String token,
            HttpServletResponse response
    ) throws IOException {
        if (!userMapper.checkAdmin(uid, token)) {
            response.setStatus(403);
            return;
        }

        long now = System.currentTimeMillis();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
        String time = Instant.ofEpochMilli(now)
                .atZone(ZoneId.systemDefault())
                .format(formatter);

        response.setContentType("application/zip");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=output_all_project_" + time + ".zip"
        );

        HashMap<String, Object> data = dataService.saveAllProjData();
        String mainFile = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);

        if (!data.containsKey("proj") || !(data.get("proj") instanceof List)) {
            response.setStatus(403);
            return;
        }
        List<EntityReviewProj> projList = (List<EntityReviewProj>) data.get("proj");
        List<String> projIds = projList.stream()
                .map(proj -> proj.projId)
                .toList();

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            zos.putNextEntry(
                    new ZipEntry("main.json")
            );
            zos.write(mainFile.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            for (String projId : projIds) {
                FileUtil.zipDirectory(
                        OutputPhotosPath.projPath(projId),
                        zos
                );
            }
        }
    }

    // 保存工程数据
    @GetMapping("/save_proj_data")
    public void saveProjData(
            @RequestParam("adminUid") int uid,
            @RequestParam("adminToken") String token,
            @RequestParam("projId") String projId,
            HttpServletResponse response
    ) throws IOException {
        if (!userMapper.checkAdmin(uid, token)) {
            response.setStatus(403);
            return;
        }

        long now = System.currentTimeMillis();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
        String time = Instant.ofEpochMilli(now)
                .atZone(ZoneId.systemDefault())
                .format(formatter);

        response.setContentType("application/zip");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=output_project_" + projId + "_" + time + ".zip"
        );

        // 所有数据
        HashMap<String, Object> data = dataService.saveProjData(projId);
        if (data == null) {
            response.setStatus(403);
            return;
        }
        String mainFile = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            // 添加主文件
            zos.putNextEntry(
                    new ZipEntry("main.json")
            );
            zos.write(mainFile.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 添加工程目录
            FileUtil.zipDirectory(
                    OutputPhotosPath.projPath(projId),
                    zos
            );
        }
    }

    @PostMapping(value = "/input_proj_data", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Respond<Map<String, Object>>> inputProjData(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uid") int uid,
            @RequestParam("token") String token
    ) {
        if (!userMapper.checkAdmin(uid, token)) {
            return importResponse(HttpStatus.FORBIDDEN, false, "Administrator authentication failed", null);
        }
        if (file.isEmpty()) {
            return importResponse(HttpStatus.BAD_REQUEST, false, "Please select a ZIP archive", null);
        }

        Path extractRoot = null;
        Path stagedProjectDirectory = null;
        try {
            extractRoot = Files.createTempDirectory("photos-review-import-");
            extractZip(file, extractRoot);
            ImportArchive archive = validateImportArchive(extractRoot);

            Path targetDirectory = OutputPhotosPath.projPath(archive.proj().projId)
                    .toAbsolutePath().normalize();
            Path projectRoot = targetDirectory.getParent();
            if (projectRoot == null) {
                throw new IOException("Project storage directory is unavailable");
            }

            Files.createDirectories(projectRoot);
            stagedProjectDirectory = Files.createTempDirectory(projectRoot, ".project-import-");
            copyDirectory(archive.projectDirectory(), stagedProjectDirectory);

            dataService.importProjData(
                    archive.proj(), archive.photos(), archive.rechecks(),
                    stagedProjectDirectory, targetDirectory
            );

            Map<String, Object> result = new HashMap<>();
            result.put("projId", archive.proj().projId);
            result.put("photoCount", archive.photos().size());
            result.put("recheckCount", archive.rechecks().size());
            return importResponse(HttpStatus.OK, true, "Project imported successfully", result);
        } catch (InvalidImportException ex) {
            return importResponse(HttpStatus.BAD_REQUEST, false, ex.getMessage(), null);
        } catch (DataIntegrityViolationException ex) {
            return importResponse(HttpStatus.CONFLICT, false, "Project data conflicts with existing data", null);
        } catch (Exception ex) {
            ex.printStackTrace();
            return importResponse(HttpStatus.INTERNAL_SERVER_ERROR, false, "Project import failed", null);
        } finally {
            removeDirectoryQuietly(stagedProjectDirectory);
            removeDirectoryQuietly(extractRoot);
        }
    }

    // 导出选中图片的zip
    @GetMapping("/download_images")
    public void downloadImages(
            @RequestParam("adminUid") int adminUid,
            @RequestParam("adminToken") String adminToken,
            @RequestParam("proj") String proj,
            @RequestParam("list") List<Integer> ids,
            HttpServletResponse response
    ) throws IOException {
        // 检测权限
        if (!userMapper.checkAdmin(adminUid, adminToken)) {
            response.setStatus(403);
            return;
        }
        // 获取文件列表
        List<String> list = reviewMapper.getPhotoNamesByIds(ids, proj);
        if (list.isEmpty()) {
            response.setStatus(404);
            return;
        }

        long now = System.currentTimeMillis();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
        String time = Instant.ofEpochMilli(now)
                .atZone(ZoneId.systemDefault())
                .format(formatter);

        response.setContentType("application/zip");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=output_photos_" + time + ".zip"
        );

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (String file : list) {
                Path filePath = OutputPhotosPath.imgPath(proj, file);

                if (!Files.exists(filePath)) {
                    continue;
                }

                FileUtil.zipFileList(filePath, zos);
            }
        }
    }

    private ResponseEntity<Respond<Map<String, Object>>> importResponse(
            HttpStatus status,
            boolean result,
            String message,
            Map<String, Object> data
    ) {
        return ResponseEntity.status(status).body(new Respond<>(result, message, data));
    }

    private void extractZip(MultipartFile file, Path destination) {
        int entryCount = 0;
        long extractedBytes = 0;
        Set<Path> extractedEntries = new HashSet<>();
        byte[] buffer = new byte[8192];

        try (InputStream input = file.getInputStream();
             ZipInputStream zip = new ZipInputStream(input, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > MAX_ZIP_ENTRIES) {
                    throw new InvalidImportException("The ZIP archive contains too many entries");
                }

                String entryName = entry.getName().replace('\\', '/');
                if (entryName.isBlank()) {
                    throw new InvalidImportException("The ZIP archive contains an invalid entry");
                }
                Path output = destination.resolve(entryName).normalize();
                if (!output.startsWith(destination) || !extractedEntries.add(output)) {
                    throw new InvalidImportException("The ZIP archive contains an unsafe or duplicate path");
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(output);
                } else {
                    Files.createDirectories(output.getParent());
                    try (OutputStream out = Files.newOutputStream(output)) {
                        int read;
                        while ((read = zip.read(buffer)) != -1) {
                            extractedBytes += read;
                            if (extractedBytes > MAX_UNCOMPRESSED_ZIP_BYTES) {
                                throw new InvalidImportException("The ZIP archive is too large after extraction");
                            }
                            out.write(buffer, 0, read);
                        }
                    }
                }
                zip.closeEntry();
            }
        } catch (InvalidImportException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new InvalidImportException("Unable to read the ZIP archive", ex);
        }

        if (entryCount == 0) {
            throw new InvalidImportException("The ZIP archive is empty");
        }
    }

    private ImportArchive validateImportArchive(Path root) {
        Path mainJson = root.resolve("main.json");
        List<Path> children;
        try (Stream<Path> stream = Files.list(root)) {
            children = stream.toList();
        } catch (IOException ex) {
            throw new InvalidImportException("Unable to inspect the ZIP archive", ex);
        }

        List<Path> projectDirectories = children.stream()
                .filter(Files::isDirectory)
                .toList();
        if (children.size() != 2 || !Files.isRegularFile(mainJson) || projectDirectories.size() != 1) {
            throw new InvalidImportException(
                    "ZIP root must contain only main.json and one project directory"
            );
        }

        Path projectDirectory = projectDirectories.get(0);
        for (String directoryName : REQUIRED_PROJECT_DIRECTORIES) {
            if (!Files.isDirectory(projectDirectory.resolve(directoryName))) {
                throw new InvalidImportException(
                        "Project directory is missing required folder: " + directoryName
                );
            }
        }

        try {
            if (Files.size(mainJson) > MAX_MAIN_JSON_BYTES) {
                throw new InvalidImportException("main.json is too large");
            }
        } catch (IOException ex) {
            throw new InvalidImportException("Unable to inspect main.json", ex);
        }

        ParsedImport parsed = parseMainJson(mainJson);
        String folderProjectId = projectDirectory.getFileName().toString();
        if (!folderProjectId.equals(parsed.proj().projId)) {
            throw new InvalidImportException("Project directory name does not match proj.projId");
        }
        validateImportedRelationships(parsed);

        return new ImportArchive(
                parsed.proj(), parsed.photos(), parsed.rechecks(), projectDirectory
        );
    }

    private ParsedImport parseMainJson(Path mainJson) {
        JsonNode root;
        try (InputStream input = Files.newInputStream(mainJson)) {
            root = jsonMapper.readTree(input);
        } catch (Exception ex) {
            throw new InvalidImportException("main.json is not valid JSON", ex);
        }

        if (root == null || !root.isObject()) {
            throw new InvalidImportException("main.json root must be a HashMap object");
        }
        JsonNode dataNode = root.get("data");
        JsonNode recheckNode = root.get("recheck");
        JsonNode projNode = root.get("proj");
        if (dataNode == null || !dataNode.isArray()
                || recheckNode == null || !recheckNode.isArray()
                || projNode == null || !projNode.isObject()) {
            throw new InvalidImportException(
                    "main.json must contain data(array), recheck(array), and proj(object)"
            );
        }

        List<EntityReviewPhotos> photos = new ArrayList<>();
        for (JsonNode photoNode : dataNode) {
            photos.add(parsePhoto(photoNode));
        }

        List<EntityReviewRecheck> rechecks = new ArrayList<>();
        for (JsonNode item : recheckNode) {
            rechecks.add(parseRecheck(item));
        }

        return new ParsedImport(parseProject(projNode), photos, rechecks);
    }

    private EntityReviewPhotos parsePhoto(JsonNode node) {
        requireObject(node, "data item");
        return new EntityReviewPhotos(
                requireInt(node, "id"),
                requireText(node, "name"),
                requireText(node, "proj"),
                requireText(node, "author"),
                requireText(node, "value")
        );
    }

    private EntityReviewRecheck parseRecheck(JsonNode node) {
        requireObject(node, "recheck item");
        return new EntityReviewRecheck(
                requireInt(node, "photoid"),
                requireText(node, "proj"),
                requireText(node, "value"),
                requireDouble(node, "finalScore")
        );
    }

    private EntityReviewProj parseProject(JsonNode node) {
        requireObject(node, "proj");
        return new EntityReviewProj(
                requireInt(node, "id"),
                requireText(node, "projId"),
                requireText(node, "name"),
                requireInt(node, "type"),
                requireInt(node, "max"),
                requireText(node, "task"),
                requireText(node, "recheck"),
                requireText(node, "thumbnail"),
                requireInt(node, "status"),
                requireText(node, "time"),
                requireInt(node, "display")
        );
    }

    private void validateImportedRelationships(ParsedImport parsed) {
        String projId = parsed.proj().projId;
        if (projId == null || projId.isBlank()) {
            throw new InvalidImportException("proj.projId cannot be blank");
        }

        Set<Integer> photoIds = new HashSet<>();
        for (EntityReviewPhotos photo : parsed.photos()) {
            if (!projId.equals(photo.proj)) {
                throw new InvalidImportException("Every data item must belong to proj.projId");
            }
            if (!photoIds.add(photo.id)) {
                throw new InvalidImportException("data contains duplicate photo ids");
            }
        }

        Set<Integer> recheckPhotoIds = new HashSet<>();
        for (EntityReviewRecheck recheck : parsed.rechecks()) {
            if (!projId.equals(recheck.proj)) {
                throw new InvalidImportException("Every recheck item must belong to proj.projId");
            }
            if (!photoIds.contains(recheck.photoid)) {
                throw new InvalidImportException("recheck contains a photoid not present in data");
            }
            if (!recheckPhotoIds.add(recheck.photoid)) {
                throw new InvalidImportException("recheck contains duplicate photoid values");
            }
        }
    }

    private void requireObject(JsonNode node, String label) {
        if (node == null || !node.isObject()) {
            throw new InvalidImportException(label + " must be an object");
        }
    }

    private String requireText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) {
            throw new InvalidImportException(field + " must be a string");
        }
        return value.asText();
    }

    private int requireInt(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isIntegralNumber()
                || value.longValue() < Integer.MIN_VALUE
                || value.longValue() > Integer.MAX_VALUE) {
            throw new InvalidImportException(field + " must be an integer");
        }
        return value.intValue();
    }

    private double requireDouble(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isNumber() || !Double.isFinite(value.doubleValue())) {
            throw new InvalidImportException(field + " must be a finite number");
        }
        return value.doubleValue();
    }

    private void copyDirectory(Path source, Path destination) throws IOException {
        try (Stream<Path> stream = Files.walk(source)) {
            for (Path sourcePath : stream.toList()) {
                Path targetPath = destination.resolve(source.relativize(sourcePath)).normalize();
                if (!targetPath.startsWith(destination)) {
                    throw new IOException("Invalid project directory path");
                }
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void removeDirectoryQuietly(Path directory) {
        if (directory == null) {
            return;
        }
        try {
            FileUtil.removeDir(directory.toString());
        } catch (Exception ignored) {
            // Best-effort cleanup for upload/extraction staging directories.
        }
    }

    private record ParsedImport(
            EntityReviewProj proj,
            List<EntityReviewPhotos> photos,
            List<EntityReviewRecheck> rechecks
    ) {}

    private record ImportArchive(
            EntityReviewProj proj,
            List<EntityReviewPhotos> photos,
            List<EntityReviewRecheck> rechecks,
            Path projectDirectory
    ) {}

    private static class InvalidImportException extends RuntimeException {
        InvalidImportException(String message) {
            super(message);
        }

        InvalidImportException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
