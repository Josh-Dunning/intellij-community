package com.intellij.usageView.impl;

import com.intellij.ide.impl.ContentManagerWatcher;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.*;
import com.intellij.usageView.UsageViewManager;
import com.intellij.usages.UsageView;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class UsageViewManagerImpl extends UsageViewManager implements ProjectComponent {
  private final Key<Boolean> REUSABLE_CONTENT_KEY = Key.create("UsageTreeManager.REUSABLE_CONTENT_KEY");
  private final Key<Boolean> NOT_REUSABLE_CONTENT_KEY = Key.create("UsageTreeManager.NOT_REUSABLE_CONTENT_KEY");        //todo[myakovlev] dont use it
  private final Key<UsageView> NEW_USAGE_VIEW_KEY = Key.create("NEW_USAGE_VIEW_KEY");
  private final ToolWindowManager myToolWindowManager;
  private ContentManager myFindContentManager;

  public UsageViewManagerImpl(final ToolWindowManager toolWindowManager) {
    myToolWindowManager = toolWindowManager;
  }

  public void disposeComponent() {
  }

  public void initComponent() {
  }

  public void projectOpened() {
    ToolWindow toolWindow = myToolWindowManager.registerToolWindow(ToolWindowId.FIND, true, ToolWindowAnchor.BOTTOM);
    toolWindow.setToHideOnEmptyContent(true);
    toolWindow.setIcon(IconLoader.getIcon("/general/toolWindowFind.png"));
    myFindContentManager = toolWindow.getContentManager();
    myFindContentManager.addContentManagerListener(new ContentManagerAdapter() {
      public void contentRemoved(ContentManagerEvent event) {
        event.getContent().release();
      }
    });
    new ContentManagerWatcher(toolWindow, myFindContentManager);
  }

  public void projectClosed() {
    myToolWindowManager.unregisterToolWindow(ToolWindowId.FIND);
    myFindContentManager = null;
  }

  public Content addContent(String contentName, boolean reusable, final JComponent component, boolean toOpenInNewTab, boolean isLockable) {
    return addContent(contentName, null, null, reusable, component, toOpenInNewTab, isLockable);
  }

  public Content addContent(String contentName, String tabName, String toolwindowTitle, boolean reusable, final JComponent component,
                            boolean toOpenInNewTab, boolean isLockable) {
    Key<Boolean> contentKey = reusable ? REUSABLE_CONTENT_KEY : NOT_REUSABLE_CONTENT_KEY;

    if (!toOpenInNewTab && reusable) {
      Content[] contents = myFindContentManager.getContents();
      Content contentToDelete = null;

      for (Content content : contents) {
        if (!content.isPinned() &&
            content.getUserData(contentKey) != null
          ) {
          UsageView usageView = content.getUserData(NEW_USAGE_VIEW_KEY);
          if (usageView == null || !usageView.isSearchInProgress()) {
            contentToDelete = content;
          }
        }
      }
      if (contentToDelete != null) {
        myFindContentManager.removeContent(contentToDelete, true);
      }
    }
    Content content = ContentFactory.SERVICE.getInstance().createContent(component, contentName, isLockable);
    content.setTabName(tabName);
    content.setToolwindowTitle(toolwindowTitle);
    content.putUserData(contentKey, Boolean.TRUE);

    myFindContentManager.addContent(content);
    myFindContentManager.setSelectedContent(content);

    return content;
  }

  public int getReusableContentsCount() {
    return getContentCount(true);
  }

  private int getContentCount(boolean reusable) {
    Key<Boolean> contentKey = reusable ? REUSABLE_CONTENT_KEY : NOT_REUSABLE_CONTENT_KEY;
    int count = 0;
    Content[] contents = myFindContentManager.getContents();
    for (Content content : contents) {
      if (content.getUserData(contentKey) != null) {
        count++;
      }
    }
    return count;
  }

  public Content getSelectedContent(boolean reusable) {
    Key<Boolean> contentKey = reusable ? REUSABLE_CONTENT_KEY : NOT_REUSABLE_CONTENT_KEY;
    Content selectedContent = myFindContentManager.getSelectedContent();
    return selectedContent == null || selectedContent.getUserData(contentKey) == null ? null : selectedContent;
  }

  public Content getSelectedContent() {
    return myFindContentManager == null ? null : myFindContentManager.getSelectedContent();
  }

  public void closeContent(@NotNull Content content) {
    myFindContentManager.removeContent(content, true);
    content.release();
  }

  @NotNull
  public String getComponentName() {
    return "UsageViewManager";
  }
}