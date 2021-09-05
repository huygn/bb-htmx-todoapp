export async function serve(
  handler: (req: Request) => Response | Promise<Response>
) {
  async function handleConn(conn: Deno.Conn) {
    const httpConn = Deno.serveHttp(conn);
    for await (const e of httpConn) {
      e.respondWith(handler(e.request)).catch((e) =>
        console.error("Failed handing a request:", e)
      );
    }
  }

  const listener = Deno.listen({ port: 8000 });
  const addr = listener.addr as Deno.NetAddr;
  console.log(`Listening on http://${addr.hostname}:${addr.port}`);

  for await (const conn of listener) {
    handleConn(conn).catch((e) =>
      console.error("Failed serving a connection:", e)
    );
  }
}
