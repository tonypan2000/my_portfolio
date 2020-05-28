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
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that returns some example content. TODO: modify this file to handle comments data */
@WebServlet("/data")
public class DataServlet extends HttpServlet {

    // hard coded greeting messages
    private ArrayList<String> greetings;

    @Override
    public void init() {
        greetings = new ArrayList<>();
        greetings.add("Hello world!");
        greetings.add("I have the same birthday as Elon Mustk.");
        greetings.add("Occupy Mars");
        greetings.add("Nuke Mars");
    }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // response.setContentType("text/html;");
    // response.getWriter().println("Hello from Tony Pan!");

    // Convert the server stats to JSON
    String json = convertToJsonWithGson(greetings);
    // Send the JSON as the response
    response.setContentType("application/json;");
    response.getWriter().println(json);
  }

  /**
   * Converts an ArrayList of Strings into a JSON String using the Gson library
   */
  private String convertToJsonWithGson(ArrayList<String> inputText) {
      Gson gson = new Gson();
      String json = gson.toJson(inputText);
      return json;
  }
}
