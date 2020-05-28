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
      ['Hello world!', 'Can you prove to me that you have consciousness?', 'I am Iron Man.'];

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
        "I still haven't received my Noogler hat"];

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
      fetch('/data').then(response => response.json()).then((greetings) => {
          // Pick a random greeting.
          const greeting = greetings[Math.floor(Math.random() * greetings.length)];
          // Add it to the page.
          document.getElementById('greeting-container').innerText = greeting;
      });
  }