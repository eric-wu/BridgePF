package org.sagebionetworks.bridge.s3;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;

import org.sagebionetworks.bridge.models.studies.MimeType;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;

/**
 * Helper class that simplifies reading S3 files. This is generally created by Spring. However, we don't use the
 * Component annotation because there are multiple S3 clients, so there may be multiple S3 helpers.
 */
public class S3Helper {
    private AmazonS3Client s3Client;

    /**
     * S3 Client. This is configured by Spring. We don't use the Autowired annotation because there are multiple S3
     * clients.
     */
    public void setS3Client(AmazonS3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Read the given S3 file as a byte array in memory.
     *
     * @param bucket
     *         S3 bucket to read from, must be non-null and non-empty
     * @param key
     *         S3 key (filename), must be non-null and non-empty
     * @return the S3 file contents as an in-memory byte array
     * @throws IOException
     *         if closing the stream fails
     */
    public byte[] readS3FileAsBytes(@Nonnull String bucket, @Nonnull String key) throws IOException {
        S3Object s3File = s3Client.getObject(bucket, key);
        try (InputStream s3Stream = s3File.getObjectContent()) {
            return ByteStreams.toByteArray(s3Stream);
        }
    }

    /**
     * Read the given S3 file contents as a string. The encoding is assumed to be UTF-8.
     *
     * @param bucket
     *         S3 bucket to read from, must be non-null and non-empty
     * @param key
     *         S3 key (filename), must be non-null and non-empty
     * @return the S3 file contents as a string
     * @throws IOException
     *         if closing the stream fails
     */
    public String readS3FileAsString(@Nonnull String bucket, @Nonnull String key) throws IOException {
        byte[] bytes = readS3FileAsBytes(bucket, key);
        return new String(bytes, Charsets.UTF_8);
    }

    public void writeBytesToS3(@Nonnull String bucket, @Nonnull String key, @Nonnull byte[] data) throws IOException {
        try (InputStream dataInputStream = new ByteArrayInputStream(data)) {
            s3Client.putObject(bucket, key, dataInputStream, null);
        }
    }


    public boolean exists(@Nonnull String bucket, @Nonnull String key) {
        final ObjectListing listing = s3Client.listObjects(bucket, key);
        return !listing.getObjectSummaries().isEmpty();
    }

    
    /**
     * Write the byte array to a bucket at S3. The bucket will be given world read privileges, and the request 
     * will be returned with the appropriate content type header for the document's MimeType.
     * @param bucket
     * @param key
     * @param data
     * @param type
     * @throws IOException
     */
    public void writeBytesToPublicS3(@Nonnull String bucket, @Nonnull String key, @Nonnull byte[] data, @Nonnull MimeType type) throws IOException {
        try (InputStream dataInputStream = new ByteArrayInputStream(data)) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(type.toString());
            PutObjectRequest request = new PutObjectRequest(bucket, key, dataInputStream, metadata)
                            .withCannedAcl(CannedAccessControlList.PublicRead);
            s3Client.putObject(request);
        }
    }
}
