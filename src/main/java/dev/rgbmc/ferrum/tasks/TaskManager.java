package dev.rgbmc.ferrum.tasks;

import dev.rgbmc.expression.FastExpression;
import dev.rgbmc.expression.parameters.StringParameter;
import dev.rgbmc.ferrum.api.Backup;
import dev.rgbmc.ferrum.api.objects.ResultInfo;

import java.util.List;

public class TaskManager {
    private final FastExpression fastExpression;

    public TaskManager() {
        fastExpression = new FastExpression();
        fastExpression.getFunctionManager().register(new CLITask());
        fastExpression.getFunctionManager().register(new FtpTask());
    }

    public FastExpression getFastExpression() {
        return fastExpression;
    }

    public void parseExpressions(List<String> expressions, ResultInfo resultInfo, Backup backup) {
        expressions.forEach(expression -> fastExpression.getFunctionManager().parseExpression(expression).forEach(callableFunction -> {
            callableFunction.setParameter(new TaskParameter(((StringParameter) callableFunction.getParameter()).getString(), resultInfo, backup));
            callableFunction.callFunction();
        }));
    }
}
