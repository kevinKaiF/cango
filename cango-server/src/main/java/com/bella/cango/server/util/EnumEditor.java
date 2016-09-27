package com.bella.cango.server.util;

import java.beans.PropertyEditorSupport;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/9/27
 */
public class EnumEditor<T extends Enum<T>> extends PropertyEditorSupport {

    private Class<T> clazz;

    public EnumEditor(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        this.setValue(T.valueOf(clazz, text));
    }
}
