package org.sagebionetworks.bridge.migration;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;
import org.sagebionetworks.bridge.config.Environment;
import org.sagebionetworks.bridge.dynamodb.DynamoUpload2;
import org.sagebionetworks.bridge.dynamodb.DynamoUploadDao;
import org.sagebionetworks.bridge.models.studies.StudyIdentifier;
import org.sagebionetworks.bridge.models.studies.StudyIdentifierImpl;
import org.sagebionetworks.bridge.models.upload.Upload;
import org.sagebionetworks.bridge.models.upload.UploadStatus;
import org.sagebionetworks.bridge.services.UploadValidationService;
import org.springframework.context.support.AbstractApplicationContext;

import akka.util.Collections;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class UploadTableDumper {

    public static void main(String[] args) throws IOException, InterruptedException {
        final UploadTableDumper dumper = new UploadTableDumper(Environment.PROD, "heroku", "cardiovascular");
        dumper.upload("upload-data-staging-edited");
//        final List<String> uploadList = Files.readAllLines(Paths.get("upload-list-uat"));
//        final UploadTableDumper dumper = new UploadTableDumper(Environment.UAT, "heroku");
//        dumper.download(uploadList, "upload-data-staging");
    }

    private final AbstractApplicationContext appContext;
    private final String studyId;

    UploadTableDumper(final Environment env, final String user, final String studyId) {
        Utils.checkEnvironmentUser(env, user);
        appContext = Utils.loadAppContext();
        Utils.checkStudy(appContext, studyId);
        this.studyId = studyId;
    }

    void download(final List<String> uploadList, final String outputFile) throws FileNotFoundException, IOException {

        final Path file = Paths.get(outputFile);
        if (Files.exists(file)) {
            throw new RuntimeException("File " + outputFile + " already exists.");
        }

        try (final FileOutputStream fos = new FileOutputStream(Files.createFile(file).toFile());
                final OutputStreamWriter osw = new OutputStreamWriter(fos);
                final Writer writer = new BufferedWriter(osw)) {
            final ObjectMapper mapper = new ObjectMapper();
            final DynamoUploadDao uploadDao = appContext.getBean(DynamoUploadDao.class);
            for (final String upload : uploadList) {
                final DynamoUpload2 uploadObj = (DynamoUpload2)uploadDao.getUpload(upload);
                writer.write(mapper.writeValueAsString(uploadObj));
                writer.write("\r\n");
            }
        }
    }

    void upload(final String inputfile) throws IOException, InterruptedException {
        final Path input = Paths.get(inputfile);
        if (!Files.exists(input)) {
            throw new RuntimeException("File " + input + " does not exist.");
        }
        final ObjectMapper jsonMapper = new ObjectMapper();
        final DynamoDBMapper uploadDdbMapper = appContext.getBean("uploadDdbMapper", DynamoDBMapper.class);
        final UploadValidationService validationService = appContext.getBean(UploadValidationService.class);
        final List<String> lines = Files.readAllLines(input);
        int i = 0;
        for (final String line : lines) {
            final JsonNode node = jsonMapper.readTree(line);
            final DynamoUpload2 upload = new DynamoUpload2();
            upload.setContentLength(node.get("contentLength").asLong());
            upload.setContentMd5(node.get("contentMd5").asText());
            upload.setContentType(node.get("contentType").asText());
            upload.setFilename(node.get("filename").asText());
            upload.setHealthCode(node.get("healthCode").asText());
            upload.setStatus(UploadStatus.valueOf(node.get("status").asText()));
            upload.setUploadDate(LocalDate.now());
            upload.setUploadId(node.get("uploadId").asText());
            upload.setValidationMessageList(new ArrayList<String>(0));
            uploadDdbMapper.save(upload);
            validationService.validateUpload(new StudyIdentifierImpl(studyId), upload);
            i++;
            System.out.println("Processed " + i + " out of " + lines.size());
            Thread.sleep(360L);
            
        }
    }
}
