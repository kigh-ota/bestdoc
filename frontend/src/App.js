import "./App.css";
import marked from "marked";
import { useCallback, useEffect, useRef, useState } from "react";
import {
  getNoteList,
  getNote,
  addNote,
  updateNote,
  deleteNote,
} from "./Graphql";

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

  const loadEditor = useCallback((id) => {
    return getNote(id).then((note) => {
      setEditor({
        id,
        title: note.title,
        titleBefore: note.title,
        text: note.text,
        textBefore: note.text,
      });
    });
  }, []);

  const setNoteToEditor = useCallback((note) => {
    setEditor({
      id: note.id,
      title: note.title,
      titleBefore: note.title,
      text: note.text,
      textBefore: note.text,
    });
  }, []);

  const [noteList, setNoteList] = useState(null);
  const [keyword, setKeyword] = useState("");

  const isChanged =
    editor.title !== editor.titleBefore || editor.text !== editor.textBefore;

  const updateNoteList = useCallback(() => {
    setNoteList(null);
    return getNoteList(keyword).then(setNoteList);
  }, [keyword]);

  const saveIfChangedAndUpdateState = useCallback(async () => {
    if (!isChanged) {
      return Promise.resolve(NEW_NOTE);
    }
    if (editor.id === null) {
      // Add
      return addNote(editor.title, editor.text).then((addedNote) => {
        updateNoteList();
        return addedNote;
      });
    } else {
      // Update
      return updateNote(editor.id, editor.title, editor.text).then(
        (updatedNote) => {
          updateNoteList();
          return updatedNote;
        }
      );
    }
  }, [editor.id, editor.title, editor.text, updateNoteList, isChanged]);

  const handleSave = useCallback(async () => {
    if (!isChanged) {
      return;
    }
    if (editor.id === null) {
      // Add
      const addedNote = await saveIfChangedAndUpdateState();
      setNoteToEditor(addedNote);
    } else {
      // Update
      saveIfChangedAndUpdateState();
      setNoteToEditor(editor);
    }
  }, [editor, isChanged, saveIfChangedAndUpdateState, setNoteToEditor]);

  const titleInputRef = useRef(null);

  const handleNew = useCallback(async () => {
    await saveIfChangedAndUpdateState();
    setEditor(NEW_NOTE);
    titleInputRef.current.focus();
  }, [saveIfChangedAndUpdateState]);

  const keydownListener = useCallback(
    (keydownEvent) => {
      const { key, repeat, ctrlKey, metaKey } = keydownEvent;
      if (repeat) return;
      if ((ctrlKey || metaKey) && key === "s") {
        handleSave();
        keydownEvent.preventDefault();
      } else if ((ctrlKey || metaKey) && key === "n") {
        handleNew();
        keydownEvent.preventDefault();
      }
    },
    [handleSave, handleNew]
  );

  useEffect(() => {
    window.addEventListener("keydown", keydownListener, true);
    return () => window.removeEventListener("keydown", keydownListener, true);
  }, [keydownListener]);

  useEffect(() => {
    document.title = isChanged ? "bestdoc *" : "bestdoc";
  }, [isChanged]);

  const revertEditor = useCallback(() => {
    setEditor({
      id: editor.id,
      title: editor.titleBefore,
      titleBefore: editor.titleBefore,
      text: editor.textBefore,
      textBefore: editor.textBefore,
    });
  }, [editor.id, editor.titleBefore, editor.textBefore]);

  useEffect(() => {
    updateNoteList();
  }, [keyword, updateNoteList]);

  const updateTitle = (title) => setEditor({ ...editor, title });
  const updateText = (text) => setEditor({ ...editor, text });

  const handleRevert = useCallback(() => {
    if (window.confirm(`Do you really want to revert changes?`)) {
      revertEditor();
    }
  }, [revertEditor]);

  const handleDelete = useCallback(() => {
    const title = noteList.find((note) => note.id === editor.id).title;
    if (window.confirm(`Do you really want to delete the note "${title}"?`)) {
      deleteNote(editor.id);
      setNoteList(noteList.filter((note) => note.id !== editor.id));
      setEditor(NEW_NOTE);
    }
  }, [editor.id, noteList]);

  const NewButton = () => (
    <button disabled={editor.id === null && !isChanged} onClick={handleNew}>
      ➕
    </button>
  );
  const SaveButton = () => (
    <button disabled={!isChanged} onClick={handleSave}>
      💾
    </button>
  );
  const RevertButton = () => (
    <button disabled={!isChanged} onClick={handleRevert}>
      ⏎
    </button>
  );
  const DeleteButton = () => (
    <button disabled={editor.id === null} onClick={handleDelete}>
      🗑
    </button>
  );

  return (
    <div id="App">
      <div className="containers">
        <div id="drawer">
          <div id="drawer-toolbar">
            <NewButton />
            <SaveButton />
            <RevertButton />
            <DeleteButton />
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
            titleInputRef={titleInputRef}
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

function TitleInput({ value, onChange, titleInputRef }) {
  return (
    <input
      id="note-title-input"
      type="text"
      value={value}
      onChange={onChange}
      placeholder="Title"
      ref={titleInputRef}
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
  if (notes === null) {
    return <span>Loading...</span>;
  }
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
