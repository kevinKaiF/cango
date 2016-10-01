package com.bella.cango.validate;

import com.bella.cango.dto.CangoRequestDto;
import com.bella.cango.enums.DbType;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/10/1
 */
public class SlaveIdValidator implements ConstraintValidator<SlaveId, CangoRequestDto> {
    @Override
    public void initialize(SlaveId slaveId) {

    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isValid(CangoRequestDto cangoRequestDto, ConstraintValidatorContext constraintValidatorContext) {
        if (DbType.MYSQL.equals(cangoRequestDto.getDbType())) {
            if (cangoRequestDto.getSlaveId() == null || cangoRequestDto.getSlaveId() < 0) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext.buildConstraintViolationWithTemplate("不能为空")
                        .addNode("slaveId").addConstraintViolation();
                return false;
            }
        }

        return true;
    }
}
