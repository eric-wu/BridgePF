package org.sagebionetworks.bridge.migration;

public interface Code {

    void run(Data input, Data output, Context context);
}
