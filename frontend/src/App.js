import "./App.css";
import marked from "marked";
import { useEffect } from 'react';

export function App() {
  useEffect(() => {
    fetch('/graphql', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify({query: "{ allNotes { title } }"})
    })
      .then(r => r.json())
      .then(data => console.log('data returned:', data));
  }, [])

  return (
    <div id="App">
      <div class="containers">
        <div id="drawer">List</div>
        <div id="content">
          <div id="note-input-container">
            <textarea
              id="note-input"
              onChange={(e) => {
                document.getElementById("preview").innerHTML = marked(
                  e.target.value
                );
              }}
            ></textarea>
          </div>
          <div id="note-preview-container">
            <div id="preview"></div>
          </div>
        </div>
      </div>
    </div>
  );
}
