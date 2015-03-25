package org.activityinfo.server.digest.activity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.activityinfo.server.authentication.ServerSideAuthProvider;
import org.activityinfo.server.digest.UserDigestResource;
import org.activityinfo.server.mail.MailSender;
import org.activityinfo.server.util.monitoring.Metrics;

import javax.persistence.EntityManager;
import javax.ws.rs.Path;

@Path(ActivityUserDigestResource.ENDPOINT)
public class ActivityUserDigestResource extends UserDigestResource {
    public static final String ENDPOINT = "/tasks/activityuserdigest";
    public static final int PARAM_DAYS_DEF = 7; // one week

    @Inject
    public ActivityUserDigestResource(Provider<EntityManager> entityManager,
                                      Provider<MailSender> mailSender,
                                      ServerSideAuthProvider authProvider,
                                      ActivityDigestModelBuilder activityDigestModelBuilder,
                                      ActivityDigestRenderer activityDigestRenderer,
                                      Metrics metrics) {
        super(entityManager, mailSender, authProvider, activityDigestModelBuilder, activityDigestRenderer, metrics);
    }

    @Override
    public int getDefaultDays() {
        return PARAM_DAYS_DEF;
    }
}
