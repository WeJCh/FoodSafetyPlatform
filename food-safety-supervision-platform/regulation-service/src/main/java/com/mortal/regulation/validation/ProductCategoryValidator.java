package com.mortal.regulation.validation;

import com.mortal.regulation.common.constants.ProductCategoryCatalog;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

public class ProductCategoryValidator implements ConstraintValidator<ValidProductCategory, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        return ProductCategoryCatalog.isSupported(value);
    }
}
