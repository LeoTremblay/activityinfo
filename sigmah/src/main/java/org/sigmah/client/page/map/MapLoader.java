/*
 * All Sigmah code is released under the GNU General Public License v3
 * See COPYRIGHT.txt and LICENSE.txt.
 */

package org.sigmah.client.page.map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.sigmah.client.page.*;

/**
 * PageLoader for the Map page
 */
public class MapLoader implements PageLoader {
    private final Provider<MapPage> mapPageProvider;

    @Inject
    public MapLoader(NavigationHandler pageManager, PageStateSerializer placeSerializer,
                     Provider<MapPage> mapPageProvider) {
        this.mapPageProvider = mapPageProvider;
        pageManager.registerPageLoader(MapPage.PAGE_ID, this);
        placeSerializer.registerStatelessPlace(MapPage.PAGE_ID, new MapPageState());
    }

    @Override
    public void load(final PageId pageId, final PageState pageState, final AsyncCallback<Page> callback) {
        GWT.runAsync(new RunAsyncCallback() {
            @Override
            public void onFailure(Throwable throwable) {
                callback.onFailure(throwable);
            }

            @Override
            public void onSuccess() {
                if(MapPage.PAGE_ID.equals(pageId)) {
                    callback.onSuccess(mapPageProvider.get());
                }
            }
        });
    }
}