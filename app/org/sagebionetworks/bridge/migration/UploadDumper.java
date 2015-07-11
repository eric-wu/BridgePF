package org.sagebionetworks.bridge.migration;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.sagebionetworks.bridge.config.Environment;

class UploadDumper {

    UploadDumper(final Environment env, final String user) {
        checkNotNull(env);
        checkNotNull(user);
        Utils.checkEnvironment(env, user);
    }

    void download(final List<String> uploadList, final String outputFolder) {
        // TODO: To be implemented -- download, decrypt, save
    }

    void upload(final String folder) {
        // TODO: To be implemented -- encrypt, upload
    }
}
