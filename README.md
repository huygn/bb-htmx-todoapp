# shadow-cljs + htmx todo app

Quick example of a todo list application using [shadow-cljs](https://github.com/thheller/shadow-cljs) and [htmx](https://htmx.org/), build to ESM format and deployed with [Deno Deploy](https://htmx-cljs.deno.dev/).

With `htmx` we get a single page app without writing a single line of Javascript.

From their own web page:

> htmx allows you to access AJAX, CSS Transitions, WebSockets and Server Sent Events directly in HTML, using attributes, so you can build modern user interfaces with the simplicity and power of hypertext

> htmx is small (~10k min.gz'd), dependency-free, extendable & IE11 compatible

## Run the app locally

```sh
npm i
npm run deno
```

Then visit http://localhost:8000
