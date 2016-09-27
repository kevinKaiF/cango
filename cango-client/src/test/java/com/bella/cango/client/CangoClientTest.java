package com.bella.cango.client;

import com.bella.cango.dto.CangoRequestDto;
import com.bella.cango.dto.CangoResponseDto;
import com.bella.cango.enums.CangoRspStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/9/24
 */
public class CangoClientTest {
    private CangoClient cangoClient;
    private String host = "localhost";
    private int port = 8080;
    private String appName = "cango";

    @Before
    public void init() {
        cangoClient = new CangoClient(host, port, appName);
    }

    @Test
    public void testAdd() throws IOException {
        CangoRequestDto cangoRequestDto = new CangoRequestDto();
        cangoRequestDto.setHost("192.168.0.113");
        final CangoResponseDto responseDto = cangoClient.request(RequestCommand.ADD, cangoRequestDto);
        Assert.assertEquals(CangoRspStatus.FAILURE, responseDto.getStatus());
    }
}
