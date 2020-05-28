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
      'Occupy Mars', 'Nuke Mars'];

  // Pick a random greeting.
  const greeting = greetings[Math.floor(Math.random() * greetings.length)];

  // Add it to the page.
  const greetingContainer = document.getElementById('greeting-container');
  greetingContainer.innerText = greeting;
}

/**
 * Adds a random fun fact about me to the page.
 */
function addRandomFact() {
  const facts =
    ['I like robotics', "I'm interested in Machine Learning", 'My birthday is on June 28th', 
      "I still haven't received my Noogler hat", 'I have the same birthday as Elon Musk.'];

  // Pick a random fact.
  const fact = facts[Math.floor(Math.random() * facts.length)];

  // Add it to the page.
  const factContainer = document.getElementById('fact-container');
  factContainer.innerText = fact;
}

/**
 * Fetches the greeting from the server and adds it to the DOM.
 */
function getGreeting() {
  console.log('Fetching the greeting message.');
  const responsePromise = fetch('/data');
  responsePromise.then(handleResponse);
}

/**
 * Handles response by converting it to text and passing the result to 
 * addGreatingToDom().
 */
function handleResponse(response) {
  console.log('Handling the response.');
  const textPromise = response.text();
  textPromise.then(addGreetingToDom);
}

/**
 * Adds the greeting message to the DOM.
 */
function addGreetingToDom(greeting) {
  console.log('Adding greeting to dom: ' + greeting);
  const greetingContainer = document.getElementById('greeting-container');
  greetingContainer.innerText = greeting;
}

/**
 * Fetches the greeting from the server and adds it to the DOM using arrow function.
 */
function getGreetingArrow() {
  fetch('/data').then(response => response.text()).then(greeting => {
    document.getElementById('greeting-container').innerText = greeting;
  });
}

/**
 * Fetches the greeting from the server and adds it to the DOM using async await.
 */
async function getGreetingAwait() {
  const response = await fetch('/data');
  const greeting = await response.text();
  document.getElementById('greeting-container').innerText = greeting;
}

/**
 * Fetches the greeting from the server and adds a randomly selected one
 * to the DOM in JSON String format with the arrow function.
 */
function getGreetingJson() {
  fetch('/data').then(response => response.json()).then(input => {
    // Pick a random greeting.
    const greeting = input.greetings[Math.floor(Math.random() * input.greetings.length)];
    // Add it to the page.
    document.getElementById('greeting-container').innerText = greeting;
  });
}

/**
 * Fetches all of the previous comments made by users and 
 * displays it below the input comment form
 */
function getComments() {
  fetch('/data').then(response => response.json()).then(text => {
    const commentsContainer = document.getElementById('previous-comments');
    text.forEach(entry => {
      commentsContainer.appendChild(createListElementForComment(entry));
    });
  });
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
  const deleteButtonElement = document.createElement('button');
  deleteButtonElement.innerText = 'Delete';
  deleteButtonElement.addEventListener('click', () => {
    deleteComment(entry);

    // Remove the comment from the DOM.
    refreshComments();
  })
  liElement.appendChild(deleteButtonElement);
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
function refreshComments() {
  // remove previous comments
  var previousComments = document.getElementById('previous-comments');
  while (previousComments.firstChild) {
    previousComments.removeChild(previousComments.firstChild);
  }
  // read user input value
  var maxNumComments = document.getElementById('max-num-comments').value;
  // encode user input parameter as a query string embedded in the URL
  var queryUrl = updateQueryString('max-num-comments', maxNumComments);
  // fetch from Datastroe and repopulate comment section
  fetch('/data?' + queryUrl).then(response => response.json()).then(text => {
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
  const params = updateQueryString('id', comment.id)
  fetch('/delete-data', {method: 'POST', body: params});
}
