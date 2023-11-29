package dev.rgbmc.ferrum.tasks;

import dev.rgbmc.expression.functions.FastFunction;
import dev.rgbmc.expression.functions.FunctionParameter;
import dev.rgbmc.expression.functions.FunctionResult;

import java.io.IOException;

public class CLITask implements FastFunction {
    @Override
    public FunctionResult call(FunctionParameter parameter) {
        TaskParameter taskParameter = (TaskParameter) parameter;
        try {
            Runtime.getRuntime().exec(taskParameter.getString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public String getName() {
        return "cli";
    }
}
