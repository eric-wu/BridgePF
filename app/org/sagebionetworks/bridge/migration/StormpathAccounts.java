package org.sagebionetworks.bridge.migration;

import java.util.Iterator;

import javax.inject.Inject;

import org.sagebionetworks.bridge.models.accounts.Account;
import org.sagebionetworks.bridge.stormpath.StormpathAccountDao;

public class StormpathAccounts implements InputData {

    private final Iterator<Account> iterator;

    @Inject
    public StormpathAccounts(final StormpathAccountDao stormpathAccountDao) {
        iterator = stormpathAccountDao.getAllAccounts();
    }

    @Override
    public DataRecord read() {
        return new DataRecord() {
            final Account account = iterator.next();
            String getId() {
                return account.getId();
            }
            String getEmail() {
                return account.getEmail();
            }
            String getHealthId() {
                return account.getHealthId();
            }
            String getUsername() {
                return account.getUsername();
            }
            String getStudy() {
                return account.getStudyIdentifier().getIdentifier();
            }
        };
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }
}
