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

import org.activityinfo.legacy.shared.auth.AuthenticatedUser;

import javax.servlet.ServletException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

@Path(LogoutController.ENDPOINT)
public class LogoutController {
    public static final String ENDPOINT = "/logout";


    @GET
    public Response logout(@Context UriInfo uri) throws ServletException, IOException {
        return Response.seeOther(uri.getAbsolutePathBuilder().replacePath(LoginController.ENDPOINT).build())
                       .cookie(emptyCookies())
                       .build();
    }

    private NewCookie[] emptyCookies() {
        return new NewCookie[]{new NewCookie(AuthenticatedUser.AUTH_TOKEN_COOKIE, null),
                new NewCookie(AuthenticatedUser.EMAIL_COOKIE, null),
                new NewCookie(AuthenticatedUser.USER_ID_COOKIE, null),
                new NewCookie(AuthenticatedUser.USER_LOCAL_COOKIE, null)};
    }
}
