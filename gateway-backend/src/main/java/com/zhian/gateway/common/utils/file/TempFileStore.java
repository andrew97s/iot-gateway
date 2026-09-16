package com.zhian.gateway.common.utils.file;

import com.zhian.gateway.common.config.ZhianConfig;
import com.zhian.gateway.common.exception.ServiceException;
import com.zhian.gateway.common.utils.DateUtils;
import com.zhian.gateway.common.utils.StringUtils;
import com.zhian.gateway.common.utils.ip.IpUtils;
import com.zhian.gateway.common.utils.uuid.IdUtils;
import com.zhian.gateway.framework.config.ServerConfig;
import com.zhian.gateway.system.service.ISysConfigService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * 全局临时文件存储。
 * <p>
 * 目录和清理策略读取系统核心参数 {@link #KEY_DIR}、{@link #KEY_RETAIN_HOURS}。
 */
@Slf4j
@Component
public class TempFileStore {

    public static final String KEY_DIR = "gateway.temp.dir";
    public static final String KEY_RETAIN_HOURS = "gateway.temp.retain.hours";
    public static final int DEFAULT_RETAIN_HOURS = 24;
    public static final String URL_PREFIX = "/temp";
    private static final String MARKER = ".gateway-temp";

    @Autowired
    private ISysConfigService configService;

    @Value("${server.port:9200}")
    private int serverPort;

    /**
     * 保存临时文件并返回访问信息。
     *
     * @param data      文件内容
     * @param category  业务子目录，如 snap
     * @param extension 扩展名，不含点，如 jpg
     */
    public SavedFile save(byte[] data, String category, String extension) {
        if (data == null || data.length == 0) {
            throw new ServiceException("临时文件内容为空");
        }
        String ext = normalizeExt(extension);
        String cat = normalizeCategory(category);
        Path root = ensureRoot();
        String relative = cat + "/" + DateUtils.datePath() + "/" + IdUtils.fastSimpleUUID() + "." + ext;
        Path target = resolveSafe(root, relative);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, data, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new ServiceException("保存临时文件失败: " + e.getMessage());
        }
        String urlPath = URL_PREFIX + "/" + relative.replace('\\', '/');
        return new SavedFile(
                target, relative.replace('\\', '/'), urlPath, publicBaseUrl() + urlPath);
    }

    /**
     * 将 URL 中 /temp/ 之后的相对路径解析为本地文件。
     */
    public Path resolvePublicPath(String relativePath) {
        if (StringUtils.isEmpty(relativePath)) {
            throw new ServiceException("临时文件路径为空");
        }
        String relative = relativePath.replace('\\', '/');
        while (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        if (relative.contains("..")) {
            throw new ServiceException("非法的临时文件路径");
        }
        return resolveSafe(ensureRoot(), relative);
    }

    public String contentType(Path file) {
        String name = file.getFileName() == null ? "" : file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        if (name.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (name.endsWith(".gif")) {
            return MediaType.IMAGE_GIF_VALUE;
        }
        if (name.endsWith(".bmp")) {
            return "image/bmp";
        }
        try {
            String probed = Files.probeContentType(file);
            if (probed != null) {
                return probed;
            }
        } catch (IOException ignored) {
            /* */
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    public Path resolveRoot() {
        String configured = config(KEY_DIR, "");
        Path path;
        if (StringUtils.isEmpty(configured)) {
            path = Paths.get(ZhianConfig.getProfile(), "temp");
        } else {
            Path given = Paths.get(configured.trim());
            path = given.isAbsolute() ? given : Paths.get(ZhianConfig.getProfile()).resolve(given);
        }
        return path.toAbsolutePath().normalize();
    }

    public int retainHours() {
        try {
            int value = Integer.parseInt(config(KEY_RETAIN_HOURS, String.valueOf(DEFAULT_RETAIN_HOURS)));
            return Math.max(1, Math.min(value, 24 * 365));
        } catch (Exception e) {
            return DEFAULT_RETAIN_HOURS;
        }
    }

    @Scheduled(cron = "0 25 * * * *")
    public void cleanup() {
        Path root = resolveRoot();
        Path marker = root.resolve(MARKER);
        if (!Files.isDirectory(root) || !Files.exists(marker)) {
            return;
        }
        Instant expireBefore = Instant.now().minusSeconds(retainHours() * 3600L);
        List<Path> expired = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                    .filter(path -> !MARKER.equals(path.getFileName().toString()))
                    .forEach(path -> {
                        try {
                            FileTime time = Files.getLastModifiedTime(path);
                            if (time.toInstant().isBefore(expireBefore)) {
                                expired.add(path);
                            }
                        } catch (IOException ignored) {
                            /* */
                        }
                    });
        } catch (IOException e) {
            log.warn("扫描临时文件失败: {}", e.getMessage());
            return;
        }
        int deleted = 0;
        for (Path path : expired) {
            try {
                Files.deleteIfExists(path);
                deleted++;
            } catch (IOException ignored) {
                /* */
            }
        }
        pruneEmptyDirs(root);
        if (deleted > 0) {
            log.info("已清理 {} 个过期临时文件，目录 {}", deleted, root);
        }
    }

    private Path ensureRoot() {
        Path root = resolveRoot();
        try {
            Files.createDirectories(root);
            Path marker = root.resolve(MARKER);
            if (!Files.exists(marker)) {
                Files.write(marker, "zhian-gateway temp".getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new ServiceException("创建临时目录失败: " + root + "，" + e.getMessage());
        }
        return root;
    }

    private Path resolveSafe(Path root, String relative) {
        Path target = root.resolve(relative).normalize().toAbsolutePath();
        if (!target.startsWith(root)) {
            throw new ServiceException("非法的临时文件路径");
        }
        return target;
    }

    private void pruneEmptyDirs(Path root) {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(root))
                    .filter(Files::isDirectory)
                    .forEach(dir -> {
                        try (Stream<Path> children = Files.list(dir)) {
                            if (!children.findAny().isPresent()) {
                                Files.deleteIfExists(dir);
                            }
                        } catch (IOException ignored) {
                            /* */
                        }
                    });
        } catch (IOException ignored) {
            /* */
        }
    }

    private String publicBaseUrl() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                if (request != null) {
                    return ServerConfig.getDomain(request);
                }
            }
        } catch (Exception ignored) {
            /* */
        }
        return "http://" + IpUtils.getHostIp() + ":" + serverPort;
    }

    private String config(String key, String defaultValue) {
        try {
            String value = configService.selectConfigByKey(key);
            return StringUtils.isEmpty(value) ? defaultValue : value.trim();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static String normalizeExt(String extension) {
        String ext = extension == null ? "" : extension.trim().toLowerCase(Locale.ROOT);
        if (ext.startsWith(".")) {
            ext = ext.substring(1);
        }
        if (!ext.matches("[a-z0-9]{1,8}")) {
            throw new ServiceException("不支持的临时文件扩展名");
        }
        return ext;
    }

    private static String normalizeCategory(String category) {
        String cat = StringUtils.isEmpty(category) ? "misc" : category.trim();
        if (!cat.matches("[a-zA-Z0-9_-]{1,32}")) {
            throw new ServiceException("非法的临时文件分类");
        }
        return cat;
    }

    @Getter
    public static class SavedFile {
        private final Path file;
        private final String relativePath;
        private final String urlPath;
        private final String httpUrl;

        public SavedFile(Path file, String relativePath, String urlPath, String httpUrl) {
            this.file = file;
            this.relativePath = relativePath;
            this.urlPath = urlPath;
            this.httpUrl = httpUrl;
        }
    }
}
