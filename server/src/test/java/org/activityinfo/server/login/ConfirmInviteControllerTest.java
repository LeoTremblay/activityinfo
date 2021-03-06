package org.activityinfo.server.login;

/*
 * #%L
 * ActivityInfo Server
 * %%
 * Copyright (C) 2009 - 2013 UNICEF
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.inject.util.Providers;
import com.sun.jersey.api.view.Viewable;
import org.activityinfo.server.database.hibernate.dao.AuthenticationDAO;
import org.activityinfo.server.database.hibernate.dao.UserDAO;
import org.activityinfo.server.database.hibernate.entity.Authentication;
import org.activityinfo.server.database.hibernate.entity.User;
import org.activityinfo.server.login.model.ConfirmInvitePageModel;
import org.activityinfo.server.login.model.InvalidInvitePageModel;
import org.activityinfo.server.util.MailingListClient;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.NoResultException;
import javax.ws.rs.core.Response;

import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ConfirmInviteControllerTest {

    private static final String VALID_KEY = "xyz123";
    private UserDAO userDAO;
    private ConfirmInviteController resource;
    private User user;

    @Before
    public final void setup() {

        user = new User();

        userDAO = createMock(UserDAO.class);
        expect(userDAO.findUserByChangePasswordKey(eq(VALID_KEY))).andReturn(
                user);
        expect(userDAO.findUserByChangePasswordKey(EasyMock.not(eq(VALID_KEY))))
                .andThrow(new NoResultException());
        replay(userDAO);

        AuthenticationDAO authDAO = createMock(AuthenticationDAO.class);
        authDAO.persist(isA(Authentication.class));
        expectLastCall().anyTimes();

        MailingListClient mailingListClient = createNiceMock(MailingListClient.class);
        replay(mailingListClient);

        AuthTokenProvider authTokenProvider = new AuthTokenProvider(
                Providers.of(authDAO));

        resource = new ConfirmInviteController(
                Providers.of(userDAO), authTokenProvider, mailingListClient);
    }

    @Test
    public void requestWithValidKeyShouldGetView() throws Exception {

        Viewable response = resource.getPage(RestMockUtils
                .mockUriInfo("http://www.activityinfo.org/confirm?xyz123"));

        assertThat(response.getModel(), instanceOf(ConfirmInvitePageModel.class));

    }

    @Test
    public void badKeyShouldGetProblemPage() throws Exception {

        Viewable response = resource.getPage(RestMockUtils
                .mockUriInfo("http://www.activityinfo.org/confirm?badkey"));

        assertThat(response.getModel(),
                instanceOf(InvalidInvitePageModel.class));
    }

    @Test
    public void passwordShouldBeSetAfterNewUserCompletion() throws Exception {

        resource.confirm(
                RestMockUtils.mockUriInfo("http://www.activityinfo.org/confirm"),
                VALID_KEY, "fr", "foobar", "Alex Bertram", false);

        assertThat(user.getHashedPassword(), is(not(nullValue())));
        assertThat(user.getLocale(), equalTo("fr"));
        assertThat(user.getChangePasswordKey(), is(nullValue()));
        assertThat(user.getName(), equalTo("Alex Bertram"));
    }

    @Test
    public void emptyPasswordShouldNotBeAccepted() throws Exception {
        Response response = resource.confirm(
                RestMockUtils.mockUriInfo("http://www.activityinfo.org/confirm"),
                VALID_KEY, "fr", null, "Alex Bertram", false);

        Viewable viewable = (Viewable) response.getEntity();
        assertThat(viewable.getModel(),
                instanceOf(ConfirmInvitePageModel.class));

        ConfirmInvitePageModel model = (ConfirmInvitePageModel) viewable
                .getModel();
        assertTrue("error message set", model.isFormIncomplete());
    }
}
