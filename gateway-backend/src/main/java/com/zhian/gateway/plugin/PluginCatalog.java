package com.zhian.gateway.plugin;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zhian.gateway.third.ThirdApplicationRunner;
import com.zhian.gateway.third.ThirdHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件类型目录：Spring Handler + classpath/plugins + 已安装插件包
 */
@Slf4j
@Component
public class PluginCatalog {

    /** 北向/内部非南向接入的类型，不在「接入插件」目录展示 */
    private static final Set<String> EXCLUDED_TYPES = new HashSet<>(Arrays.asList(
            "gateway", "cascade", "casecade-server", "cascade-server"
    ));

    private final Map<String, PluginDescriptor> descriptors = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refresh();
    }

    public synchronized void refresh() {
        Map<String, PluginDescriptor> next = new ConcurrentHashMap<>();

        // 1) 已注册的 Handler
        for (String code : ThirdApplicationRunner.getRegisteredPlatforms()) {
            if (EXCLUDED_TYPES.contains(code)) {
                continue;
            }
            ThirdHandler handler = ThirdApplicationRunner.getHandler(code);
            if (handler == null) {
                continue;
            }
            PluginDescriptor d = baseFromHandler(handler);
            mergeClasspathMeta(d);
            d.setRunnable(true);
            next.put(d.getId(), d);
        }

        // 2) classpath:/plugins/*/plugin.yaml（可补充尚未写 Handler 元数据的类型）
        loadClasspathYaml(next);

        // 3) 已安装到本地目录的插件包
        loadInstalledPackages(next);

        descriptors.clear();
        descriptors.putAll(next);
        log.info("插件目录已刷新，共 {} 个接入插件类型", descriptors.size());
    }

    public List<PluginDescriptor> listTypes() {
        List<PluginDescriptor> list = new ArrayList<>(descriptors.values());
        list.sort(Comparator.comparing(PluginDescriptor::getName, Comparator.nullsLast(String::compareTo)));
        return list;
    }

    public PluginDescriptor getType(String pluginId) {
        return descriptors.get(pluginId);
    }

    public boolean isAccessPlugin(String pluginId) {
        return pluginId != null && !EXCLUDED_TYPES.contains(pluginId) && descriptors.containsKey(pluginId);
    }

    public boolean isExcluded(String pluginId) {
        return pluginId != null && EXCLUDED_TYPES.contains(pluginId);
    }

    public void registerOrUpdate(PluginDescriptor descriptor) {
        if (descriptor == null || descriptor.getId() == null) {
            return;
        }
        PluginDescriptor existing = descriptors.get(descriptor.getId());
        if (existing != null) {
            merge(existing, descriptor);
            if (ThirdApplicationRunner.getHandler(descriptor.getId()) != null) {
                existing.setRunnable(true);
            }
        } else {
            if (ThirdApplicationRunner.getHandler(descriptor.getId()) != null) {
                descriptor.setRunnable(true);
            }
            descriptors.put(descriptor.getId(), descriptor);
        }
    }

    private PluginDescriptor baseFromHandler(ThirdHandler handler) {
        PluginDescriptor d = new PluginDescriptor();
        d.setId(handler.getPlatform());
        d.setName(defaultName(handler));
        d.setVendor(handler.getVendor());
        d.setProtocol(handler.getProtocol());
        d.setVersion(handler.getVersion());
        d.setCapabilities(new ArrayList<>(handler.getCapabilities()));
        d.setDescription(handler.getDescription());
        List<Map<String, Object>> schema = handler.getConfigSchema();
        if (schema != null && !schema.isEmpty()) {
            d.setConfigSchema(new ArrayList<>(schema));
        }
        return d;
    }

    private String defaultName(ThirdHandler handler) {
        String desc = handler.getDescription();
        if (desc != null && !desc.isEmpty() && !desc.equals(handler.getProtocol())) {
            return desc;
        }
        return handler.getPlatform() + " 接入插件";
    }

    private void mergeClasspathMeta(PluginDescriptor d) {
        try {
            Resource yamlRes = new PathMatchingResourcePatternResolver()
                    .getResource("classpath:plugins/" + d.getId() + "/plugin.yaml");
            if (yamlRes.exists()) {
                try (InputStream in = yamlRes.getInputStream()) {
                    merge(d, parseYaml(in, d.getId()));
                }
            }
            Resource schemaRes = new PathMatchingResourcePatternResolver()
                    .getResource("classpath:plugins/" + d.getId() + "/config-schema.json");
            if (schemaRes.exists()) {
                try (InputStream in = schemaRes.getInputStream()) {
                    List<Map<String, Object>> schema = parseSchema(in);
                    if (!schema.isEmpty()) {
                        d.setConfigSchema(schema);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("加载 classpath 插件元数据失败 {}: {}", d.getId(), e.getMessage());
        }
    }

    private void loadClasspathYaml(Map<String, PluginDescriptor> next) {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:plugins/*/plugin.yaml");
            for (Resource res : resources) {
                String filename = res.getURL().toString();
                String id = extractIdFromPath(filename);
                if (id == null || EXCLUDED_TYPES.contains(id)) {
                    continue;
                }
                try (InputStream in = res.getInputStream()) {
                    PluginDescriptor d = parseYaml(in, id);
                    PluginDescriptor existing = next.get(id);
                    if (existing == null) {
                        d.setRunnable(ThirdApplicationRunner.getHandler(id) != null);
                        next.put(id, d);
                    } else {
                        merge(existing, d);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("扫描 classpath 插件描述失败: {}", e.getMessage());
        }
    }

    private void loadInstalledPackages(Map<String, PluginDescriptor> next) {
        Path root = PluginPaths.packagesRoot();
        if (!Files.isDirectory(root)) {
            return;
        }
        try {
            Files.list(root).filter(Files::isDirectory).forEach(dir -> {
                Path yaml = dir.resolve("plugin.yaml");
                if (!Files.exists(yaml)) {
                    return;
                }
                try (InputStream in = Files.newInputStream(yaml)) {
                    PluginDescriptor d = parseYaml(in, dir.getFileName().toString());
                    d.setFromPackage(true);
                    d.setPackagePath(dir.toAbsolutePath().toString());
                    Path schema = dir.resolve("config-schema.json");
                    if (Files.exists(schema)) {
                        try (InputStream sin = Files.newInputStream(schema)) {
                            List<Map<String, Object>> s = parseSchema(sin);
                            if (!s.isEmpty()) {
                                d.setConfigSchema(s);
                            }
                        }
                    }
                    d.setRunnable(ThirdApplicationRunner.getHandler(d.getId()) != null);
                    PluginDescriptor existing = next.get(d.getId());
                    if (existing == null) {
                        next.put(d.getId(), d);
                    } else {
                        merge(existing, d);
                        existing.setFromPackage(true);
                        existing.setPackagePath(d.getPackagePath());
                    }
                } catch (Exception e) {
                    log.warn("加载已安装插件包失败 {}: {}", dir, e.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("扫描已安装插件目录失败: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public PluginDescriptor parseYaml(InputStream in, String fallbackId) {
        Yaml yaml = new Yaml();
        Map<String, Object> map = yaml.load(in);
        PluginDescriptor d = new PluginDescriptor();
        if (map == null) {
            d.setId(fallbackId);
            d.setName(fallbackId);
            return d;
        }
        d.setId(str(map.get("id"), fallbackId));
        d.setName(str(map.get("name"), d.getId()));
        d.setVendor(str(map.get("vendor"), ""));
        d.setProtocol(str(map.get("protocol"), ""));
        d.setVersion(str(map.get("version"), "1.0.0"));
        d.setDescription(str(map.get("description"), d.getName()));
        Object caps = map.get("capabilities");
        if (caps instanceof List) {
            List<String> list = new ArrayList<>();
            for (Object c : (List<?>) caps) {
                if (c != null) {
                    list.add(String.valueOf(c));
                }
            }
            d.setCapabilities(list);
        }
        return d;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> parseSchema(InputStream in) throws Exception {
        byte[] bytes = readAll(in);
        String text = new String(bytes, StandardCharsets.UTF_8).trim();
        if (text.isEmpty()) {
            return Collections.emptyList();
        }
        Object parsed = JSON.parse(text);
        if (parsed instanceof JSONArray) {
            return (List<Map<String, Object>>) (List<?>) ((JSONArray) parsed).toJavaList(Map.class);
        }
        if (parsed instanceof JSONObject) {
            JSONObject obj = (JSONObject) parsed;
            if (obj.get("fields") instanceof JSONArray) {
                return (List<Map<String, Object>>) (List<?>) obj.getJSONArray("fields").toJavaList(Map.class);
            }
            if (obj.get("properties") instanceof JSONObject) {
                // 简易 JSON Schema → 内部字段列表
                List<Map<String, Object>> fields = new ArrayList<>();
                JSONObject props = obj.getJSONObject("properties");
                JSONArray required = obj.getJSONArray("required");
                Set<String> req = new HashSet<>();
                if (required != null) {
                    for (Object r : required) {
                        req.add(String.valueOf(r));
                    }
                }
                for (String key : props.keySet()) {
                    JSONObject p = props.getJSONObject(key);
                    Map<String, Object> field = new LinkedHashMap<>();
                    field.put("code", key);
                    field.put("name", p.getString("title") != null ? p.getString("title") : key);
                    field.put("desc", p.getString("description"));
                    field.put("type", mapJsonSchemaType(p.getString("type")));
                    field.put("required", req.contains(key));
                    if (p.containsKey("default")) {
                        field.put("defaultValue", p.get("default"));
                    }
                    fields.add(field);
                }
                return fields;
            }
        }
        return Collections.emptyList();
    }

    private String mapJsonSchemaType(String t) {
        if (t == null) {
            return "string";
        }
        switch (t) {
            case "integer":
                return "integer";
            case "number":
                return "number";
            case "boolean":
                return "boolean";
            default:
                return "string";
        }
    }

    private void merge(PluginDescriptor target, PluginDescriptor src) {
        if (src.getName() != null && !src.getName().isEmpty()) {
            target.setName(src.getName());
        }
        if (src.getVendor() != null && !src.getVendor().isEmpty()) {
            target.setVendor(src.getVendor());
        }
        if (src.getProtocol() != null && !src.getProtocol().isEmpty()) {
            target.setProtocol(src.getProtocol());
        }
        if (src.getVersion() != null && !src.getVersion().isEmpty()) {
            target.setVersion(src.getVersion());
        }
        if (src.getDescription() != null && !src.getDescription().isEmpty()) {
            target.setDescription(src.getDescription());
        }
        if (src.getCapabilities() != null && !src.getCapabilities().isEmpty()) {
            target.setCapabilities(src.getCapabilities());
        }
        if (src.getConfigSchema() != null && !src.getConfigSchema().isEmpty()) {
            target.setConfigSchema(src.getConfigSchema());
        }
    }

    private String extractIdFromPath(String path) {
        // .../plugins/{id}/plugin.yaml
        int idx = path.lastIndexOf("/plugins/");
        if (idx < 0) {
            idx = path.lastIndexOf("plugins/");
            if (idx < 0) {
                return null;
            }
            idx += "plugins/".length();
        } else {
            idx += "/plugins/".length();
        }
        int end = path.indexOf('/', idx);
        if (end < 0) {
            return null;
        }
        return path.substring(idx, end);
    }

    private static String str(Object v, String def) {
        return v == null || String.valueOf(v).trim().isEmpty() ? def : String.valueOf(v).trim();
    }

    private static byte[] readAll(InputStream in) throws Exception {
        byte[] buf = new byte[8192];
        int n;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        while ((n = in.read(buf)) >= 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
}
