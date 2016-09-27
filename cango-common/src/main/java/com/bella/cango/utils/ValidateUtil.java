package com.bella.cango.utils;

import com.bella.cango.exception.CangoException;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/9/24
 */
public class ValidateUtil {
    private static Validator validator;
    static {
        final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }
    public static <T> void validate(T t) {
        final Set<ConstraintViolation<T>> constraintViolations = validator.validate(t);
        if (constraintViolations != null && constraintViolations.size() > 0) {
            StringBuilder stringBuilder = new StringBuilder();
            for (ConstraintViolation<T> constraintViolation : constraintViolations) {
                stringBuilder.append(constraintViolation.getPropertyPath())
                        .append(" : ")
                        .append(constraintViolation.getMessage())
                        .append(", ");
            }
            String constraintMessage = stringBuilder.deleteCharAt(stringBuilder.length() - 1).toString();
            throw new CangoException(constraintMessage);
        }
    }
}
