package com.bella.cango.client;

import com.alibaba.fastjson.JSONObject;
import com.bella.cango.dto.CangoRequestDto;
import com.bella.cango.dto.CangoResponseDto;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    public CangoResponseDto request(RequestCommand command, CangoRequestDto requestDto) throws IOException, IllegalAccessException {
        String uri = buildUri(command);
        HttpPost httpPost = buildHttpPost(uri, requestDto);
        return parseResponse(httpPost);
    }

    private HttpPost buildHttpPost(String uri, CangoRequestDto requestDto) throws UnsupportedEncodingException, IllegalAccessException {
        HttpPost httpPost = new HttpPost(uri);
        UrlEncodedFormEntity formEntity = buildFormEntity(requestDto);
        httpPost.setEntity(formEntity);
        return httpPost;
    }

    private UrlEncodedFormEntity buildFormEntity(CangoRequestDto requestDto) throws IllegalAccessException {
        List<NameValuePair> nameValuePairs = new ArrayList<>();
        Class<CangoRequestDto> dtoClass = CangoRequestDto.class;
        Field[] declaredFields = dtoClass.getDeclaredFields();
        if (declaredFields != null && declaredFields.length > 0) {
            for (Field declaredField : declaredFields) {
                if (!Modifier.isStatic(declaredField.getModifiers())) {
                    String declaredFieldName = declaredField.getName();
                    declaredField.setAccessible(true);
                    Object value = declaredField.get(requestDto);
                    if (value != null) {
                        Class<?> type = declaredField.getType();
                        if (type.isPrimitive()
                                || Integer.class.equals(type)
                                || String.class.equals(type)
                                || type.isEnum()) {
                            nameValuePairs.add(new BasicNameValuePair(declaredFieldName, value.toString()));
                        }

                        if (Set.class.isAssignableFrom(type)) {
                            Set<String> set = (Set) value;
                            StringBuilder stringBuilder = new StringBuilder();
                            for (String item : set) {
                                stringBuilder.append(item).append(",");
                            }

                            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                            nameValuePairs.add(new BasicNameValuePair(declaredFieldName, stringBuilder.toString()));
                        }
                    }

                    declaredField.setAccessible(false);
                }
            }
        }

        return new UrlEncodedFormEntity(nameValuePairs, UTF_8);
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
