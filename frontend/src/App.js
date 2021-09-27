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

  function updateTitle(title) {
    if (selectedId === null) {
      return; // fail safe
    }
    const note = notes[selectedId];
    setNotes({ ...notes, [selectedId]: { ...note, title } });
  }

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
        query: `mutation { updateNote(id:"${selectedId}", title:"${note.title}", text:"""${note.text}""") { id, title, text, createdAt, updatedAt } }`,
      }),
    }).then((r) => r.json())
    .then((data) => {
      console.log("data returned:", data);
      const note = data.data.updateNote;
      setNotes({ ...notes, [note.id]: note });
    })
  }

  return (
    <div id="App">
      <div class="containers">
        <div id="drawer">
          <div id="drawer-toolbar">
            <button>New</button>
            <button onClick={save} disabled={selectedId === null}>
              Save
            </button>
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
          <input
            id="note-title-input"
            type="text"
            value={selectedId ? notes[selectedId].title : ""}
            onChange={(e) => updateTitle(e.target.value)}
            disabled={selectedId === null}
            placeholder="Title"
          />
          <div id="note-text-container">
            <textarea
              id="note-text-input"
              onChange={(e) => updateEditorText(e.target.value)}
              value={selectedId ? notes[selectedId].text : ""}
              disabled={selectedId === null}
            />
            <div
              id="note-text-preview"
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
      {Object.values(notes)
        .sort((a, b) => {
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
