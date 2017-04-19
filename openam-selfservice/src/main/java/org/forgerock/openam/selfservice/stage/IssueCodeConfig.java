package org.forgerock.openam.selfservice.stage;

import org.forgerock.selfservice.core.config.StageConfig;

import java.util.Objects;

public class IssueCodeConfig implements StageConfig {
    public static final String NAME = "issueCode";

    public String getName() {
        return NAME;
    }

    public String getProgressStageClassName() {
        return IssueCodeStage.class.getName();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof IssueCodeConfig)) {
            return false;
        } else {
            IssueCodeConfig that = (IssueCodeConfig) o;
            return Objects.equals(this.getName(), that.getName()) && Objects.equals(this.getProgressStageClassName(), that.getProgressStageClassName());
        }
    }

    public int hashCode() {
        return Objects.hash(this.getName(), this.getProgressStageClassName());
    }
}
