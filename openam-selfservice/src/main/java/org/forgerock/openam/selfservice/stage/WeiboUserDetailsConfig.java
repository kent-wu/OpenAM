package org.forgerock.openam.selfservice.stage;

import org.forgerock.selfservice.core.config.StageConfig;

import java.util.Objects;

public class WeiboUserDetailsConfig implements StageConfig {
    public static final String NAME = "weiboRegistration";

    public String getName() {
        return "weiboRegistration";
    }

    public String getProgressStageClassName() {
        return WeiboUserDetailsStage.class.getName();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof WeiboUserDetailsConfig)) {
            return false;
        } else {
            WeiboUserDetailsConfig that = (WeiboUserDetailsConfig) o;
            return Objects.equals(this.getName(), that.getName()) && Objects.equals(this.getProgressStageClassName(), that.getProgressStageClassName());
        }
    }

    public int hashCode() {
        return Objects.hash(this.getName(), this.getProgressStageClassName());
    }
}
