package dev.rgbmc.ferrum.tasks;

import dev.rgbmc.expression.parameters.StringParameter;
import dev.rgbmc.ferrum.api.Backup;
import dev.rgbmc.ferrum.api.objects.ResultInfo;

public class TaskParameter extends StringParameter {
    private final ResultInfo resultInfo;
    private final Backup backup;

    public TaskParameter(String parameter, ResultInfo resultInfo, Backup backup) {
        super(parameter);
        this.resultInfo = resultInfo;
        this.backup = backup;
    }

    public Backup getBackup() {
        return backup;
    }

    public ResultInfo getResultInfo() {
        return resultInfo;
    }
}
