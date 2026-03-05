export async function GET(
  request: Request,
  { params }: { params: Promise<{ userId: string; podcastId: string }> }
) {
  const { userId, podcastId } = await params;
  const backendBase = process.env.BACKEND_URL || "http://localhost:8085";
  const backendUrl = `${backendBase}/users/${userId}/podcasts/${podcastId}/preview`;

  const backendResponse = await fetch(backendUrl, {
    headers: { Accept: "text/event-stream" },
  });

  if (!backendResponse.ok) {
    return new Response(backendResponse.statusText, {
      status: backendResponse.status,
    });
  }

  return new Response(backendResponse.body, {
    headers: {
      "Content-Type": "text/event-stream",
      "Cache-Control": "no-cache",
      Connection: "keep-alive",
    },
  });
}
