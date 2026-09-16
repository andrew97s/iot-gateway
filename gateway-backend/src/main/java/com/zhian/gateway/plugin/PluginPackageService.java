package com.zhian.gateway.plugin;

import com.zhian.gateway.common.exception.ServiceException;
import com.zhian.gateway.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 插件包安装：解压 zip（plugin.yaml + config-schema.json + 可选 module/）
 */
@Slf4j
@Service
public class PluginPackageService {

    @Autowired
    private PluginCatalog pluginCatalog;

    /**
     * 上传并安装插件包，返回类型描述
     */
    public PluginDescriptor installPackage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请上传插件包（zip）");
        }
        String original = file.getOriginalFilename();
        if (original == null || !original.toLowerCase().endsWith(".zip")) {
            throw new ServiceException("插件包必须是 .zip 文件");
        }

        Path root = PluginPaths.packagesRoot();
        try {
            Files.createDirectories(root);
            Path tempZip = Files.createTempFile("plugin-", ".zip");
            file.transferTo(tempZip.toFile());
            Path tempDir = Files.createTempDirectory("plugin-extract-");
            try {
                unzip(tempZip, tempDir);
                Path contentRoot = findContentRoot(tempDir);
                Path yaml = contentRoot.resolve("plugin.yaml");
                if (!Files.exists(yaml)) {
                    throw new ServiceException("插件包缺少 plugin.yaml");
                }
                PluginDescriptor descriptor;
                try (InputStream in = Files.newInputStream(yaml)) {
                    descriptor = pluginCatalog.parseYaml(in, null);
                }
                if (StringUtils.isEmpty(descriptor.getId())) {
                    throw new ServiceException("plugin.yaml 缺少 id");
                }
                Path schema = contentRoot.resolve("config-schema.json");
                if (Files.exists(schema)) {
                    try (InputStream in = Files.newInputStream(schema)) {
                        descriptor.setConfigSchema(pluginCatalog.parseSchema(in));
                    }
                }
                Path target = PluginPaths.packageDir(descriptor.getId());
                if (Files.exists(target)) {
                    deleteRecursive(target);
                }
                Files.createDirectories(target.getParent());
                copyRecursive(contentRoot, target);
                descriptor.setFromPackage(true);
                descriptor.setPackagePath(target.toAbsolutePath().toString());
                descriptor.setRunnable(com.zhian.gateway.third.ThirdApplicationRunner.getHandler(descriptor.getId()) != null);
                pluginCatalog.registerOrUpdate(descriptor);
                pluginCatalog.refresh();
                PluginDescriptor registered = pluginCatalog.getType(descriptor.getId());
                if (registered != null && !registered.isRunnable()) {
                    log.warn("插件包 {} 已安装元数据，但当前网关未内置对应 Handler，实例将无法真正启动", descriptor.getId());
                }
                return pluginCatalog.getType(descriptor.getId());
            } finally {
                Files.deleteIfExists(tempZip);
                deleteRecursive(tempDir);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("安装插件包失败", e);
            throw new ServiceException("安装插件包失败: " + e.getMessage());
        }
    }

    private Path findContentRoot(Path tempDir) throws IOException {
        Path direct = tempDir.resolve("plugin.yaml");
        if (Files.exists(direct)) {
            return tempDir;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir)) {
            for (Path p : stream) {
                if (Files.isDirectory(p) && Files.exists(p.resolve("plugin.yaml"))) {
                    return p;
                }
            }
        }
        throw new ServiceException("未在压缩包中找到 plugin.yaml");
    }

    private void unzip(Path zipPath, Path destDir) throws IOException {
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path out = destDir.resolve(entry.getName()).normalize();
                if (!out.startsWith(destDir)) {
                    throw new IOException("非法压缩条目: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    try (InputStream in = zip.getInputStream(entry)) {
                        Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    private void copyRecursive(Path src, Path dest) throws IOException {
        Files.walk(src).forEach(path -> {
            try {
                Path rel = src.relativize(path);
                Path target = dest.resolve(rel.toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void deleteRecursive(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        Files.walk(root)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
    }
}
