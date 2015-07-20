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
import java.util.List;

import org.sagebionetworks.bridge.config.Environment;
import org.sagebionetworks.bridge.dynamodb.DynamoUploadDao;
import org.sagebionetworks.bridge.models.upload.Upload;
import org.springframework.context.support.AbstractApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

class UploadTableDumper {

    public static void main(String[] args) throws IOException {
        final List<String> uploadList = Files.readAllLines(Paths.get("upload-list-uat"));
        final UploadTableDumper dumper = new UploadTableDumper(Environment.UAT, "heroku");
        dumper.download(uploadList, "upload-data-staging");
    }

    private final AbstractApplicationContext appContext;

    UploadTableDumper(final Environment env, final String user) {
        Utils.checkEnvironmentUser(env, user);
        appContext = Utils.loadAppContext();
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
                final Upload uploadObj = uploadDao.getUpload(upload);
                writer.write(mapper.writeValueAsString(uploadObj));
                writer.write("\r\n");
            }
        }
    }
}
