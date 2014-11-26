package org.activityinfo.server.endpoint.odk;

import com.google.api.client.util.Maps;
import com.google.appengine.api.images.Image;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import org.activityinfo.legacy.shared.command.CreateLocation;
import org.activityinfo.legacy.shared.command.result.VoidResult;
import org.activityinfo.model.auth.AuthenticatedUser;
import org.activityinfo.model.form.FormClass;
import org.activityinfo.model.form.FormField;
import org.activityinfo.model.form.FormInstance;
import org.activityinfo.model.legacy.KeyGenerator;
import org.activityinfo.model.resource.ResourceId;
import org.activityinfo.model.type.FieldType;
import org.activityinfo.model.type.FieldValue;
import org.activityinfo.model.type.ReferenceType;
import org.activityinfo.model.type.ReferenceValue;
import org.activityinfo.model.type.geo.GeoPoint;
import org.activityinfo.model.type.geo.GeoPointType;
import org.activityinfo.model.type.image.ImageRowValue;
import org.activityinfo.model.type.image.ImageValue;
import org.activityinfo.model.type.primitive.TextValue;
import org.activityinfo.server.command.DispatcherSync;
import org.activityinfo.server.command.ResourceLocatorSync;
import org.activityinfo.server.endpoint.odk.xform.XFormInstance;
import org.activityinfo.server.endpoint.odk.xform.XFormInstanceImpl;
import org.activityinfo.service.blob.BlobFieldStorageService;
import org.activityinfo.service.blob.BlobId;
import org.w3c.dom.Element;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.appengine.api.images.ImagesServiceFactory.makeImage;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static org.activityinfo.model.legacy.CuidAdapter.GPS_FIELD;
import static org.activityinfo.model.legacy.CuidAdapter.LOCATION_FIELD;
import static org.activityinfo.model.legacy.CuidAdapter.LOCATION_NAME_FIELD;
import static org.activityinfo.model.legacy.CuidAdapter.field;
import static org.activityinfo.model.legacy.CuidAdapter.getLegacyIdFromCuid;
import static org.activityinfo.model.legacy.CuidAdapter.locationInstanceId;
import static org.activityinfo.model.legacy.CuidAdapter.newLegacyFormInstanceId;
import static org.activityinfo.server.endpoint.odk.OdkHelper.isLocation;

@Path("/submission")
public class FormSubmissionResource {
    private static final Logger LOGGER = Logger.getLogger(FormSubmissionResource.class.getName());

    final private DispatcherSync dispatcher;
    final private ResourceLocatorSync locator;
    final private AuthenticationTokenService authenticationTokenService;
    final private BlobFieldStorageService blobFieldStorageService;
    final private SubmissionArchiver submissionArchiver;

    @Inject
    public FormSubmissionResource(DispatcherSync dispatcher,
                                  ResourceLocatorSync locator,
                                  AuthenticationTokenService authenticationTokenService,
                                  BlobFieldStorageService blobFieldStorageService,
                                  SubmissionArchiver submissionArchiver) {
        this.dispatcher = dispatcher;
        this.locator = locator;
        this.authenticationTokenService = authenticationTokenService;
        this.blobFieldStorageService = blobFieldStorageService;
        this.submissionArchiver = submissionArchiver;
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_XML)
    public Response submit(byte bytes[]) {

        XFormInstance instance = new XFormInstanceImpl(bytes);

        String authenticationToken = instance.getAuthenticationToken();
        AuthenticatedUser user = authenticationTokenService.authenticate(authenticationToken);

        FormClass formClass = locator.getFormClass(instance.getFormClassId());
        ResourceId formId = newLegacyFormInstanceId(formClass.getId());
        FormInstance formInstance = new FormInstance(formId, formClass.getId());

        LOGGER.log(Level.INFO, "Saving XForm " + instance.getId() + " as " + formId);

        for (FormField formField : formClass.getFields()) {
            Optional<Element> element = instance.getFieldContent(formField.getId());
            if (element.isPresent()) {
                formInstance.set(formField.getId(), tryParse(formInstance, formField, element.get()));
            } else if (isLocation(formClass, formField)) {
                FieldType fieldType = formField.getType();
                Optional<Element> gpsField = instance.getFieldContent(field(formClass.getId(), GPS_FIELD));
                Optional<Element> nameField = instance.getFieldContent(field(formClass.getId(), LOCATION_NAME_FIELD));

                if (fieldType instanceof ReferenceType && gpsField.isPresent() && nameField.isPresent()) {
                    ResourceId locationFieldId = field(formClass.getId(), LOCATION_FIELD);
                    int newLocationId = new KeyGenerator().generateInt();
                    ResourceId locationFormClassId = Iterables.getOnlyElement(((ReferenceType) fieldType).getRange());
                    int locationTypeId = getLegacyIdFromCuid(locationFormClassId);
                    FieldValue fieldValue = new ReferenceValue(locationInstanceId(newLocationId));
                    String name = OdkHelper.extractText(nameField.get());
                    GeoPoint geoPoint = parseLocation(gpsField.get());

                    formInstance.set(locationFieldId, fieldValue);
                    createLocation(newLocationId, locationTypeId, name, geoPoint);
                }
            }
        }

        for (FieldValue fieldValue : formInstance.getFieldValueMap().values()) {
            if (fieldValue instanceof ImageValue) {
                persistImageData(user, instance, (ImageValue) fieldValue);
            }
        }
        locator.persist(formInstance);

        // Backup the original XForm in case something went wrong with processing
        submissionArchiver.backup(formClass.getId(), formId, ByteSource.wrap(bytes));

        return Response.status(CREATED).build();
    }

