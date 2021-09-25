import "./App.css";
import marked from "marked";
import { useEffect, useState } from "react";

export function App() {
  const [selectedId, setSelectedId] = useState(null);
  const [editorText, setEditorText] = useState(null);
  const [notes, setNotes] = useState([]);

  useEffect(() => {
    fetch("/graphql", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify({ query: "{ allNotes { id, title, text } }" }),
    })
      .then((r) => r.json())
      .then((data) => {
        console.log("data returned:", data);
        setNotes(data.data.allNotes);
      });
  }, []);

  return (
    <div id="App">
      <div class="containers">
        <div id="drawer">
          <NoteList
            notes={notes}
            selectedId={selectedId}
            onSelect={(id) => setSelectedId(id)}
          />
        </div>
        <div id="content">
          <div id="note-input-container">
            <textarea
              id="note-input"
              onChange={(e) => {
                setEditorText(e.target.value);
              }}
              disabled={selectedId === null}
            >
              {editorText}
            </textarea>
          </div>
          <div id="note-preview-container">
            <div
              id="preview"
              dangerouslySetInnerHTML={{
                __html: marked(editorText ? editorText : ""),
              }}
            ></div>
          </div>
        </div>
      </div>
    </div>
  );
}

function NoteList({ notes, onSelect, selectedId }) {
  return (
    <ul id="note-list">
      {notes.map((note) => {
        const selected = note.id === selectedId;
        const className = selected
          ? "note-list-item selected"
          : "note-list-item";
        return (
          <li
            key={note.id}
            class={className}
            onClick={(e) => onSelect(note.id)}
          >
            {note.title}
          </li>
        );
      })}
    </ul>
  );
}
