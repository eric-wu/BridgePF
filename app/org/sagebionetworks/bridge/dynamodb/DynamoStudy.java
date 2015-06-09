package org.sagebionetworks.bridge.dynamodb;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.sagebionetworks.bridge.exceptions.BridgeServiceException;
import org.sagebionetworks.bridge.json.BridgeObjectMapper;
import org.sagebionetworks.bridge.json.BridgeTypeName;
import org.sagebionetworks.bridge.json.JsonUtils;
import org.sagebionetworks.bridge.models.studies.EmailTemplate;
import org.sagebionetworks.bridge.models.studies.PasswordPolicy;
import org.sagebionetworks.bridge.models.studies.Study;
import org.sagebionetworks.bridge.models.studies.StudyIdentifier;
import org.sagebionetworks.bridge.models.studies.StudyIdentifierImpl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMarshalling;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBVersionAttribute;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

@DynamoDBTable(tableName = "Study")
@BridgeTypeName("Study")
@JsonFilter("filter") 
public class DynamoStudy implements Study {
    
    private static ObjectMapper mapper = new ObjectMapper();

    private static final String RESEARCHER_ROLE_PROPERTY = "researcherRole";
    private static final String STORMPATH_HREF_PROPERTY = "stormpathHref";

    private static final String MAX_NUM_OF_PARTICIPANTS_PROPERTY = "maxNumOfParticipants";
    private static final String MIN_AGE_OF_CONSENT_PROPERTY = "minAgeOfConsent";
    private static final String SUPPORT_EMAIL_PROPERTY = "supportEmail";
    private static final String CONSENT_NOTIFICATION_EMAIL_PROPERTY = "consentNotificationEmail";
    private static final String USER_PROFILE_ATTRIBUTES_PROPERTY = "userProfileAttributes";
    private static final String PASSWORD_POLICY_PROPERTY = "passwordPolicy";
    private static final String VERIFY_EMAIL_TEMPLATE_PROPERTY = "verifyEmailTemplate";
    private static final String RESET_PASSWORD_TEMPLATE_PROPERTY = "resetPasswordTemplate";
    private static final String ACTIVE_PROPERTY = "active";
    private static final String TECHNICAL_EMAIL_PROPERTY = "technicalEmail";
    private static final String SPONSOR_NAME_PROPERTY = "sponsorName";

    private static final FilterProvider RESEARCHER_VIEW_FILTER = new SimpleFilterProvider()
        .addFilter("filter", SimpleBeanPropertyFilter.serializeAllExcept(STORMPATH_HREF_PROPERTY, RESEARCHER_ROLE_PROPERTY));
    
    public static final ObjectWriter STUDY_WRITER = new BridgeObjectMapper().writer(
        DynamoStudy.RESEARCHER_VIEW_FILTER);

    private String name;
    private String sponsorName;
    private String identifier;
    private String researcherRole;
    private String stormpathHref;
    private String supportEmail;
    private String technicalEmail;
    private String consentNotificationEmail;
    private int minAgeOfConsent;
    private int maxNumOfParticipants;
    private Long version;
    private boolean active;
    private StudyIdentifier studyIdentifier;
    private Set<String> profileAttributes;
    private PasswordPolicy passwordPolicy;
    private EmailTemplate verifyEmailTemplate;
    private EmailTemplate resetPasswordTemplate;

    public DynamoStudy() {
        profileAttributes = new HashSet<>();
    }
    
