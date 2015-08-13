package org.openl.extension.xmlrules.model;

import java.util.List;

import org.openl.extension.xmlrules.model.single.FunctionExpressionImpl;
import org.openl.extension.xmlrules.model.single.ParameterImpl;

public interface Function {
    String getName();

    List<ParameterImpl> getParameters();

    String getReturnType();

    String getCellAddress();
//    List<FunctionExpressionImpl> getExpressions();
}
