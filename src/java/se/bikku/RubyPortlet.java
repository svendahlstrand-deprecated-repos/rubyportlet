package se.bikku;

import javax.portlet.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import senselogic.sitevision.api.Utils;
import senselogic.sitevision.api.security.PermissionUtil;

import org.jruby.embed.ScriptingContainer;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;

public class RubyPortlet extends GenericPortlet {
  protected static final String SITEVISION_CONFIG_PORTLET_MODE = "config";

  protected void doDispatch(RenderRequest aRenderRequest, RenderResponse aRenderResponse) throws PortletException, IOException {
    if (SITEVISION_CONFIG_PORTLET_MODE.equals(aRenderRequest.getPortletMode().toString())) {
      doConfig(aRenderRequest, aRenderResponse);
    } else {
      super.doDispatch(aRenderRequest, aRenderResponse);
    }
  }

  protected void doConfig(RenderRequest aRenderRequest, RenderResponse aRenderResponse) throws PortletException, IOException {
    PrintWriter writer = aRenderResponse.getWriter();

    // Check if current user are allowed to update the shared portlet preferences (i.e. has write permission on current page)
    if (hasWritePermission(aRenderRequest)) {
      PortletPreferences prefs = aRenderRequest.getPreferences();

      ScriptingContainer container = getScriptingContainer(writer);
      container.put("action_url", aRenderResponse.createActionURL());
      container.put("erb_template", prefs.getValue("erb", ""));
      container.put("script", prefs.getValue("script", ""));

      container.runScriptlet("require 'erb'; template = ERB.new(File.read('./WEB-INF/views/config.html.erb'))\n" +
        "puts template.result(binding)");
    } else {
      writer.println("<strong>You are not allowed to update the portlet configuration</strong>");
    }
  }

  protected void doView(RenderRequest aRenderRequest, RenderResponse aRenderResponse) throws PortletException, IOException {
    PrintWriter writer = aRenderResponse.getWriter();
    PortletPreferences prefs = aRenderRequest.getPreferences();

    ScriptingContainer container = getScriptingContainer(writer);

    container.runScriptlet(prefs.getValue("script", ""));
    container.runScriptlet("require 'erb'; template = ERB.new <<-EOF\n" +
      prefs.getValue("erb", "") + "\n" +
      "EOF\n" +
      "puts template.result(binding)");
  }

  public void processAction(ActionRequest anActionRequest, ActionResponse anActionResponse) throws PortletException, IOException {
    // Update shared portlet preferences if current user are allowed to update current page (i.e. has write permission)
    if (hasWritePermission(anActionRequest)) {
      // Iterate all preferences and store new values
      PortletPreferences prefs = anActionRequest.getPreferences();
      for (Enumeration enm = prefs.getNames(); enm.hasMoreElements(); ) {
        String name = (String) enm.nextElement();
        String newValue = anActionRequest.getParameter(name);
        if (newValue != null) {
          prefs.setValue(name, newValue);
        }
      }
      prefs.store();
    }
    anActionResponse.setPortletMode(PortletMode.VIEW);
  }

  protected final boolean hasWritePermission(PortletRequest aPortletRequest) {
    Utils siteVisionUtils = (Utils) aPortletRequest.getAttribute("sitevision.utils");
    PermissionUtil permissionUtil = siteVisionUtils.getPermissionUtil();

    return permissionUtil.hasWritePermission();
  }

  protected final ScriptingContainer getScriptingContainer(PrintWriter writer) {
    ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE, LocalVariableBehavior.PERSISTENT);
    container.setOutput(writer);
    container.setCurrentDirectory(getPortletContext().getRealPath(""));

    return container;
  }
}