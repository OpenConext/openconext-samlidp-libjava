package saml;

import lombok.SneakyThrows;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.credential.impl.KeyStoreCredentialResolver;
import org.opensaml.security.criteria.UsageCriterion;
import org.w3c.dom.Element;
import saml.crypto.KeyStoreLocator;
import saml.model.SAMLConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultSAMLIdPServiceTest {

    private static final DefaultSAMLIdPService openSamlParser;
    private static final SimpleDateFormat issueFormat = new SimpleDateFormat("yyyy-MM-dd'T'H:mm:ss");
    private static final SAMLConfiguration samlConfiguration;
    private static final Credential signinCredential;

    static {
        String entityId = "https://test.entity.org";
        samlConfiguration = new SAMLConfiguration(
                readFile("saml_idp.crt"),
                readFile("saml_idp.pem"),
                entityId,
                readFile("saml_idp.crt"),
                false
        );
        openSamlParser = new DefaultSAMLIdPService(samlConfiguration);
        KeyStore keyStore = KeyStoreLocator.createKeyStore(
                entityId,
                samlConfiguration.getIdpCertificate(),
                samlConfiguration.getIdpPrivateKey(),
                "secret"
        );
        KeyStoreCredentialResolver resolver = new KeyStoreCredentialResolver(keyStore, Map.of(entityId, "secret"), UsageType.SIGNING);
        try {
            signinCredential = resolver.resolveSingle(new CriteriaSet(new EntityIdCriterion(entityId), new UsageCriterion(UsageType.SIGNING)));
        } catch (ResolverException e) {
            throw new RuntimeException(e);
        }

    }

    @SneakyThrows
    private String samlAuthnRequest() {
        String samlRequestTemplate = readFile("authn_request.xml");
        String samlRequest = String.format(samlRequestTemplate, UUID.randomUUID(), issueFormat.format(new Date()));
        return deflatedBase64encoded(samlRequest);
    }

    @SneakyThrows
    private String signedSamlAuthnRequest() {
        String samlRequest = samlAuthnRequest();

        AuthnRequest authnRequest = openSamlParser.parseAuthnRequest(samlRequest, true, true);
        openSamlParser.signObject(authnRequest, signinCredential);

        Element element = XMLObjectSupport.marshall(authnRequest);
        String xml = SerializeSupport.nodeToString(element);

        return deflatedBase64encoded(xml);
    }


    @SneakyThrows
    private static String readFile(String path) {
        InputStream inputStream = DefaultSAMLIdPService.class.getClassLoader().getResourceAsStream(path);
        return IOUtils.toString(inputStream, Charset.defaultCharset());
    }

    private String deflatedBase64encoded(String input) throws IOException {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.DEFLATED, true);
        DeflaterOutputStream deflaterStream = new DeflaterOutputStream(bytesOut, deflater);
        deflaterStream.write(input.getBytes(Charset.defaultCharset()));
        deflaterStream.finish();
        return new String(Base64.encodeBase64(bytesOut.toByteArray()));
    }

    @SneakyThrows
    @Test
    void parseAuthnRequest() {
        String samlRequest = this.samlAuthnRequest();
        AuthnRequest authnRequest = openSamlParser.parseAuthnRequest(samlRequest, true, true);
        String uri = authnRequest.getScoping().getRequesterIDs().get(0).getURI();
        assertEquals("https://test.surfconext.nl", uri);
    }

    @SneakyThrows
    @Test
    void parseSignedAuthnRequest() {
        String authnRequestXML = this.signedSamlAuthnRequest();
        AuthnRequest authnRequest = openSamlParser.parseAuthnRequest(authnRequestXML, true, true);

        String uri = authnRequest.getScoping().getRequesterIDs().get(0).getURI();
        assertEquals("https://test.surfconext.nl", uri);
    }
}