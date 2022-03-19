function callGraphql(query, variables) {
  return fetch("/graphql", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: JSON.stringify({
      query,
      variables,
    }),
  })
    .then((r) => r.json())
    .then((data) => {
      console.log("data returned:", data);
      return data;
    });
}

export function getNote(id) {
  return callGraphql(
    `query GetNote($id: ID!) { getNote(id: $id) { id, title, text, tags, createdAt, updatedAt } }`,
    { id }
  ).then((data) => {
    return data.data.getNote;
  });
}

export function getNoteList(keyword, limit) {
  return callGraphql(
    `query AllNotes($keyword: String, $limit: Int) { allNotes(keyword: $keyword, limit: $limit) { id, title, tags, createdAt, updatedAt } }`,
    { keyword: keyword.trim(), limit }
  ).then((data) => {
    return data.data.allNotes;
  });
}

export function addNote(title, text) {
  return callGraphql(
    `mutation AddNote($title: String!, $text: String!) { addNote(title: $title, text: $text) { id, title, text, createdAt, updatedAt } }`,
    { title: title.trim(), text }
  ).then((data) => {
    return data.data.addNote;
  });
}

export function updateNote(id, title, text) {
  return callGraphql(
    `mutation UpdateNote($id: ID!, $title: String!, $text: String!) { updateNote(id: $id, title: $title, text: $text) { id, title, text, createdAt, updatedAt } }`,
    { id, title, text }
  ).then((data) => data.data.updateNote);
}

export function deleteNote(id) {
  return callGraphql(
    `mutation DeleteNote($id: ID!) { deleteNote(id: $id) }`,
    { id }
  ).then((data) => data.data.deleteNote);
}