    private FieldValue tryParse(FormInstance formInstance, FormField formField, Element element) {
        try {
            OdkFieldValueParser odkFieldValueParser = OdkFieldValueParserFactory.fromFieldType(formField.getType());
            return odkFieldValueParser.parse(element);

        } catch (Exception e) {
            String text = OdkHelper.extractText(element);

            if (text == null) {
                LOGGER.log(Level.SEVERE, "Malformed Element in form instance prevents parsing", e);
            } else if (!text.equals("")) {
                LOGGER.log(Level.WARNING, "Can't parse form instance contents, storing as text", e);
                formInstance.set(formField.getId(), TextValue.valueOf(text));
            }
        }
        return null;
    }

    private GeoPoint parseLocation(Element element) {
        try {
            OdkFieldValueParser odkFieldValueParser = OdkFieldValueParserFactory.fromFieldType(GeoPointType.INSTANCE);
            return (GeoPoint) odkFieldValueParser.parse(element);
        } catch (Exception e) {
            return null;
        }
    }

    private void persistImageData(AuthenticatedUser user, XFormInstance instance, ImageValue fieldValue) {
        ImageRowValue imageRowValue = fieldValue.getValues().get(0);
        if (imageRowValue.getFilename() != null) {
            try {
                BodyPart bodyPart = ((XFormInstanceImpl) instance).findBodyPartByFilename(imageRowValue.getFilename());
                Image image = makeImage(ByteStreams.toByteArray(bodyPart.getInputStream()));

                String contentDisposition = bodyPart.getDisposition();
                String mimeType = bodyPart.getContentType();
                ByteSource byteSource = ByteSource.wrap(image.getImageData());
                imageRowValue.setMimeType(mimeType);
                imageRowValue.setHeight(image.getHeight());
                imageRowValue.setWidth(image.getWidth());


                blobFieldStorageService.put(user, contentDisposition, mimeType,
                        new BlobId(imageRowValue.getBlobId()), byteSource);

            } catch (MessagingException messagingException) {
                LOGGER.log(Level.SEVERE, "Unable to parse input", messagingException);
                throw new WebApplicationException(Response.status(BAD_REQUEST).build());
            } catch (IOException ioException) {
                LOGGER.log(Level.SEVERE, "Could not write image to GCS", ioException);
                throw new WebApplicationException(Response.status(SERVICE_UNAVAILABLE).build());
            }
        }
    }

    private VoidResult createLocation(int id, int locationTypeId, String name, GeoPoint geoPoint) {
        Map<String, Object> properties = Maps.newHashMap();

        properties.put("id", id);
        properties.put("locationTypeId", locationTypeId);
        properties.put("name", name);

        if (geoPoint != null) {
            properties.put("latitude", geoPoint.getLatitude());
            properties.put("longitude", geoPoint.getLongitude());
        }

        return dispatcher.execute(new CreateLocation(properties));
    }
}
