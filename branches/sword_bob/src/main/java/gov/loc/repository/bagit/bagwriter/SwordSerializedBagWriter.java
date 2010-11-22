package gov.loc.repository.bagit.bagwriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.MessageFormat;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFile;
import gov.loc.repository.bagit.BagWriter;
import gov.loc.repository.bagit.utilities.MessageDigestHelper;
import gov.loc.repository.bagit.Manifest.Algorithm;

public class SwordSerializedBagWriter implements BagWriter {

	public static final String CONTENT_TYPE = "application/zip";
	public static final String PACKAGING = "http://purl.org/net/sword-types/bagit";

	private static final Log log = LogFactory.getLog(SwordSerializedBagWriter.class);

	private ByteArrayOutputStream out = new ByteArrayOutputStream();
	private String collectionURL = null;
	private ZipBagWriter zipBagWriter = null;
	private Integer statusCode = null;
	private String body = null;
	private String location = null;
	
	public SwordSerializedBagWriter(String bagDir, String collectionURL) {
		this.collectionURL = collectionURL;
		this.out = new ByteArrayOutputStream();
		this.zipBagWriter = new ZipBagWriter(bagDir, this.out);
	}
	
	@Override
	public void close() {
		this.zipBagWriter.close();
		HttpClient client = new HttpClient();
		PostMethod post = new PostMethod(collectionURL);
		post.addRequestHeader("X-Packaging", PACKAGING);
		byte[] bagBytes = this.out.toByteArray();
		post.addRequestHeader("Content-MD5", MessageDigestHelper.generateFixity(new ByteArrayInputStream(bagBytes), Algorithm.MD5 ));
		post.setRequestEntity(new ByteArrayRequestEntity(bagBytes, CONTENT_TYPE));
		try {
			log.debug("Posting to " + collectionURL);
			client.executeMethod(post);
			log.debug(MessageFormat.format("Response to post was response code {0} and response body of {1}", post.getStatusCode(), post.getResponseBodyAsString()));
			this.statusCode = post.getStatusCode();
			this.body = post.getResponseBodyAsString();
			Header locationHeader = post.getResponseHeader("Location");
			if (locationHeader != null) {
				this.location = locationHeader.getValue();
			}
			if (post.getStatusCode() != HttpStatus.SC_CREATED) {
				throw new RuntimeException(MessageFormat.format("Attempt to create resource failed.  Server returned a response code of {0} and body of {1}", post.getStatusCode(), post.getResponseBodyAsString()));
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		} finally {
			post.releaseConnection();
		}

	}

	@Override
	public void open(Bag bag) {
		this.zipBagWriter.open(bag);

	}

	@Override
	public void writePayloadFile(String filepath, BagFile bagFile) {
		this.zipBagWriter.writePayloadFile(filepath, bagFile);

	}

	@Override
	public void writeTagFile(String filepath, BagFile bagFile) {
		this.zipBagWriter.writeTagFile(filepath, bagFile);

	}

	public String getLocation() {
		return location;
	}
	
	public Integer getStatusCode() {
		return statusCode;
	}

	public String getBody() {
		return body;
	}

}