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

/**
 * Adds a random greeting to the page.
 */
function addRandomGreeting() {
  const greetings =
    ['Hello world!', 'Can you prove to me that you have consciousness?', 'I am Iron Man.',
      'Occupy Mars', 'Nuke Mars', 'I like robotics', "I'm interested in Machine Learning", 
      'My birthday is on June 28th', 
      "I still haven't received my Noogler hat", 'I have the same birthday as Elon Musk.'];

  // Pick a random greeting.
  const greeting = greetings[Math.floor(Math.random() * greetings.length)];

  // Add it to the page.
  const greetingContainer = document.getElementById('greeting-container');
  greetingContainer.innerText = greeting;
}
   
/**
 * Creates an <li> element containing comment entries. 
 */
function createListElementForComment(entry) {
  const liElement = document.createElement('li');
  const nameElement = document.createElement('h4');
  nameElement.innerText = 'Posted by: ' + entry.name;
  liElement.appendChild(nameElement);
  const dateElement = document.createElement('p');
  dateElement.innerText = epochToLocale(entry.timestamp);
  liElement.appendChild(dateElement);
  const commentElement = document.createElement('p');
  commentElement.innerText = entry.content;
  liElement.appendChild(commentElement);
  if (entry.mood !== undefined) {
    const moodText = document.createElement('label');
    moodText.innerHTML = 'mood: ';
    liElement.appendChild(moodText);
    const moodElement = document.createElement('img');
    moodElement.src = 'emojis/' + entry.mood + '.png';
    moodElement.style.width = '20px';
    moodElement.style.height = '20px';
    liElement.appendChild(moodElement);
    const lineBreak = document.createElement('br');
    liElement.appendChild(lineBreak);
  }
  console.log('image url fetching: ' + entry.imageUrl);
  if (entry.imageUrl !== undefined) {
    const imageElement = document.createElement('img');
    imageElement.src = entry.imageUrl;
    liElement.appendChild(imageElement);
    const lineBreak = document.createElement('br');
    liElement.appendChild(lineBreak);
  }
  const deleteButtonElement = document.createElement('button');
  deleteButtonElement.innerText = 'Delete';
  deleteButtonElement.addEventListener('click', () => {
    deleteComment(entry);

    // Remove the comment from the DOM.
    refreshComments();
  })
  liElement.appendChild(deleteButtonElement);
  // TODO: find a more efficient way to update current cursor
  const nextPageElement = document.getElementById('next-page');
  nextPageElement.innerHTML = entry.cursor;
  return liElement;
}

/**
 * Converts a timezone-agnostic timestamp epoch milliseconds
 * to a Date displaying in local time zone
 */
function epochToLocale(epoch) {
  const time = new Date(epoch);
  return time.toLocaleString();
}

/**
 * After user changes the max num of comments to display
 * refreshes the comment section
 */
function refreshComments(anotherQueryString) {
  // read user input value
  var maxNumComments = document.getElementById('max-num-comments').value;
  if (maxNumComments < 0) {
    alert('The maximum number of comments on display should be a non-negative integer.');
    return;
  }
  // remove previous comments
  var previousComments = document.getElementById('previous-comments');
  while (previousComments.firstChild) {
    previousComments.removeChild(previousComments.firstChild);
  }
  
  // encode user input parameter as a query string embedded in the URL
  var maxCommentsQuery = updateQueryString('max-num-comments', maxNumComments);
  // fetch from Datastroe and repopulate comment section
  var queryString = '/data?' + maxCommentsQuery;
  if (anotherQueryString !== undefined) {
    queryString += '&' + anotherQueryString;
  }
  fetch(queryString).then(response => response.json()).then(text => {
    const commentsContainer = document.getElementById('previous-comments');
    text.forEach(entry => {
      commentsContainer.appendChild(createListElementForComment(entry));
    });
  });
}

/**
 * Returns an updated URL search param
 */
function updateQueryString(key, value) {
  var searchParams = new URLSearchParams();
  searchParams.append(key, value);
  return searchParams;
}

/**
 * Deletes a chosen comment from Datastore, 
 */
function deleteComment(comment) {
  getLoginStatus('delete').then(response => {
    if (response) {
      const params = updateQueryString('id', comment.id);
      fetch('/delete-data', {method: 'POST', body: params}).then(response => response.text()).then(message => {
        alert(message);
        refreshComments();
      });
    } else {
      alert('You need to log in to delete a comment.');
    }
  });
}

/**
 * Fetches the login status of user
 * if user clicked login button, log user in
 * if they clicked logout button, log them out
 * if delete comment, make a request
 * refreshes comments
 */
function getLoginStatus(id) {
  return fetch('/login-status').then(response => response.text()).then(link => {
    // if user is logged in, server sends the logout link
    if (link.includes('logout')) {
      document.getElementById('post-event').style.display = 'block';
      if (id === 'logout') {
        location.replace(link);
      } else if (id === 'delete') {
        return true;
      }
    } else {
      document.getElementById('post-event').style.display = 'none';
      if (id === 'login') {
        location.replace(link);
      }
    }
    refreshComments();
  });
}

/**
 * If the element is currently visible, change to hidden
 * If it is hidden, show it
 */
function toggleVisibility(id) {
  if (document.getElementById(id).style.display === 'block') {
    document.getElementById(id).style.display = 'none';
  } else {
    document.getElementById(id).style.display = 'block';
  }
}

/**
 * Fetches the URL for BlobStore and unhides
 * the form to submit
 */
function fetchBlobUrl() {
  fetch('/blob-url').then(response => response.text()).then(url => {
    const submitForm = document.getElementById('post-event');
    submitForm.action = url;
    const blobInput = document.getElementById('blob-input');
    blobInput.classList.remove('hidden');
  });
}

/**
 * Gets the current cursor from the last comment displayed
 * Pass that cursor to server end to fetch the next n comments
 */
function nextPage() {
  const nextPageElement = document.getElementById('next-page');
  const cursorString = nextPageElement.innerHTML;
  var cursorQuery = updateQueryString('cursor', cursorString);
  
  refreshComments(cursorQuery);
}

/**
 * Sends request to /data to translate previous comments to a language
 */
function requestTranslation() {
  const selectedLanguage = document.getElementById('translation').value;
  const commentElement = document.getElementById('previous-comments');
  commentElement.lang = selectedLanguage;
  const languageQuery = updateQueryString('language', selectedLanguage);
  refreshComments(languageQuery);
}
