package org.activityinfo.server.command.handler;

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

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.activityinfo.legacy.shared.Log;
import org.activityinfo.legacy.shared.command.GetSyncRegionUpdates;
import org.activityinfo.legacy.shared.command.result.CommandResult;
import org.activityinfo.legacy.shared.command.result.SyncRegionUpdate;
import org.activityinfo.legacy.shared.exception.CommandException;
import org.activityinfo.server.command.handler.sync.*;
import org.activityinfo.server.database.hibernate.entity.User;
import org.activityinfo.server.util.monitoring.Metrics;
import org.activityinfo.server.util.monitoring.Profiler;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GetSyncRegionUpdatesHandler implements CommandHandler<GetSyncRegionUpdates> {

    private static final Logger LOGGER = Logger.getLogger(GetSyncRegionsHandler.class.getName());

    private final Injector injector;
    private final Metrics metrics;

    @Inject
    public GetSyncRegionUpdatesHandler(Injector injector, Metrics metrics) {
        this.injector = injector;
        this.metrics = metrics;
    }

    @Override
    public CommandResult execute(GetSyncRegionUpdates cmd, User user) throws CommandException {

        Log.info("Fetching updates for " + cmd.getRegionId() + ", localVersion = " + cmd.getLocalVersion());

        UpdateBuilder builder;

        if (cmd.getRegionId().startsWith("db/")) {
            builder = injector.getInstance(DbUpdateBuilder.class);

        } else if (cmd.getRegionId().startsWith("admin/")) {
            builder = injector.getInstance(AdminUpdateBuilder.class);

        } else if (cmd.getRegionId().startsWith("location/")) {
            builder = injector.getInstance(LocationUpdateBuilder.class);

        } else if (cmd.getRegionId().startsWith("form-submissions/")) {
            builder = injector.getInstance(SiteUpdateBuilder.class);

        } else if (cmd.getRegionId().equals("site-tables")) {
            builder = injector.getInstance(TableDefinitionUpdateBuilder.class);

        } else {
            throw new CommandException("Unknown sync region: " + cmd.getRegionId());
        }

        Profiler profiler = metrics.profile("sync", "region", prefix(cmd.getRegionId()));
        try {
            
            SyncRegionUpdate update = builder.build(user, cmd);
            
            profiler.succeeded();
            return update;
            
        } catch (Exception e) {
            profiler.failed();
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private String prefix(String regionId) {
        String prefix;
        int slash = regionId.indexOf('/');
        if(slash != -1) {
            prefix = regionId.substring(0, slash);
        } else {
            prefix = regionId;
        }
        return prefix.replace('-', '_');
    }

}
