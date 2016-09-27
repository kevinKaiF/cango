package com.bella.cango.service;

import com.bella.cango.dto.CangoRequestDto;
import com.bella.cango.dto.CangoResponseDto;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/8/7
 */
public interface CangoManagerService {

    CangoResponseDto start(CangoRequestDto cangoRequestDto);

    CangoResponseDto add(CangoRequestDto cangoRequestDto);

    CangoResponseDto stop(CangoRequestDto cangoRequestDto);

    CangoResponseDto enable(CangoRequestDto cangoRequestDto);

    CangoResponseDto disable(CangoRequestDto cangoRequestDto);

    CangoResponseDto check(CangoRequestDto canalReqDto);

    CangoResponseDto startAll();

    CangoResponseDto stopAll();

    CangoResponseDto shutdown();

    CangoResponseDto clearAll();
}
