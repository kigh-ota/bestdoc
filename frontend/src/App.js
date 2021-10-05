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

const NEW_NOTE = {
  id: null,
  title: "",
  text: "",
};

export function App() {
  const [editor, setEditor] = useState(NEW_NOTE);
  const [notes, setNotes] = useState({}); // {id: Note}
  const [keyword, setKeyword] = useState("");

  const isChanged = () => {
    const base = editor.id === null ? NEW_NOTE : notes[editor.id];
    return editor.title !== base.title || editor.text !== base.text;
  };

  const getNoteAndUpdateState = (id) => {
    return fetch("/graphql", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify({
        query: `{ getNote(id:"${id}") { id, title, text, tags, createdAt, updatedAt } }`,
      }),
    })
      .then((r) => r.json())
      .then((data) => {
        console.log("data returned:", data);
        const note = data.data.getNote;
        setNotes({ ...notes, [note.id]: note });
      });
  };

  useEffect(() => {
    fetch("/graphql", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify({
        query: `query AllNotes($keyword: String) { allNotes(keyword: $keyword) { id, title, text, tags, createdAt, updatedAt } }`,
        variables: { keyword: keyword.trim() },
      }),
    })
      .then((r) => r.json())
      .then((data) => {
        console.log("data returned:", data);
        setNotes(allNotesToObject(data.data.allNotes));
      });
  }, [keyword]);

  const updateTitle = (title) => setEditor({ ...editor, title });
  const updateText = (text) => setEditor({ ...editor, text });

  const saveIfChangedAndUpdateState = async () => {
    if (!isChanged()) {
      return Promise.resolve(NEW_NOTE);
    }
    if (editor.id === null) {
      // Add
      return fetch("/graphql", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
        },
        body: JSON.stringify({
          query: `mutation { addNote(title:"${editor.title}", text:"""${editor.text}""") { id, title, text, createdAt, updatedAt } }`,
        }),
      })
        .then((r) => r.json())
        .then((data) => {
          console.log("data returned:", data);
          const addedNote = data.data.addNote;
          setNotes({ ...notes, [addedNote.id]: addedNote });
          return addedNote;
        });
    } else {
      // Update
      return fetch("/graphql", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
        },
        body: JSON.stringify({
          query: `mutation { updateNote(id:"${editor.id}", title:"${editor.title}", text:"""${editor.text}""") { id, title, text, createdAt, updatedAt } }`,
        }),
      })
        .then((r) => r.json())
        .then((data) => {
          console.log("data returned:", data);
          const updatedNote = data.data.updateNote;
          setNotes({ ...notes, [updatedNote.id]: updatedNote });
          return updatedNote;
        });
    }
  };

  return (
    <div id="App">
      <div className="containers">
        <div id="drawer">
          <div id="drawer-toolbar">
            <button
              onClick={async () => {
                await saveIfChangedAndUpdateState();
                setEditor(NEW_NOTE);
              }}
            >
              New
            </button>
            <button
              onClick={async () => {
                if (editor.id === null) {
                  // Add
                  const addedNote = await saveIfChangedAndUpdateState();
                  setEditor(addedNote);
                } else {
                  // Update
                  saveIfChangedAndUpdateState();
                }
              }}
            >
              {editor.id === null ? "Add" : "Update"}
            </button>
          </div>
          <SearchBox
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
          />
          <NoteList
            notes={notes}
            selectedId={editor.id}
            onSelect={async (id) => {
              await saveIfChangedAndUpdateState();
              setEditor(notes[id]);
            }}
          />
        </div>
        <div id="content">
          <TitleInput
            value={editor.title}
            onChange={(e) => updateTitle(e.target.value)}
          />
          <div id="note-text-container">
            <TextInput
              value={editor.text}
              onChange={(e) => updateText(e.target.value)}
            />
            <Preview text={editor.text} />
          </div>
        </div>
      </div>
    </div>
  );
}

function SearchBox({ value, onChange }) {
  return (
    <input
      id="searchbox"
      type="text"
      value={value}
      onChange={onChange}
      placeholder="keyword"
    />
  );
}

function TitleInput({ value, onChange }) {
  return (
    <input
      id="note-title-input"
      type="text"
      value={value}
      onChange={onChange}
      placeholder="Title"
    />
  );
}

function TextInput({ value, onChange }) {
  return <textarea id="note-text-input" value={value} onChange={onChange} />;
}

function Preview({ text }) {
  return (
    <div
      id="note-text-preview"
      dangerouslySetInnerHTML={{
        __html: marked(text),
      }}
    ></div>
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
              className={className}
              onClick={(e) => onSelect(note.id)}
            >
              {note.title || "(No title)"}
            </li>
          );
        })}
    </ul>
  );
}
