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

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.QueryResultList;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.Sentiment;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.gson.Gson;
import java.io.IOException;
import java.lang.Long;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Servlet that returns some example content. This file handles comment data */
@WebServlet("/data")
public class DataServlet extends HttpServlet {
  
  private static class Comment {
    long id;
    String name;
    Long timestamp;
    String content;
    String imageUrl;
    String mood;
    float sentiment;
    String cursor;

    private Comment(long id, String name, Long timestamp, String content,
        String imageUrl, String mood, float sentiment, String cursor) {
      this.id = id;
      this.name = name;
      this.timestamp = timestamp;
      this.content = content;
      this.imageUrl = imageUrl;
      this.mood = mood;
      this.sentiment = sentiment;
      this.cursor = cursor;
    }
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get the max num comments from input
    int maxNumComments = getMaxNumComments(request, 5);

    FetchOptions fetchOptions = FetchOptions.Builder.withLimit(maxNumComments);

    // If this servlet is passed a cursor parameter, let's use it.
    String startCursor = request.getParameter("cursor");
    if (startCursor != null) {
      fetchOptions.startCursor(Cursor.fromWebSafeString(startCursor));
    }

    Query query = new Query("Comment").addSort("timestamp", SortDirection.DESCENDING);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery preparedQuery = datastore.prepare(query);

    // limits preparedQuery to the user specified
    QueryResultList<Entity> entities;
    String encodedCursor;
    try {
      entities = preparedQuery.asQueryResultList(fetchOptions);
      Cursor originalCursor = entities.getCursor();
      encodedCursor = originalCursor.toWebSafeString();
    } catch (IllegalArgumentException e) {
      response.setContentType("text/html;");
      response.getWriter().println("Invalid cursor: " + e);
      return;
    }

    // Do the translation.
    String language = request.getParameter("language");
    Translate translate = TranslateOptions.getDefaultInstance().getService();

    List<Comment> comments = new ArrayList<>();
    for (Entity entity : entities) {
      long id = entity.getKey().getId();
      String name = (String) entity.getProperty("name");
      Long timestamp = ((Number) entity.getProperty("timestamp")).longValue();
      String content = (String) entity.getProperty("content");
      Translation translation = translate.translate(content, Translate.TranslateOption.targetLanguage(language));
      String translatedText = translation.getTranslatedText();
      String imageUrl = (String) entity.getProperty("image");
      String mood = (String) entity.getProperty("mood");
      float sentiment = ((Number) entity.getProperty("sentiment")).floatValue();
      // TODO: more efficient method of storing cursor
      String cursor = encodedCursor;

      Comment comment = new Comment(id, name, timestamp, translatedText, imageUrl, mood, sentiment, cursor);
      comments.add(comment);
    }

    Gson gson = new Gson();

    // Send the JSON as the response
    response.setContentType("application/json; charset=UTF-8");
    response.setCharacterEncoding("UTF-8");
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
    UserService userService = UserServiceFactory.getUserService();
    // checked for user log in status on the client side
    // get email from userservice
    String userEmail = userService.getCurrentUser().getEmail();
    // Get the nickname and comment from the post request
    String userName = getParam(request, "name-input", "");
    String comment = getParam(request, "comment-input", "");

    // Get the URL of the image that the user uploaded to Blobstore.
    String imageUrl = (String) getUploadedFileUrl(request, "image");

    String mood = getParam(request, "mood", "Happy");
    if (userName.isEmpty()) {
      userName = userEmail;
    }
    // TODO: a more elegant way of error checking/notifying user 
    if (comment.isEmpty() && imageUrl == null) {
      response.setContentType("text/html");
      response.getWriter().println("Please enter a valid comment or upload an image.");
      return;
    } 

    // perform sentiment analysis
    float score = -2; // sentinel value
    if (!comment.isEmpty()) {
      Document doc = Document.newBuilder().setContent(comment).setType(Document.Type.PLAIN_TEXT).build();
      LanguageServiceClient languageService = LanguageServiceClient.create();
      Sentiment sentiment = languageService.analyzeSentiment(doc).getDocumentSentiment();
      score = sentiment.getScore();
      languageService.close();
    }

    // get the current timestamp
    Instant currentTimeMillisInstant = Instant.now();
    long currentTimeMillis = currentTimeMillisInstant.toEpochMilli();
    // TODO: look into if two instances of comments are posted at the same milisecond

    Entity commentEntity = new Entity("Comment");
    commentEntity.setProperty("email", userEmail);
    commentEntity.setProperty("name", userName);
    commentEntity.setProperty("timestamp", currentTimeMillis);
    commentEntity.setProperty("content", comment);
    commentEntity.setProperty("image", imageUrl);
    commentEntity.setProperty("mood", mood);
    commentEntity.setProperty("sentiment", score);

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

  /** Returns a URL that points to the uploaded file, or null if the user didn't upload a file. */
  private String getUploadedFileUrl(HttpServletRequest request, String formInputElementName) {
    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(request);
    List<BlobKey> blobKeys = blobs.get(formInputElementName);

    // User submitted form without selecting a file, so we can't get a URL. (dev server)
    if (blobKeys == null || blobKeys.isEmpty()) {
      return null;
    }

    // Our form only contains a single file input, so get the first index.
    BlobKey blobKey = blobKeys.get(0);

    // User submitted form without selecting a file, so we can't get a URL. (live server)
    BlobInfo blobInfo = new BlobInfoFactory().loadBlobInfo(blobKey);
    if (blobInfo.getSize() == 0) {
      blobstoreService.delete(blobKey);
      return null;
    }

    // We could check the validity of the file here, e.g. to make sure it's an image file
    // https://stackoverflow.com/q/10779564/873165

    // Use ImagesService to get a URL that points to the uploaded file.
    ImagesService imagesService = ImagesServiceFactory.getImagesService();
    ServingUrlOptions options = ServingUrlOptions.Builder.withBlobKey(blobKey);

    // To support running in Google Cloud Shell with AppEngine's devserver, we must use the relative
    // path to the image, rather than the path returned by imagesService which contains a host.
    try {
      URL url = new URL(imagesService.getServingUrl(options));
      return url.getPath();
    } catch (MalformedURLException e) {
      return imagesService.getServingUrl(options);
    }
  }
}
