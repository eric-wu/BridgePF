package org.sagebionetworks.bridge.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.bridge.BridgeUtils;
import org.sagebionetworks.bridge.TestUserAdminHelper;
import org.sagebionetworks.bridge.config.BridgeConfigFactory;
import org.sagebionetworks.bridge.dao.StudyConsentDao;
import org.sagebionetworks.bridge.dynamodb.DynamoStudy;
import org.sagebionetworks.bridge.models.studies.Study;
import org.sagebionetworks.bridge.models.studies.StudyConsent;
import org.sagebionetworks.bridge.models.studies.StudyConsentForm;
import org.sagebionetworks.bridge.models.studies.StudyConsentView;
import org.sagebionetworks.bridge.models.studies.StudyIdentifier;
import org.sagebionetworks.bridge.models.studies.StudyIdentifierImpl;
import org.sagebionetworks.bridge.s3.S3Helper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration("classpath:test-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class StudyConsentServiceImplTest {
    
    private static final String BUCKET = BridgeConfigFactory.getConfig().getConsentsBucket();
    private static final StudyIdentifier STUDY_ID = new StudyIdentifierImpl("study-key");

    @Resource
    private StudyConsentDao studyConsentDao;

    @Resource
    private TestUserAdminHelper helper;

    @Resource(name = "s3ConsentsHelper")
    private S3Helper s3Helper;

    @Resource
    private StudyConsentService studyConsentService;

    private Study study;
    
    @Before
    public void before() {
        study = new DynamoStudy();
        study.setIdentifier(STUDY_ID.getIdentifier());
        study.setName("A test study");
        study.setSponsorName("A sponsor name");
    }
    
    @After
    public void after() {
        studyConsentDao.deleteAllConsents(STUDY_ID);
    }

    @Test
    public void crudStudyConsent() {
        String documentContent = "<p>This is a consent document.</p><p>This is the second paragraph of same.</p>";
        StudyConsentForm form = new StudyConsentForm(documentContent);

        // addConsent should return a non-null consent object.
        StudyConsentView addedConsent1 = studyConsentService.addConsent(STUDY_ID, form);
        assertNotNull(addedConsent1);

        try {
            studyConsentService.getActiveConsent(STUDY_ID);
            fail("getActiveConsent should throw exception, as there is no currently active consent.");
        } catch (Exception e) {
        }

        // Get active consent returns the most recently activated consent document.
        StudyConsentView activatedConsent = studyConsentService.publishConsent(study, addedConsent1.getCreatedOn());
        StudyConsentView getActiveConsent = studyConsentService.getActiveConsent(study);
        assertTrue(activatedConsent.getCreatedOn() == getActiveConsent.getCreatedOn());
        
        // This is "fixed" by the XML and sanitizing parse that happens. It's fine.
        assertEquals("<p>This is a consent document.</p>\n<p>This is the second paragraph of same.</p>", getActiveConsent.getDocumentContent());
        assertNotNull(getActiveConsent.getStudyConsent().getStoragePath());
        
        // Get all consents returns one consent document (addedConsent).
        List<StudyConsent> allConsents = studyConsentService.getAllConsents(STUDY_ID);
        assertTrue(allConsents.size() == 1);
    }
    
    @Test
    public void studyConsentWithFileAndS3ContentTakesS3Content() throws Exception {
        DateTime createdOn = DateTime.now();
        String key = STUDY_ID.getIdentifier() + "." + createdOn.getMillis();
        s3Helper.writeBytesToS3(BUCKET, key, "<document/>".getBytes());
        
        StudyConsent consent = studyConsentDao.addConsent(STUDY_ID, key, createdOn);
        studyConsentDao.publish(consent);
        // The junk path should not prevent the service from getting the S3 content.
        // We actually wouldn't get here if it tried to load from disk with the path we've provided.
        StudyConsentView view = studyConsentService.getConsent(STUDY_ID, createdOn.getMillis());
        assertEquals("<document/>", view.getDocumentContent());
    }
    
    @Test
    public void invalidMarkupIsFixed() {
        StudyConsentForm form = new StudyConsentForm("<cml><p>This is not valid XML.</cml>");
        StudyConsentView view = studyConsentService.addConsent(new StudyIdentifierImpl("api"), form);
        assertEquals("<p>This is not valid XML.</p>", view.getDocumentContent());
    }
    
    @Test
    public void fullDocumentsAreConvertedToFragments() {
        String doc = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title></title></head><body><p>This is all the content that should be kept.</p><br><p>And this makes it a fragment.</p></body></html>";
        
        StudyConsentForm form = new StudyConsentForm(doc);
        StudyConsentView view = studyConsentService.addConsent(new StudyIdentifierImpl("api"), form);
        assertEquals("<p>This is all the content that should be kept.</p>\n<br />\n<p>And this makes it a fragment.</p>", view.getDocumentContent());
    }
    
    /**
     * There used to be a test that an InvalidEntityException would be thrown if the content was not valid XML. But
     * Jsoup is very dogged in fixing even the worst documents, as this test demonstrates. Consenquently the validator 
     * just isn't throwing an exception when testing through the service.
     */
    @Test
    public void evenVeryBrokenContentIsFixed() {
        StudyConsentForm form = new StudyConsentForm("</script><div ankle='foo'>This just isn't a SGML-based document no matter how you slice it.</p><h4><img>");
        StudyConsentView view = studyConsentService.addConsent(new StudyIdentifierImpl("api"), form);
        assertEquals("<div>\n This just isn't a SGML-based document no matter how you slice it.\n <p></p>\n <h4><img /></h4>\n</div>", view.getDocumentContent());
    }
    
    @Test
    public void publishingConsentCreatesPublicBucketDocuments() throws IOException {
        String content = "<p>"+BridgeUtils.generateGuid()+"</p>";

        StudyConsentForm form = new StudyConsentForm(content);
        StudyConsentView view = studyConsentService.addConsent(study.getStudyIdentifier(), form);
        studyConsentService.publishConsent(study, view.getCreatedOn());

        // Now retrieve the HTML version of the document and verify it has been updated.
        // Removing SSL because IOUtils doesn't support it and although we do it, we don't need to.
        String htmlURL = study.getConsentHTML();
        
        String retrievedContent = IOUtils.toString(new URL(htmlURL).openStream(), Charset.forName("UTF-8"));
        assertTrue(retrievedContent.contains(content));
    }
    
}