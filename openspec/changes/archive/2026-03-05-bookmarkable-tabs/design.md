## Context

All 4 tabbed pages use the shadcn `Tabs` component with `defaultValue` — tab state is local and lost on reload. Next.js provides `useSearchParams()` for reading and `useRouter()` for updating query parameters.

## Goals / Non-Goals

**Goals:**
- Make all tab selections bookmarkable via `?tab=X` query parameter
- Preserve existing tab behavior (default tab when no param)
- Use a shared hook to avoid duplicating the pattern across 4 pages

**Non-Goals:**
- No changes to tab content or layout
- No browser history entry per tab switch (use `router.replace`, not `router.push`)

## Decisions

**1. Custom `useTabParam` hook**

Create a shared hook that encapsulates the read/write of `?tab=X`. Each page calls it with its default tab value. The hook returns `[currentTab, setTab]`. This avoids duplicating `useSearchParams` + `useRouter` logic in 4 places.

**2. Use `router.replace` not `router.push`**

Tab switches should replace the current history entry, not push new ones. Users don't expect back/forward to cycle through tabs — they expect it to navigate between pages.

**3. Validate tab param against allowed values**

If `?tab=bogus` is in the URL, fall back to the default tab. Each page passes its allowed tab values to the hook.

## Risks / Trade-offs

- [Shallow routing] → Next.js App Router `router.replace` with query params triggers a client-side navigation without full page reload, which is the desired behavior.
