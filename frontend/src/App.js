import "./App.css";
import marked from "marked";
import { useCallback, useEffect, useState } from "react";
import { getNoteList, getNote, addNote, updateNote } from "./Graphql";

const NEW_NOTE = {
  id: null,
  title: "",
  titleBefore: "",
  text: "",
  textBefore: "",
};

// TODO Loading

export function App() {
  const [editor, setEditor] = useState(NEW_NOTE);
  const [noteList, setNoteList] = useState([]);
  const [keyword, setKeyword] = useState("");

  const isChanged =
    editor.title !== editor.titleBefore || editor.text !== editor.textBefore;

  useEffect(() => {
    document.title = isChanged ? "bestdoc *" : "bestdoc";
  }, [isChanged]);

  const updateNoteList = useCallback(() => {
    return getNoteList(keyword).then(setNoteList);
  }, [keyword]);

  function loadEditor(id) {
    return getNote(id).then((note) => {
      setEditor({
        id,
        title: note.title,
        titleBefore: note.title,
        text: note.text,
        textBefore: note.text,
      });
    });
  }

  useEffect(() => {
    updateNoteList();
  }, [keyword, updateNoteList]);

  const updateTitle = (title) => setEditor({ ...editor, title });
  const updateText = (text) => setEditor({ ...editor, text });

  const saveIfChangedAndUpdateState = async () => {
    if (!isChanged) {
      return Promise.resolve(NEW_NOTE);
    }
    if (editor.id === null) {
      // Add
      return addNote(editor.title, editor.text).then(() => {
        updateNoteList();
      });
    } else {
      // Update
      return updateNote(editor.id, editor.title, editor.text).then(() => {
        updateNoteList();
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
                  setEditor({
                    id: editor.id,
                    title: editor.title,
                    titleBefore: editor.title,
                    text: editor.text,
                    textBefore: editor.text,
                  });
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
            notes={noteList}
            selectedId={editor.id}
            onSelect={async (id) => {
              await saveIfChangedAndUpdateState();
              loadEditor(id);
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
        .map((note) => (
          <NoteListItem
            key={note.id}
            onSelect={() => onSelect(note.id)}
            title={note.title || "(No title)"}
            selected={note.id === selectedId}
          />
        ))}
    </ul>
  );
}

function NoteListItem({ onSelect, title, selected }) {
  const className = selected ? "note-list-item selected" : "note-list-item";
  return (
    <li className={className} title={title} onClick={onSelect}>
      {title}
    </li>
  );
}
