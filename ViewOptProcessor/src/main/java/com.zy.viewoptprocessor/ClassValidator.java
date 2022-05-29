package com.zy.viewoptprocessor;

import javax.lang.model.element.TypeElement;

public final class ClassValidator {
    static String getClassName(TypeElement type, String packageName) {
        int packageLength = packageName.length() + 1;
        return type.getQualifiedName().toString().substring(packageLength).replace(".", "$");
    }
}
