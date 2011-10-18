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
      PortletURL actionURL = aRenderResponse.createActionURL();

      writer.println("<form action=\"" + actionURL.toString() + "\" method=\"post\">");

      for (Enumeration enm = prefs.getNames(); enm.hasMoreElements(); ) {
        String name = (String) enm.nextElement();
        String value = prefs.getValue(name, "");
        writer.println("<label for=\"" + name + "\">" + name + ": </label>");
        writer.println("<textarea id=\"" + name + "\" name=\"" + name + "\" type=\"text\" style=\"width: 100%; height: 15em;\">" + value + "</textarea><br />");
      }
      writer.println("<input type=\"submit\" value=\"Save\" />");
      writer.println("</form>");
    } else {
      writer.println("<strong>You are not allowed to update the portlet configuration</strong>");
    }
  }

  protected void doView(RenderRequest aRenderRequest, RenderResponse aRenderResponse) throws PortletException, IOException {
    PortletPreferences prefs = aRenderRequest.getPreferences();
    PrintWriter writer = aRenderResponse.getWriter();

    ScriptingContainer container = new ScriptingContainer(LocalContextScope.THREADSAFE, LocalVariableBehavior.PERSISTENT);

    container.setOutput(writer);
    container.runScriptlet(prefs.getValue("script", ""));
    container.runScriptlet("require 'erb'; template = ERB.new <<-EOF\n" +
      prefs.getValue("erb", "") + "\n" +
      "EOF\n" +
      "puts template.result(binding)");
    writer.flush();
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
}