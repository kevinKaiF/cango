package com.bella.cango.server.util;

import com.alibaba.fastjson.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/9/25
 */
public class HttpServletResponseUtil {
    public static <T> void response(HttpServletResponse response, T t) {
        OutputStream outputStream = null;
        try {
            response.setContentType("application/json;charset=UTF-8");
            outputStream = response.getOutputStream();
            outputStream.write(JSONObject.toJSONString(t).getBytes("UTF-8"));
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
