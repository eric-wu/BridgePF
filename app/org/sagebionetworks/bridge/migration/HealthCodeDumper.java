package org.sagebionetworks.bridge.migration;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.sagebionetworks.bridge.config.Environment;
import org.sagebionetworks.bridge.dao.HealthIdDao;
import org.springframework.context.support.AbstractApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

class HealthCodeDumper {

    public static void main(String[] args) throws IOException {
        final HealthCodeDumper dumper = new HealthCodeDumper(Environment.PROD, "heroku");
        dumper.dump("health-id-prod", "health-id2code-prod");
    }

    private final AbstractApplicationContext appContext;

    HealthCodeDumper(final Environment env, final String user) {
        Utils.checkEnvironmentUser(env, user);
        appContext = Utils.loadAppContext();
    }

    void dump(final String inputFile, final String outputFile) throws IOException {
        final Path input = Paths.get(inputFile);
        if (!Files.exists(input)) {
            throw new RuntimeException(inputFile + " does not exist.");
        }
        final Path output = Paths.get(outputFile);
        if (Files.exists(output)) {
            throw new RuntimeException(outputFile + " already exists.");
        }
        Files.createFile(output);
        try (final FileOutputStream fos = new FileOutputStream(output.toFile());
                final OutputStreamWriter osw = new OutputStreamWriter(fos);
                final Writer writer = new BufferedWriter(osw)) {
            final HealthIdDao hiDao = appContext.getBean(HealthIdDao.class);
            final ObjectMapper mapper = new ObjectMapper();
            final List<String> lines = Files.readAllLines(input);
            for (String line : lines) {
                final String hc = hiDao.getCode(line);
                if (hc != null) {
                    final ObjectNode node = mapper.getNodeFactory().objectNode();
                    node.put("hid", line);
                    node.put("hc", hc);
                    writer.write(mapper.writeValueAsString(node));
                    writer.write("\r\n");
                }
            }
        }
    }
}
