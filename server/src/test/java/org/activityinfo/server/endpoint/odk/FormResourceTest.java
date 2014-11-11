package org.activityinfo.server.endpoint.odk;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.inject.Provider;
import com.google.inject.util.Providers;
import org.activityinfo.fixtures.InjectionSupport;
import org.activityinfo.model.auth.AuthenticatedUser;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.server.command.CommandTestCase2;
import org.activityinfo.server.command.ResourceLocatorSyncImpl;
import org.activityinfo.server.database.OnDataSet;
import org.activityinfo.server.endpoint.odk.xform.XForm;
import org.activityinfo.service.DeploymentConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;

import static com.google.common.io.Resources.asByteSource;
import static com.google.common.io.Resources.getResource;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.fromStatusCode;
import static org.activityinfo.model.legacy.CuidAdapter.*;
import static org.activityinfo.model.legacy.CuidAdapter.field;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

@RunWith(InjectionSupport.class)
public class FormResourceTest extends CommandTestCase2 {

    public static final int ACTIVITY_ID = 11218;
    public static final int USER_ID = 9944;

    private FormResource formResource;
    private FormSubmissionResource formSubmissionResource;

    @Before
    public void setUp() throws IOException {
        ResourceLocatorSyncImpl resourceLocator = new ResourceLocatorSyncImpl(getDispatcherSync());
        Provider<AuthenticatedUser> authProvider = Providers.of(new AuthenticatedUser("", USER_ID, "jorden@bdd.com"));

        OdkFormFieldBuilderFactory fieldFactory = new OdkFormFieldBuilderFactory(resourceLocator);
        OdkFieldValueParserFactory parserFactory = new OdkFieldValueParserFactory(resourceLocator);

        AuthTokenProvider tokenService = new AuthTokenProvider();

        TestBlobstoreService blobstore = new TestBlobstoreService();
        SubmissionArchiver backupService = new SubmissionArchiver(
                new DeploymentConfiguration(new Properties()));

        formResource = new FormResource(resourceLocator, authProvider, fieldFactory, tokenService);
        formSubmissionResource = new FormSubmissionResource(
                parserFactory, resourceLocator, tokenService, blobstore,
                backupService);
    }

    @Test
    @OnDataSet("/dbunit/chad-form.db.xml")
    public void getBlankForm() throws JAXBException, URISyntaxException, IOException, InterruptedException {

        setUser(USER_ID);

        Response form = this.formResource.form(ACTIVITY_ID);
        File file = new File(targetDir(), "form.xml");
        JAXBContext context = JAXBContext.newInstance(XForm.class);
        Marshaller marshaller = context.createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(form.getEntity(), file);
        validate(file);
    }


    @Test @OnDataSet("/dbunit/sites-simple1.db.xml")
    public void current() throws IOException {

    }

    @Test @OnDataSet("/dbunit/chad-form.db.xml")
    public void parse() throws IOException {
        byte bytes[] = asByteSource(getResource(FormResourceTest.class, "form.mime")).read();

        Response response = formSubmissionResource.submit(bytes);
        assertEquals(CREATED, fromStatusCode(response.getStatus()));

        //Map<String, Object> map = store.getLastUpdated().getProperties();

//        assertEquals(7, map.size());
//        assertEquals(CLASS_ID.asString(), map.get("classId"));
//        assertEquals(new ReferenceValue(partnerInstanceId(507, 562)).asRecord(), map.get(fieldName(PARTNER_FIELD)));
//        assertEquals(new LocalDate(2005, 8, 31).asRecord(), map.get(fieldName(END_DATE_FIELD)));
//        assertEquals("09/06/06", map.get(CODE_FIELD));
//        assertEquals(new ReferenceValue(entity(141796)).asRecord(), map.get("a1081f11"));
//        assertNull(map.get("i5346"));
//        assertEquals(new NarrativeValue("Awesome.").asRecord(), map.get("a1081f14"));
//        assertNotNull(map.get("backupBlobId"));
    }


    @Test @OnDataSet("/dbunit/chad-form.db.xml")
    public void parseWithBlankComments() throws IOException {
        byte bytes[] = asByteSource(getResource(FormResourceTest.class, "form-no-comments.mime")).read();

        Response response = formSubmissionResource.submit(bytes);
        assertEquals(CREATED, fromStatusCode(response.getStatus()));

        //Map<String, Object> map = store.getLastUpdated().getProperties();

//        assertEquals(7, map.size());
//        assertEquals(CLASS_ID.asString(), map.get("classId"));
//        assertEquals(new ReferenceValue(partnerInstanceId(507, 562)).asRecord(), map.get(fieldName(PARTNER_FIELD)));
//        assertEquals(new LocalDate(2005, 8, 31).asRecord(), map.get(fieldName(END_DATE_FIELD)));
//        assertEquals("09/06/06", map.get(CODE_FIELD));
//        assertEquals(new ReferenceValue(entity(141796)).asRecord(), map.get("a1081f11"));
//        assertNull(map.get("i5346"));
//        assertEquals(new NarrativeValue("Awesome.").asRecord(), map.get("a1081f14"));
//        assertNotNull(map.get("backupBlobId"));
    }

    private String fieldName(int fieldIndex) {
        return field(activityFormClass(ACTIVITY_ID), fieldIndex).asString();
    }

    private File targetDir() {
        String relPath = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        File targetDir = new File(relPath + "../../target");
        if (!targetDir.exists()) {
            targetDir.mkdir();
        }
        return targetDir;
    }

    public void validate(File file) throws URISyntaxException, IOException, InterruptedException {


        URL validatorJar = Resources.getResource(FormResourceTest.class, "odk-validate-1.4.3.jar");
        String[] command = {"java", "-jar", Paths.get(validatorJar.toURI()).toString(), file.getAbsolutePath()};

        System.out.println(Joiner.on(' ').join(command));

        ProcessBuilder validator = new ProcessBuilder(command);
        validator.inheritIO();
        int exitCode = validator.start().waitFor();

        if(exitCode != 0) {
            System.out.println("Offending XML: " + Files.toString(file, Charsets.UTF_8));
        }

        assertThat(exitCode, equalTo(0));
    }

    private class AuthTokenProvider implements AuthenticationTokenService {

        @Override
        public String createAuthenticationToken(int userId, ResourceId formClassId) {
            return "XYZ";
        }

        @Override
        public AuthenticatedUser authenticate(String authenticationToken) {
            Set<String> tokens = Sets.newHashSet("LDbRuQsl", "token:3e2a26585ac83ff5", "token:c55536ea93b0c7da");
            if(tokens.contains(authenticationToken)) {
                AuthenticatedUser user = new AuthenticatedUser("", USER_ID, "@");
                setUser(user.getId());
                return user;
            }
            throw new AssertionError("Not authenticated");
        }
    }
}
