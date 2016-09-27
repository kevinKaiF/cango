package com.bella.cango.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bella.cango.dto.CangoRequestDto;
import com.bella.cango.dto.CangoResponseDto;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/9/16
 */
public class CangoClient {
    final String suffixRequestUrl = ".htm";
    final Charset UTF_8 = Charset.forName("UTF-8");
    private String host;
    private int port;
    private String appName;
    private String appPath;

    public CangoClient(String host, int port, String appName) {
        this.host = host;
        this.port = port;
        this.appName = appName;
        this.appPath = host + ":" + port + "/" + appName;
    }

    public CangoResponseDto request(RequestCommand command, CangoRequestDto requestDto) throws IOException {
        String uri = buildUri(command);
        HttpPost httpPost = buildHttpPost(uri, requestDto);
        return parseResponse(httpPost);
    }

    private HttpPost buildHttpPost(String uri, CangoRequestDto requestDto) {
        HttpPost httpPost = new HttpPost(uri);
        httpPost.addHeader("content-type", "application/json");
        StringEntity entity = new StringEntity(JSON.toJSONString(requestDto), ContentType.APPLICATION_JSON);
        httpPost.setEntity(entity);
        return httpPost;
    }


    private CangoResponseDto parseResponse(HttpPost httpPost) throws IOException {
        CloseableHttpResponse httpResponse = null;
        CloseableHttpClient httpClient = null;
        try {
            httpClient = HttpClients.createDefault();
            httpResponse = httpClient.execute(httpPost);
            String jsonResponse = EntityUtils.toString(httpResponse.getEntity(), UTF_8);
            return JSONObject.parseObject(jsonResponse, CangoResponseDto.class);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            httpResponse.close();
            httpClient.close();
        }
        return null;
    }

    private String buildUri(RequestCommand command) {
        String url = "http://" + appPath + "/" + command.getCommand() + suffixRequestUrl;
        return url;
    }

}
