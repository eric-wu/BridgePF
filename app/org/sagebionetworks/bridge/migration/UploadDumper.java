package org.sagebionetworks.bridge.migration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.sagebionetworks.bridge.config.BridgeConfigFactory;
import org.sagebionetworks.bridge.config.Environment;
import org.sagebionetworks.bridge.s3.S3Helper;
import org.sagebionetworks.bridge.services.UploadArchiveService;
import org.springframework.context.support.AbstractApplicationContext;

class UploadDumper {

    public static void main(String[] args) throws IOException, InterruptedException {
        // *** Upload ***
        final UploadDumper dumper = new UploadDumper(Environment.PROD, "heroku", "cardiovascular");
        dumper.encryptUpload("02-uat-decrypted", true);
//        // *** Decrypt ***
//        final UploadDumper dumper = new UploadDumper(Environment.UAT, "heroku", "cardiovascular");
//        dumper.decrypt("02-prod-downloads", "02-uat-decrypted");
//        // *** Parallel download from s3 ***
//        final List<String> downloads = Files.readAllLines(Paths.get("02-upload-list"));
//        final UploadDumper dumper = new UploadDumper(Environment.PROD, "heroku", "cardiovascular");
//        List<Runnable> downloadJobs = createDownloadJobs(downloads, "02-prod-downloads", dumper, 10);
//        final ExecutorService threadPool = Executors.newFixedThreadPool(10);
//        for (Runnable job : downloadJobs) {
//            threadPool.execute(job);
//        }
//        threadPool.awaitTermination(1, TimeUnit.HOURS);
    }

    static List<Runnable> createDownloadJobs(final List<String> downloads, final String downloadFolder,
            final UploadDumper dumper, final int threadPoolSize) throws IOException {
        final int chunkSize = downloads.size() / (threadPoolSize - 1);
        final List<Runnable> jobs = new ArrayList<>();
        for (int i = 0; i < threadPoolSize; i++) {
            final int from = i * chunkSize;
            final int to = (from + chunkSize) < downloads.size() ? from + chunkSize : downloads.size();
            final List<String> subList = downloads.subList(from, to);
            jobs.add(new Runnable() {
                @Override
                public void run() {
                    try {
                        dumper.download(subList, downloadFolder);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        return jobs;
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
     * Downloads from S3.
     */
    void download(final List<String> uploadList, final String outputFolder) throws IOException {
        final Path path = Paths.get(outputFolder);
        if (!Files.exists(path)) {
            throw new RuntimeException(outputFolder + " does not exists.");
        }
        final S3Helper s3Helper = appContext.getBean("s3Helper", S3Helper.class);
        final String bucket = BridgeConfigFactory.getConfig().getProperty("upload.bucket");
        System.out.println("S3 bucket is " + bucket);
        int i = 0;
        for (final String uploadId : uploadList) {
            
            final byte[] bytes = s3Helper.readS3FileAsBytes(bucket, uploadId);
            final Path filePath = Paths.get(outputFolder, uploadId);
            Files.write(filePath, bytes);
            i++;
            System.out.println("Thread " + Thread.currentThread().getId() +
                    " has downloaded " + i + " out of " + uploadList.size());
        }
    }

    void decrypt(final String inputFolder, final String outputFolder) throws IOException {
        final Path inputPath = Paths.get(inputFolder);
        if (!Files.exists(inputPath)) {
            throw new RuntimeException(inputFolder + " does not exists.");
        }
        final Path outputPath = Paths.get(outputFolder);
        if (!Files.exists(outputPath)) {
            throw new RuntimeException(outputFolder + " does not exists.");
        }
        final UploadArchiveService uaService = appContext.getBean(UploadArchiveService.class);
        final File[] inputFiles = inputPath.toFile().listFiles();
        int i = 0;
        for (final File inputFile : inputFiles) {
            final byte[] encrypted = Files.readAllBytes(inputFile.toPath());
            try {
                final byte[] decrypted = uaService.decrypt(studyId, encrypted);
                final Path outputFile = Paths.get(outputFolder, inputFile.getName());
                Files.write(outputFile, decrypted);
                i++;
                System.out.println("Decrypted " + i + " out of " + inputFiles.length);
            } catch (Exception e) {
                System.out.println("Failed to decrypt " + inputFile.getName());
            }
        }
    }

    /**
     * Downloads from S3, decrypts it, and saves it.
     */
    void downloadDecrypt(final List<String> uploadList, final String outputFolder) throws IOException {
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

    /**
     * Encrypts then uploads to S3.
     */
    void encryptUpload(final String folder, final boolean overwrite) throws IOException {
        final File path = new File(folder);
        if (!path.exists()) {
            throw new RuntimeException(folder + " does not exists.");
        }
        final String bucket = BridgeConfigFactory.getConfig().getProperty("upload.bucket");
        System.out.println("S3 bucket is " + bucket);
        final S3Helper s3Helper = appContext.getBean("s3Helper", S3Helper.class);
        final UploadArchiveService uaService = appContext.getBean(UploadArchiveService.class);
        final File[] files = path.listFiles();
        int i = 0;
        for (final File file : files) {
            if (file.isFile() && !file.isHidden()) {
                final byte[] bytes = Files.readAllBytes(file.toPath());
                final byte[] encrypted = uaService.encrypt(studyId, bytes);
                final String fileName = file.getName();
                if (!overwrite && s3Helper.exists(bucket, fileName)) {
                    System.out.println(fileName + " exists and is skipped.");
                } else {
                    s3Helper.writeBytesToS3(bucket, fileName, encrypted);
                    i++;
                    System.out.println("Uploaded file " + fileName + "  " + i + " out of " + files.length);
                }
            }
        }
    }
}
