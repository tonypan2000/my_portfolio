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

import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that returns some example content. This file handles comment data */
@WebServlet("/data")
public class DataServlet extends HttpServlet {

  // initialize user input comments to a new ArrayList
  // <name, comments>
  // maps all comments to each username
  private HashMap<String, ArrayList<String>> comments;

  @Override
  public void init() {
    comments = new HashMap<String, ArrayList<String>>();  
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Send the JSON as the response
    response.setContentType("application/json;");
    String commentsInJson = convertToJsonWithGson(comments);
    response.getWriter().println(commentsInJson);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get the name and comment from the form and add it to comments
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
    if (comments.get(name) == null) {
      ArrayList<String> commentsFromName = new ArrayList<String>();
      commentsFromName.add(comment);
      comments.put(name, commentsFromName);
    } else {
      comments.get(name).add(comment);
    }
    
    // Redirect back to the HTML page.
    response.sendRedirect("/index.html");
  }

  /**
   * Converts an ArrayList of Strings into a JSON String using the Gson library
   */
  private String convertToJsonWithGson(ArrayList<String> input) {
    Gson gson = new Gson();
    String json = gson.toJson(input);
    return json;
  }

  /**
   * Converts an ArrayList of Strings into a JSON String using the Gson library
   */
  private String convertToJsonWithGson(HashMap<String, ArrayList<String>> input) {
    Gson gson = new Gson();
    String json = gson.toJson(input);
    return json;
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
