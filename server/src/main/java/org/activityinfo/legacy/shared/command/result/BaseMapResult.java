package org.activityinfo.legacy.shared.command.result;

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

import org.activityinfo.legacy.shared.model.TileBaseMap;

import java.util.List;

/**
 * List of <code>BaseMap</code>s returned by the <code>GetBaseMaps</code>
 * command.
 *
 * @author Alex Bertram
 * @see org.activityinfo.legacy.shared.model.TileBaseMap
 * @see org.activityinfo.legacy.shared.command.GetBaseMaps
 */
public class BaseMapResult implements CommandResult {

    List<TileBaseMap> baseMaps;

    public BaseMapResult() {

    }

    public BaseMapResult(List<TileBaseMap> maps) {
        this.baseMaps = maps;
    }

    public List<TileBaseMap> getBaseMaps() {
        return baseMaps;
    }
}
