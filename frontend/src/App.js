import "./App.css";
import marked from "marked";
import { useEffect, useState } from "react";

function allNotesToObject(notes) {
  const obj = {};
  notes.forEach((note) => {
    obj[note.id] = note;
  });
  return obj;
}

export function App() {
  const [selectedId, setSelectedId] = useState(null);
  const [notes, setNotes] = useState({}); // {id: Note}

  function getNoteAndUpdateState(id) {
    return fetch("/graphql", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify({
        query: `{ getNote(id:"${id}") { id, title, text, createdAt, updatedAt } }`,
      }),
    })
      .then((r) => r.json())
      .then((data) => {
        console.log("data returned:", data);
        const note = data.data.getNote;
        setNotes({ ...notes, [note.id]: note });
      });
  }

  useEffect(() => {
    fetch("/graphql", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify({
        query: "{ allNotes { id, title, text, createdAt, updatedAt } }",
      }),
    })
      .then((r) => r.json())
      .then((data) => {
        console.log("data returned:", data);
        setNotes(allNotesToObject(data.data.allNotes));
      });
  }, []);

  function updateEditorText(text) {
    if (selectedId === null) {
      return; // fail safe
    }
    const note = notes[selectedId];
    setNotes({ ...notes, [selectedId]: { ...note, text } });
  }

  function save() {
    const note = notes[selectedId];
    fetch("/graphql", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify({
        query: `mutation { updateNote(id:"${selectedId}", title:"${note.title}", text:"${note.text}") }`,
      }),
    }).then(getNoteAndUpdateState(selectedId))
  }

  return (
    <div id="App">
      <div class="containers">
        <div id="drawer">
          <div>
            <button>New</button>
            <button onClick={save}>Save</button>
          </div>
          <NoteList
            notes={notes}
            selectedId={selectedId}
            onSelect={async (id) => {
              setSelectedId(id);
              getNoteAndUpdateState(id);
            }}
          />
        </div>
        <div id="content">
          <div id="note-input-container">
            <div>
              <input
                id="note-title-input"
                type="text"
                value={selectedId ? notes[selectedId].title : ""}
                disabled={selectedId === null}
              />
              <textarea
                id="note-input"
                onChange={(e) => updateEditorText(e.target.value)}
                value={selectedId ? notes[selectedId].text : ""}
                disabled={selectedId === null}
              />
            </div>
          </div>
          <div id="note-preview-container">
            <div
              id="preview"
              dangerouslySetInnerHTML={{
                __html: marked(selectedId ? notes[selectedId].text : ""),
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
      {Object.values(notes).sort((a,b) => {
        if (a.updatedAt < b.updatedAt) {
          return 1;
        }
        if (a.updatedAt > b.updatedAt) {
          return -1;
        }
        return 0;
      })
      .map((note) => {
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
