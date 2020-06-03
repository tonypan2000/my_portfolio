package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import java.io.IOException;
import java.lang.Long;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that handles deletions of comments from Datastore */
@WebServlet("/delete-data")
public class DeleteDataServlet extends HttpServlet {

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("text/plain;");
    UserService userService = UserServiceFactory.getUserService();
    long id = Long.parseLong(request.getParameter("id"));
    
    Key commentKey = KeyFactory.createKey("Comment", id);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    String posterEmail = "";
    String userEmail = userService.getCurrentUser().getEmail();
    try {
      posterEmail = (String) datastore.get(commentKey).getProperty("email");
    } catch (Exception e) {
      response.getWriter().println(e);
      return;
    }
    if (posterEmail.equals(userEmail)) {
      datastore.delete(commentKey);
      response.getWriter().println("Comment deleted.");
    } else {
      response.getWriter().println("Sorry, you can only delete your own comments.");
    }
  }
}
