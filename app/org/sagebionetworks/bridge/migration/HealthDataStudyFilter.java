package org.sagebionetworks.bridge.migration;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sagebionetworks.bridge.models.healthdata.HealthDataRecord;

class HealthDataStudyFilter implements HealthDataFilter {

    private final String studyId;

    HealthDataStudyFilter(final String studyId) {
        checkNotNull(studyId);
        this.studyId = studyId;
    }

    @Override
    public boolean keep(HealthDataRecord record) {
        if (record != null && this.studyId.equals(record.getStudyId())) {
            return true;
        }
        return false;
    }
}
