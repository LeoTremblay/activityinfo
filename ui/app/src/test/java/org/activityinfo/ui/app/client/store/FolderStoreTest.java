package org.activityinfo.ui.app.client.store;

import com.teklabs.gwt.i18n.server.LocaleProxy;
import org.activityinfo.model.resource.FolderProjection;
import org.activityinfo.model.resource.ResourceNode;
import org.activityinfo.model.system.FolderClass;
import org.activityinfo.model.type.primitive.TextValue;
import org.activityinfo.ui.app.client.TestFolder;
import org.activityinfo.ui.app.client.TestFormClass;
import org.activityinfo.ui.app.client.TestScenario;
import org.activityinfo.ui.app.client.form.store.UpdateFieldAction;
import org.activityinfo.ui.app.client.request.FetchFolder;
import org.activityinfo.ui.app.client.request.SaveRequest;
import org.activityinfo.ui.flux.store.Status;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class FolderStoreTest {

    @Rule
    public TestScenario scenario = new TestScenario();

    @Before
    public void setUp() {
        LocaleProxy.initialize();
    }

    @Test
    public void testCacheNewWorkspace() {


        // Create a new workspace
        InstanceState workspaceDraft = scenario.application().getDraftStore().getWorkspaceDraft();
        workspaceDraft.updateField(new UpdateFieldAction(workspaceDraft.getInstanceId(), FolderClass.LABEL_FIELD_ID,
            TextValue.valueOf("My Workspace")));
        scenario.application().getRequestDispatcher().execute(new SaveRequest(workspaceDraft.getUpdatedResource()));

        // Verify that the folder store is updated
        Status<FolderProjection> folderProjectionStatus = scenario.application().getFolderStore().get(workspaceDraft.getInstanceId());
        assertTrue(folderProjectionStatus.isAvailable());
        assertThat(folderProjectionStatus.get().getRootNode().getLabel(), equalTo("My Workspace"));
    }

    @Test
    public void requestWorkspaceChildren() {
        TestFolder workspace = scenario.createWorkspace("Workspace A");
        TestFolder folder = workspace.createFolder("Folder 1");
        TestFormClass form = folder.newFormClass("Form 1").create();

        assertThat(folder.getId(), not(equalTo(workspace.getId())));

        workspace.fetch();

        scenario.fetchWorkspaces();

        Status<List<ResourceNode>> workspaceItems = scenario.application().getFolderStore().getFolderItems(workspace.getId());
        assertThat(workspaceItems.isAvailable(), equalTo(true));
        assertThat(workspaceItems.get(), hasSize(1));
        assertThat(workspaceItems.get().get(0).getId(), equalTo(folder.getId()));

        folder.fetch();

        Status<List<ResourceNode>> folderItems = scenario.application().getFolderStore().getFolderItems(folder.getId());
        assertTrue(folderItems.isAvailable());
        assertThat(folderItems.get().get(0).getLabel(), CoreMatchers.equalTo("Form 1"));
    }

    @Test
    public void testUpdateItem() {
        TestFolder workspace = scenario.createWorkspace("Workspace A");
        TestFolder folder = workspace.createFolder("Folder 1");
        TestFormClass form = folder.newFormClass("Form 1").create();

        // do the initial fetch
        scenario.application().getRequestDispatcher().execute(new FetchFolder(folder.getId()));

        assertThat(scenario.application().getFolderStore().getFolderItems(folder.getId()).get(), hasSize(1));


        // now add a form in this folder
        CountingStoreListener storeListener = new CountingStoreListener();
        scenario.application().getFolderStore().addChangeListener(storeListener);

        TestFormClass newForm = folder.newFormClass("Form 2").create();
        scenario.application().getRequestDispatcher().execute(new SaveRequest(newForm.get()));

        assertThat(storeListener.getChangeCount(), equalTo(1));
        assertThat(scenario.application().getFolderStore().getFolderItems(folder.getId()).get(), hasSize(2));
    }

}