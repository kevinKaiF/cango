package com.bella.cango.server.action;

import com.bella.cango.dto.CangoRequestDto;
import com.bella.cango.dto.CangoResponseDto;
import com.bella.cango.enums.CangoRspStatus;
import com.bella.cango.enums.DbType;
import com.bella.cango.server.util.EnumEditor;
import com.bella.cango.server.util.HttpServletResponseUtil;
import com.bella.cango.service.CangoManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/8/22
 */
@Controller
public class ServerAction {

    @Autowired
    private CangoManagerService cangoManagerService;

    @InitBinder
    public void initBinder(WebDataBinder webDataBinder) {
        webDataBinder.registerCustomEditor(DbType.class, new EnumEditor(DbType.class));
    }

    @RequestMapping("add")
    @ResponseBody
    public void add(HttpServletResponse response, @Validated CangoRequestDto cangoRequestDto, BindingResult result) {
        CangoResponseDto responseDto = null;
        if (result.hasErrors()) {
            responseDto = buildFailedResponse(result);
        } else {
            responseDto = cangoManagerService.add(cangoRequestDto);
        }
        HttpServletResponseUtil.response(response, responseDto);
    }

    @RequestMapping("check")
    public void check(HttpServletResponse response, @Validated CangoRequestDto cangoRequestDto, BindingResult result) {
        CangoResponseDto responseDto = null;
        if (result.hasErrors()) {
            responseDto = buildFailedResponse(result);
        } else {
            responseDto = cangoManagerService.check(cangoRequestDto);
        }
        HttpServletResponseUtil.response(response, responseDto);
    }

    @RequestMapping("start")
    public void start(HttpServletResponse response, @Validated CangoRequestDto cangoRequestDto, BindingResult result) {
        CangoResponseDto responseDto = null;
        if (result.hasErrors()) {
            responseDto = buildFailedResponse(result);
        } else {
            responseDto = cangoManagerService.start(cangoRequestDto);
        }
        HttpServletResponseUtil.response(response, responseDto);
    }

    @RequestMapping("stop")
    public void stop(HttpServletResponse response, @Validated CangoRequestDto cangoRequestDto, BindingResult result) {
        CangoResponseDto responseDto = null;
        if (result.hasErrors()) {
            responseDto = buildFailedResponse(result);
        } else {
            responseDto = cangoManagerService.stop(cangoRequestDto);
        }
        HttpServletResponseUtil.response(response, responseDto);
    }

    @RequestMapping("shutdown")
    public void shutdown(HttpServletResponse response) {
        CangoResponseDto responseDto = cangoManagerService.shutdown();
        HttpServletResponseUtil.response(response, responseDto);
    }

    @RequestMapping("startAll")
    public void startAll(HttpServletResponse response) {
        CangoResponseDto responseDto = cangoManagerService.startAll();
        HttpServletResponseUtil.response(response, responseDto);
    }

    @RequestMapping("stopAll")
    public void stopAll(HttpServletResponse response) {
        CangoResponseDto responseDto = cangoManagerService.stopAll();
        HttpServletResponseUtil.response(response, responseDto);
    }

    @RequestMapping("clearAll")
    public void clearAll(HttpServletResponse response) {
        CangoResponseDto responseDto = cangoManagerService.clearAll();
        HttpServletResponseUtil.response(response, responseDto);
    }

    @RequestMapping("enable")
    public void enable(HttpServletResponse response, @Validated CangoRequestDto cangoRequestDto, BindingResult result) {
        CangoResponseDto responseDto = null;
        if (result.hasErrors()) {
            responseDto = buildFailedResponse(result);
        } else {
            responseDto = cangoManagerService.enable(cangoRequestDto);
        }
        HttpServletResponseUtil.response(response, responseDto);
    }

    @RequestMapping("disable")
    public void disable(HttpServletResponse response, @Validated CangoRequestDto cangoRequestDto, BindingResult result) {
        CangoResponseDto responseDto = null;
        if (result.hasErrors()) {
            responseDto = buildFailedResponse(result);
        } else {
            responseDto = cangoManagerService.disable(cangoRequestDto);
        }
        HttpServletResponseUtil.response(response, responseDto);
    }

    private CangoResponseDto buildFailedResponse(BindingResult result) {
        StringBuilder stringBuilder = new StringBuilder();
        List<FieldError> list = result.getFieldErrors();
        for (FieldError fieldError : list) {
            stringBuilder.append(fieldError.getField())
                    .append(" : ")
                    .append(fieldError.getRejectedValue())
                    .append("(")
                    .append(fieldError.getDefaultMessage())
                    .append(")")
                    .append(", ");
        }

        String failMessage = stringBuilder.deleteCharAt(stringBuilder.length() - 2).toString();
        CangoResponseDto cangoResponseDto = new CangoResponseDto();
        cangoResponseDto.setStatus(CangoRspStatus.FAILURE)
                .setFailMsg(failMessage);
        return cangoResponseDto;
    }
}
