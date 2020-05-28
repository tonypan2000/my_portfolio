// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.gson.Gson;
import java.io.IOException;
import java.lang.Long;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Servlet that returns some example content. This file handles comment data */
@WebServlet("/data")
public class DataServlet extends HttpServlet {
  
  private class Comment {
    long id;
    String name;
    Long timestamp;
    String content;

    private Comment(long idInput, String nameInput, Long timestampInput, String contentInput) {
      id = idInput;
      name = nameInput;
      timestamp = timestampInput;
      content = contentInput;
    }
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get the max num comments from input
    int maxNumComments = getMaxNumComments(request, 5);

    Query query = new Query("Comment").addSort("timestamp", SortDirection.DESCENDING);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    // limits query results to the user specified
    List<Entity> entities = results.asList(FetchOptions.Builder.withLimit(maxNumComments));

    List<Comment> comments = new ArrayList<>();
    for (Entity entity : entities) {
      long id = entity.getKey().getId();
      String name = (String) entity.getProperty("name");
      Long timestamp = ((Number) entity.getProperty("timestamp")).longValue();
      String content = (String) entity.getProperty("content");

      Comment comment = new Comment(id, name, timestamp, content);
      comments.add(comment);
    }

    Gson gson = new Gson();

    // Send the JSON as the response
    response.setContentType("application/json;");
    response.getWriter().println(gson.toJson(comments));
  }

  /**
   * Returns the maximum number of comments the page should display
   * as entered by the user, or a default value if the choice was invalid.
   */
  private int getMaxNumComments(HttpServletRequest request, int input) {    
    // get the input from URL query String
    String inputString = request.getParameter("max-num-comments");

    // convert input from String to int
    int maxNumInt = input;
    try {
      maxNumInt = Integer.parseInt(inputString);
    } catch (NumberFormatException e) {
      System.err.println("Could not convert " + inputString + " to int");
      return input;
    }

    // check that the max num is a positive int
    if (maxNumInt < 0) {
      System.err.println("User input " + maxNumInt + " is out of range.");
      return input;
    }
    return maxNumInt;
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get the name and comment from the post request
    String name = getParam(request, "name-input", "");
    String comment = getParam(request, "comment-input", "");

    // TODO: a more elegant way of error checking/notifying user 
    if (name.isEmpty()) {
      response.setContentType("text/html");
      response.getWriter().println("Please enter a valid name.");
      return;
    } else if (comment.isEmpty()) {
      response.setContentType("text/html");
      response.getWriter().println("Please enter a valid comment.");
      return;
    } 

    // get the current timestamp
    Instant currentTime = Instant.now();
    long currentTimeEpoch = currentTime.toEpochMilli();
    // TODO: look into if two instances of comments are posted at the same milisecond

    Entity commentEntity = new Entity("Comment");
    commentEntity.setProperty("name", name);
    commentEntity.setProperty("timestamp", currentTimeEpoch);
    commentEntity.setProperty("content", comment);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(commentEntity);
    
    // Redirect back to the HTML page.
    response.sendRedirect("/index.html");
  }

  /**
   * @return the request parameter, or the default value if the parameter
   *         was not specified by the client
   */
  private String getParam(HttpServletRequest request, String name, String defaultValue) {
    String value = request.getParameter(name);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }
}