    /** {@inheritDoc} */
    @Override
    @DynamoDBAttribute
    public String getSponsorName() {
        return sponsorName;
    }
    @Override
    public void setSponsorName(String sponsorName) {
        this.sponsorName = sponsorName;
    }
    /** {@inheritDoc} */
    @Override
    @DynamoDBAttribute
    public String getName() {
        return name;
    }
    @Override
    public void setName(String name) {
        this.name = name;
    }
    /** {@inheritDoc} */
    @Override
    @DynamoDBHashKey
    public String getIdentifier() {
        return identifier;
    }
    @Override
    public void setIdentifier(String identifier) {
        if (identifier != null) {
            this.identifier = identifier;
            this.studyIdentifier = new StudyIdentifierImpl(identifier);
        }
    }
    /** {@inheritDoc} */
    @Override
    @JsonIgnore
    @DynamoDBIgnore
    public StudyIdentifier getStudyIdentifier() {
        return studyIdentifier;
    }
    /** {@inheritDoc} */
    @Override
    @DynamoDBVersionAttribute
    public Long getVersion() {
        return version;
    }
    public void setVersion(Long version) {
        this.version = version;
    }
    /** {@inheritDoc} */
    @Override
    public String getResearcherRole() {
        return researcherRole;
    }
    @Override
    public void setResearcherRole(String role) {
        this.researcherRole = role;
    }
    /** {@inheritDoc} */
    @Override
    public int getMinAgeOfConsent() {
        return minAgeOfConsent;
    }
    @Override
    public void setMinAgeOfConsent(int minAge) {
        this.minAgeOfConsent = minAge;
    }
    /** {@inheritDoc} */
    @Override
    public int getMaxNumOfParticipants() {
        return maxNumOfParticipants;
    }
    @Override
    public void setMaxNumOfParticipants(int maxParticipants) {
        this.maxNumOfParticipants = maxParticipants;
    }
    /** {@inheritDoc} */
    @Override
    public String getStormpathHref() {
        return stormpathHref;
    }
    @Override
    public void setStormpathHref(String stormpathHref) {
        this.stormpathHref = stormpathHref;
    }
    /** {@inheritDoc} */
    @Override
    public String getSupportEmail() {
        return supportEmail;
    }
    @Override
    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }
    /** {@inheritDoc} */
    @Override
    public String getTechnicalEmail() {
        return technicalEmail;
    }
    @Override
    public void setTechnicalEmail(String technicalEmail) {
        this.technicalEmail = technicalEmail;
    }
    /** {@inheritDoc} */
    @Override
    public String getConsentNotificationEmail() {
        return consentNotificationEmail;
    }
    @Override
    public void setConsentNotificationEmail(String consentNotificationEmail) {
        this.consentNotificationEmail = consentNotificationEmail;
    }
    /** {@inheritDoc} */
    @DynamoDBMarshalling(marshallerClass = StringSetMarshaller.class)
    @Override
    public Set<String> getUserProfileAttributes() {
        return profileAttributes;
    }
    @Override
    public void setUserProfileAttributes(Set<String> profileAttributes) {
        this.profileAttributes = profileAttributes;
    }
    /** {@inheritDoc} */
    @DynamoDBMarshalling(marshallerClass = PasswordPolicyMarshaller.class)
    @Override
    public PasswordPolicy getPasswordPolicy() {
        return passwordPolicy;
    }
    @Override
    public void setPasswordPolicy(PasswordPolicy passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }
    /** {@inheritDoc} */
    @DynamoDBMarshalling(marshallerClass = EmailTemplateMarshaller.class)
    @Override
    public EmailTemplate getVerifyEmailTemplate() {
        return verifyEmailTemplate;
    }
    @Override
    public void setVerifyEmailTemplate(EmailTemplate template) {
        this.verifyEmailTemplate = template;
    }
    /** {@inheritDoc} */
    @DynamoDBMarshalling(marshallerClass = EmailTemplateMarshaller.class)
    @Override
    public EmailTemplate getResetPasswordTemplate() {
        return resetPasswordTemplate;
    }
    @Override
    public void setResetPasswordTemplate(EmailTemplate template) {
        this.resetPasswordTemplate = template;
    }
    /** {@inheritDoc} */
    @Override
    public boolean isActive() {
        return active;
    }
    @Override
    public void setActive(boolean active) {
        this.active = active;
    }
    // Left for legacy support of earlier versions of studies. Will be removed after migration.
    @JsonIgnore
    public String getData() {
        ObjectMapper mapper = BridgeObjectMapper.get();
        
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put(RESEARCHER_ROLE_PROPERTY, researcherRole);
        node.put(MIN_AGE_OF_CONSENT_PROPERTY, minAgeOfConsent);
        node.put(MAX_NUM_OF_PARTICIPANTS_PROPERTY, maxNumOfParticipants);
        node.put(STORMPATH_HREF_PROPERTY, stormpathHref);
        node.put(SUPPORT_EMAIL_PROPERTY, supportEmail);
        node.put(CONSENT_NOTIFICATION_EMAIL_PROPERTY, consentNotificationEmail);
        node.put(SPONSOR_NAME_PROPERTY, sponsorName);
        node.put(TECHNICAL_EMAIL_PROPERTY, technicalEmail);
        node.put(ACTIVE_PROPERTY, active);
        node.putPOJO(PASSWORD_POLICY_PROPERTY, mapper.valueToTree(passwordPolicy));
        node.putPOJO(VERIFY_EMAIL_TEMPLATE_PROPERTY, mapper.valueToTree(verifyEmailTemplate));
        node.putPOJO(RESET_PASSWORD_TEMPLATE_PROPERTY, mapper.valueToTree(resetPasswordTemplate));
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        if (profileAttributes != null) {
            for (String att : profileAttributes) {
                array.add(att);
            }
        }
        node.set(USER_PROFILE_ATTRIBUTES_PROPERTY, array);    
        return node.toString();
    }
    public void setData(String data) {
        try {
            JsonNode node = mapper.readTree(data);
            this.researcherRole = JsonUtils.asText(node, RESEARCHER_ROLE_PROPERTY);
            this.minAgeOfConsent = JsonUtils.asIntPrimitive(node, MIN_AGE_OF_CONSENT_PROPERTY);
            this.maxNumOfParticipants = JsonUtils.asIntPrimitive(node, MAX_NUM_OF_PARTICIPANTS_PROPERTY);
            this.supportEmail = JsonUtils.asText(node, SUPPORT_EMAIL_PROPERTY);
            this.consentNotificationEmail = JsonUtils.asText(node, CONSENT_NOTIFICATION_EMAIL_PROPERTY);
            this.stormpathHref = JsonUtils.asText(node, STORMPATH_HREF_PROPERTY);
            this.profileAttributes = JsonUtils.asStringSet(node, USER_PROFILE_ATTRIBUTES_PROPERTY);
            this.passwordPolicy = JsonUtils.asEntity(node, PASSWORD_POLICY_PROPERTY, PasswordPolicy.class);
            this.verifyEmailTemplate = JsonUtils.asEntity(node, VERIFY_EMAIL_TEMPLATE_PROPERTY, EmailTemplate.class);
            this.resetPasswordTemplate = JsonUtils.asEntity(node, RESET_PASSWORD_TEMPLATE_PROPERTY, EmailTemplate.class);
            this.sponsorName = JsonUtils.asText(node, SPONSOR_NAME_PROPERTY);
            this.technicalEmail = JsonUtils.asText(node, TECHNICAL_EMAIL_PROPERTY);
            this.active = JsonUtils.asBoolean(node, ACTIVE_PROPERTY);
            this.passwordPolicy = JsonUtils.asEntity(node, PASSWORD_POLICY_PROPERTY, PasswordPolicy.class);
            this.verifyEmailTemplate = JsonUtils.asEntity(node, VERIFY_EMAIL_TEMPLATE_PROPERTY, EmailTemplate.class);
            this.resetPasswordTemplate = JsonUtils.asEntity(node, RESET_PASSWORD_TEMPLATE_PROPERTY, EmailTemplate.class);
        } catch (IOException e) {
            throw new BridgeServiceException(e);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Objects.hashCode(identifier);
        result = prime * result + Objects.hashCode(maxNumOfParticipants);
        result = prime * result + Objects.hashCode(minAgeOfConsent);
        result = prime * result + Objects.hashCode(name);
        result = prime * result + Objects.hashCode(sponsorName);
        result = prime * result + Objects.hashCode(researcherRole);
        result = prime * result + Objects.hashCode(supportEmail);
        result = prime * result + Objects.hashCode(technicalEmail);
        result = prime * result + Objects.hashCode(consentNotificationEmail);
        result = prime * result + Objects.hashCode(stormpathHref);
        result = prime * result + Objects.hashCode(version);
        result = prime * result + Objects.hashCode(profileAttributes);
        result = prime * result + Objects.hashCode(passwordPolicy);
        result = prime * result + Objects.hashCode(verifyEmailTemplate);
        result = prime * result + Objects.hashCode(resetPasswordTemplate);
        result = prime * result + Objects.hashCode(active);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        DynamoStudy other = (DynamoStudy) obj;
        
        return (Objects.equals(identifier, other.identifier) && Objects.equals(supportEmail, other.supportEmail) &&
            Objects.equals(maxNumOfParticipants, other.maxNumOfParticipants) && 
            Objects.equals(minAgeOfConsent, other.minAgeOfConsent) && Objects.equals(name, other.name) && 
            Objects.equals(researcherRole, other.researcherRole) && Objects.equals(stormpathHref, other.stormpathHref) && 
            Objects.equals(consentNotificationEmail, other.consentNotificationEmail) && 
            Objects.equals(version, other.version) && Objects.equals(profileAttributes, other.profileAttributes) && 
            Objects.equals(sponsorName, other.sponsorName) && Objects.equals(technicalEmail, other.technicalEmail) && 
            Objects.equals(active, other.active));
    }

    @Override
    public String toString() {
        return String.format("DynamoStudy [name=%s, active=%s, sponsorName=%, identifier=%s, researcherRole=%s, stormpathHref=%s, "
            + "minAgeOfConsent=%s, maxNumOfParticipants=%s, supportEmail=%s, technicalEmail=%s, consentNotificationEmail=%s, "
            + "version=%s, userProfileAttributes=%s, passwordPolicy=%s, verifyEmailTemplate=%s, resetPasswordTemplate=%s]",
            name, active, sponsorName, identifier, researcherRole, stormpathHref, minAgeOfConsent, maxNumOfParticipants,
            supportEmail, technicalEmail, consentNotificationEmail, version, profileAttributes, passwordPolicy,
            verifyEmailTemplate, resetPasswordTemplate);        
    }
    
}
