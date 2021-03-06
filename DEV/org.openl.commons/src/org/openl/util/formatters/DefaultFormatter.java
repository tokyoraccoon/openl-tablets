package org.openl.util.formatters;

public class DefaultFormatter implements IFormatter {

    public DefaultFormatter() {
    }

    @Override
    public String format(Object obj) {
        return obj == null ? null : obj.toString();
    }

    @Override
    public Object parse(String value) {
        return value;
    }

}
