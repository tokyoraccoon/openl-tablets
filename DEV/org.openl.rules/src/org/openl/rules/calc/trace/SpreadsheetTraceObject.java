package org.openl.rules.calc.trace;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.table.ATableTracerNode;
import org.openl.types.IOpenMethod;

public class SpreadsheetTraceObject extends ATableTracerNode {

    public SpreadsheetTraceObject(Spreadsheet spreadsheet, Object[] params) {
        super("spreadsheet", "SpreadSheet", spreadsheet, params);
    }

    /**
     * Is overriden to provide functionality not to write a result when it is represented as
     * SpreadsheetResult
     */
    @Override
    protected String getFormattedValue(Object value, IOpenMethod method) {
        if (!ClassUtils.isAssignable(method.getType().getInstanceClass(), SpreadsheetResult.class, false)) {
            // Write results when it is not a SpreadsheetResult, as it is very complex
            return super.getFormattedValue(value, method);
        }
        return StringUtils.EMPTY;
    }
}
