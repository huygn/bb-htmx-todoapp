import { handler } from "./dist/server.js";
import { serve } from "./serve.ts";

async function handleRequest(req) {
  try {
    const { body, status } = await handler(req);
    return new Response(body, {
      status,
      headers: {
        "content-type": "text/html;charset=utf-8",
      },
    });
  } catch (err) {
    return new Response(err.message, {
      status: 500,
    });
  }
}

serve(handleRequest);
