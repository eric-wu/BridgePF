package org.sagebionetworks.bridge.migration;

public interface InputData {

    boolean hasNext();

    DataRecord read();
}
