package com.zhian.gateway.common.utils.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 包装 HttpResponse 实现 可重复读取响应流
 *
 * @author tongwenjin
 * @since 2022 /11/10
 */
@Slf4j
public class HttpResponseMessageWrapper implements ClientHttpResponse {

    private ClientHttpResponse object;

    private final ByteArrayInputStream  body;

    /**
     * Instantiates a new Http response message wrapper.
     *
     * @param object the object
     */
    public HttpResponseMessageWrapper(ClientHttpResponse object) {
        this.object = object;

        // 利用ByteArrayInputStream 实现 可重复读取 inputStream
        try {
           body = new ByteArrayInputStream(
                   HttpHelper.getStringFromInputStream(
                           object.getBody()).getBytes(StandardCharsets.UTF_8)
           );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the body of the message as an input stream.
     *
     * @return the input stream body (never {@code null})
     * @throws IOException in case of I/O errors
     */
    @Override
    public InputStream getBody() throws IOException {
        return body;
    }

    /**
     * Return the headers of this message.
     *
     * @return a corresponding HttpHeaders object (never {@code null})
     */
    @Override
    public HttpHeaders getHeaders() {
        return object.getHeaders();
    }

    /**
     * Get the HTTP status code as an {@link HttpStatus} enum value.
     * <p>For status codes not supported by {@code HttpStatus}, use
     * {@link #getRawStatusCode()} instead.
     *
     * @return the HTTP status as an HttpStatus enum value (never {@code null})
     * @throws IOException              in case of I/O errors
     * @throws IllegalArgumentException in case of an unknown HTTP status code
     * @see HttpStatus#valueOf(int)
     * @since #getRawStatusCode()
     */
    @Override
    public HttpStatus getStatusCode() throws IOException {
        return object.getStatusCode();
    }

    /**
     * Get the HTTP status code (potentially non-standard and not
     * resolvable through the {@link HttpStatus} enum) as an integer.
     *
     * @return the HTTP status as an integer value
     * @throws IOException in case of I/O errors
     * @see #getStatusCode()
     * @see HttpStatus#resolve(int)
     * @since 3.1.1
     */
    @Override
    public int getRawStatusCode() throws IOException {
        return object.getRawStatusCode();
    }

    /**
     * Get the HTTP status text of the response.
     *
     * @return the HTTP status text
     * @throws IOException in case of I/O errors
     */
    @Override
    public String getStatusText() throws IOException {
        return object.getStatusText();
    }

    /**
     * Close this response, freeing any resources created.
     */
    @Override
    public void close() {
        object.close();
    }
}
