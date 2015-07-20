package org.sagebionetworks.bridge.migration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.sagebionetworks.bridge.config.Environment;
import org.sagebionetworks.bridge.dao.UploadDao;
import org.sagebionetworks.bridge.models.studies.StudyIdentifier;
import org.sagebionetworks.bridge.models.studies.StudyIdentifierImpl;
import org.sagebionetworks.bridge.services.UploadValidationService;
import org.springframework.context.support.AbstractApplicationContext;

class UploadValidator {

    public static void main(String[] args) throws IOException, InterruptedException {
        final UploadValidator validator = new UploadValidator(Environment.PROD, "heroku", "cardiovascular");
        final List<String> uploads = Files.readAllLines(Paths.get("02-upload-list"));
        validator.validate(uploads);
    }

    private final AbstractApplicationContext appContext;
    private final String studyId;

    UploadValidator(final Environment env, final String user, final String studyId) {
        Utils.checkEnvironmentUser(env, user);
        appContext = Utils.loadAppContext();
        Utils.checkStudy(appContext, studyId);
        this.studyId = studyId;
    }

    void validate(final List<String> uploads) throws InterruptedException {
        final UploadDao uploadDao = appContext.getBean(UploadDao.class);
        final UploadValidationService validator = appContext.getBean(UploadValidationService.class);
        final StudyIdentifier study = new StudyIdentifierImpl(studyId);
        int i = 0;
        for (final String uploadId : uploads) {
            validator.validateUpload(study, uploadDao.getUpload(uploadId));
            System.out.println("Validation started for " + uploadId + " " + i + " out of " + uploads.size());
            Thread.sleep(1000);
        }
    }
}
