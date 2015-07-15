package org.sagebionetworks.bridge.migration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.sagebionetworks.bridge.config.BridgeConfigFactory;
import org.sagebionetworks.bridge.config.Environment;
import org.sagebionetworks.bridge.s3.S3Helper;
import org.sagebionetworks.bridge.services.UploadArchiveService;
import org.springframework.context.support.AbstractApplicationContext;

class UploadDumper {

    public static void main(String[] args) throws IOException {
        final UploadDumper dumper = new UploadDumper(Environment.UAT, "heroku", "cardiovascular");
        final Path path = Paths.get("upload-list-uat");
        final List<String> uploadList = Files.readAllLines(path);
        dumper.download(uploadList, "upload-uat");
    }

    private final AbstractApplicationContext appContext;
    private final String studyId;

    UploadDumper(final Environment env, final String user, final String studyId) {
        Utils.checkEnvironmentUser(env, user);
        appContext = Utils.loadAppContext();
        Utils.checkStudy(appContext, studyId);
        this.studyId = studyId;
    }

    /**
     * Downloads from S3, decrypts it, and saves it.
     */
    void download(final List<String> uploadList, final String outputFolder) throws IOException {
        Path path = Paths.get(outputFolder);
        if (Files.exists(path)) {
            throw new RuntimeException(outputFolder + " already exists.");
        }
        Files.createDirectory(path);
        final S3Helper s3Helper = appContext.getBean("s3Helper", S3Helper.class);
        final String bucket = BridgeConfigFactory.getConfig().getProperty("upload.bucket");
        System.out.println("S3 bucket is " + bucket);
        final UploadArchiveService uaService = appContext.getBean(UploadArchiveService.class);
        for (final String uploadId : uploadList) {
            final byte[] bytes = s3Helper.readS3FileAsBytes(bucket, uploadId);
            final byte[] decrypted = uaService.decrypt(studyId, bytes);
            final Path filePath = Paths.get(outputFolder, uploadId);
            Files.write(filePath, decrypted);
        }
    }

    void upload(final String folder) throws IOException {
        final S3Helper s3Helper = appContext.getBean("s3Helper", S3Helper.class);
        final String bucket = BridgeConfigFactory.getConfig().getProperty("upload.bucket");
        System.out.println("S3 bucket is " + bucket);
        final UploadArchiveService uaService = appContext.getBean(UploadArchiveService.class);
        final Collection<File> fileList = FileUtils.listFiles(new File(folder), new String[]{}, false);
        for (final File file : fileList) {
            if (file.isFile() && !file.isHidden()) {
                final byte[] bytes = Files.readAllBytes(file.toPath());
                final byte[] encrypted = uaService.encrypt(studyId, bytes);
                s3Helper.writeBytesToS3(bucket, file.getName(), encrypted);
            }
        }
    }
}
