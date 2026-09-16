package com.zhian.gateway.web.controller.common;

import com.zhian.gateway.common.utils.file.TempFileStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 临时文件 HTTP 访问。路径形如 /temp/snap/2026/08/25/xxx.jpg
 */
@RestController
public class TempFileController {

    @Autowired
    private TempFileStore tempFileStore;

    @GetMapping(TempFileStore.URL_PREFIX + "/**")
    public void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String relative = extractRelative(request);
        Path file = tempFileStore.resolvePublicPath(relative);
        if (!Files.isRegularFile(file)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType(tempFileStore.contentType(file));
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName() + "\"");
        response.setContentLengthLong(Files.size(file));
        try (InputStream in = Files.newInputStream(file)) {
            StreamUtils.copy(in, response.getOutputStream());
        }
        response.flushBuffer();
    }

    private String extractRelative(HttpServletRequest request) throws UnsupportedEncodingException {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && uri.startsWith(context)) {
            uri = uri.substring(context.length());
        }
        String prefix = TempFileStore.URL_PREFIX + "/";
        int idx = uri.indexOf(prefix);
        String relative = idx >= 0 ? uri.substring(idx + prefix.length()) : "";
        return URLDecoder.decode(relative, String.valueOf(StandardCharsets.UTF_8));
    }
}
